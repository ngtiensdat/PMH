# FILE HỌC CODE THỰC CHIẾN - PHẦN 3 (LEARN03)

Bạn đã xuất sắc vượt qua phần 2 và bắt đầu hiểu rất sâu về cách hoạt động của Controller và DTO. 
Phần 3 này sẽ đưa bạn vào trung tâm xử lý nghiệp vụ của Backend (Service Layer), nơi trực tiếp tương tác với Cơ sở dữ liệu Oracle qua Stored Procedure.

---

## 💻 Đoạn code: Xử lý duyệt từng bản ghi và gọi Stored Procedure

Đoạn code này được trích xuất từ phương thức private helper `approveSingleCategory` trong file [GroupCategoryServiceImpl.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/service/impl/GroupCategoryServiceImpl.java).

```java
    private Map<String, Object> approveSingleCategory(Long id, String approver, TransactionTemplate transactionTemplate) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                GroupCategory entity = getById(id);
                
                // 1. Kiểm tra nguyên tắc phê duyệt kép (Maker-Checker)
                if (approver.equalsIgnoreCase(entity.getCreatedBy()) || approver.equalsIgnoreCase(entity.getUpdatedBy())) {
                    throw new IllegalStateException("Người phê duyệt (" + approver + ") không được trùng với người tạo/cập nhật yêu cầu!");
                }

                int statusBefore = entity.getStatus();

                // 2. Thiết lập và gọi Oracle Stored Procedure
                StoredProcedureQuery query = entityManager
                        .createStoredProcedureQuery("PROC_APPROVE_GROUP_CATEGORY");
                query.registerStoredProcedureParameter("p_id", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_user", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_status", Integer.class, ParameterMode.OUT);
                query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);
                
                query.setParameter("p_id", id);
                query.setParameter("p_user", approver);
                query.execute();

                // 3. Đọc kết quả trả về từ Procedure
                Object spStatusObj = query.getOutputParameterValue("p_status");
                String spMessage = (String) query.getOutputParameterValue("p_message");
                boolean success = false;
                if (spStatusObj instanceof Number) {
                    success = ((Number) spStatusObj).intValue() == 1;
                }

                res.put("success", success);
                res.put("message", spMessage);

                if (success) {
                    // Ghi log lịch sử hệ thống (Audit Log)
                    auditLogService.log(MODULE, String.valueOf(id), "Phê duyệt", approver, null, null,
                            String.format("Phê duyệt tham số ID=%d. SP: %s", id, spMessage), statusBefore, 4);
                } else {
                    throw new RuntimeException(spMessage);
                }
            });
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage() != null ? e.getMessage() : "Lỗi thực thi");
            log.error("[GroupCategory] Batch approve failed for id={}. error={}", id, e.getMessage());
        }
        return res;
    }
```

---

## ❓ Câu hỏi dành cho bạn:

### 1. Phân quyền và Bảo mật (Maker-Checker):
*   Tại sao code lại so sánh `approver` với cả `entity.getCreatedBy()` và `entity.getUpdatedBy()`? Nếu kết quả so sánh là trùng khớp (`true`), hành động gì sẽ xảy ra và hệ thống sẽ xử lý thế nào?

### 2. Ý nghĩa của các tham số Stored Procedure:
*   Trong đoạn code khai báo tham số cho `PROC_APPROVE_GROUP_CATEGORY`:
    *   Sự khác biệt giữa `ParameterMode.IN` và `ParameterMode.OUT` là gì?
    *   Hai tham số `p_status` và `p_message` được dùng để làm gì? Sau khi Procedure chạy xong, Java đọc kết quả từ hai tham số này như thế nào?

### 3. Giao dịch (Transaction):
*   Khi có lỗi xảy ra trong khối lệnh `transactionTemplate.executeWithoutResult` (ví dụ: Procedure trả về thất bại và quăng ra `throw new RuntimeException`), giao dịch thay đổi dữ liệu của bản ghi đó dưới Database sẽ bị xử lý như thế nào?

---

## ✍️ Phần trả lời của bạn:
1. Phải so sánh appover với entity.getCreatedBy() và entity.getUpdatedBy() vì hệ thống có 2 vai trò khác nhau là người tạo và người duyệt, nếu so sánh true thì sẽ thực hiện các lệnh được request, còn không thì sẽ không hiển thị các button hoặc là không thể thực hiện quyền
2. 
