# FILE HỌC CODE THỰC CHIẾN - PHẦN 5 (LEARN05)

Một kết quả tuyệt vời khác! Bạn đang nắm chắc phần cốt lõi của kết nối Database JPA Specification.
Bài học số 5 này sẽ đi sâu vào một trong những khái niệm quan trọng nhất của lập trình Java Enterprise: **Quản lý Giao dịch (Transaction Management)** bằng mã nguồn.

---

## 💻 Đoạn code: Khai báo Transaction trong xử lý theo lô (Batch Processing)

Đoạn code này nằm trong file [GroupCategoryServiceImpl.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/service/impl/GroupCategoryServiceImpl.java).

```java
    @Override
    public List<Map<String, Object>> batchApprove(List<Long> ids, String approver) {
        log.info("[GroupCategory] Batch approve started. count={}, approver={}", ids.size(), approver);
        List<Map<String, Object>> results = new ArrayList<>();
        
        // 1. Khởi tạo TransactionTemplate để quản lý transaction thủ công
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (Long id : ids) {
            // 2. Chạy hàm xử lý duyệt cho từng bản ghi độc lập
            results.add(approveSingleCategory(id, approver, transactionTemplate));
        }

        return results;
    }
```

---

## ❓ Câu hỏi dành cho bạn:

### 1. Phân biệt hai loại Quản lý Transaction:
*   Trong lớp Service, chúng ta đã khai báo chú thích `@Transactional` ở đầu class (Declarative Transaction). Tại sao trong hàm `batchApprove` này ta lại cần khởi tạo thêm đối tượng `TransactionTemplate` (Programmatic Transaction)?

### 2. Ý nghĩa của cờ lan truyền (Propagation):
*   Tham số `PROPAGATION_REQUIRES_NEW` có ý nghĩa gì?
*   Giả sử Checker tích chọn duyệt 5 bản ghi. Trong quá trình chạy vòng lặp, bản ghi thứ 3 bị lỗi dữ liệu và bị **Rollback**. Theo bạn, 4 bản ghi còn lại (1, 2, 4, 5) có được phê duyệt thành công xuống Database hay không? Tại sao?

### 3. Vòng đời của một Transaction trong vòng lặp:
*   Mỗi khi vòng lặp `for` chạy qua một `id` và gọi hàm `approveSingleCategory(...)`, một transaction mới được mở ra hay dùng lại transaction cũ? Khi nào thì transaction của một bản ghi được Commit chính thức?

---

## ✍️ Phần trả lời của bạn:
*(Bạn hãy gõ câu trả lời của mình trực tiếp vào ô chat để chúng ta cùng thảo luận nhé!)*
