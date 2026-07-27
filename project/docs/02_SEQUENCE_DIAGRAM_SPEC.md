# 🔄 ĐẶC TẢ SƠ ĐỒ TUẦN TỰ (SEQUENCE DIAGRAM SPECIFICATION)

**Dự án:** Payment Hub (PMH)
**Chủ đề:** Quy trình Phê duyệt Hàng loạt (Batch Approve) & Lưu trữ Tạm (`NEW_DATA`)

---

## 1. MÔ TẢ LUỒNG TƯƠNG TÁC KỸ THUẬT

Quy trình xử lý Maker-Checker bao gồm tương tác qua 4 tầng:
1. **Frontend (Angular 17+):** Người dùng thao tác UI, Form Zod validation & Signals.
2. **Backend Controller / Service (Spring Boot):** Tiếp nhận DTO, quản lý `TransactionTemplate` độc lập.
3. **Audit Log Service:** Ghi nhật ký biến động trạng thái.
4. **Database (Oracle Stored Procedure):** Thực thi cập nhật dữ liệu gốc & kiểm soát toàn vẹn.

---

## 2. MERMAID SEQUENCE DIAGRAM

```mermaid
sequenceDiagram
    autonumber
    actor Maker as Maker (USER01)
    actor Checker as Checker (USER02)
    participant UI as Angular Frontend
    participant API as GroupCategoryServiceImpl
    participant Tx as TransactionTemplate (REQUIRES_NEW)
    participant SP as Oracle Stored Procedure (PROC_APPROVE)
    participant DB as Oracle Database

    %% --- PHASE 1: MAKER EDIT APPROVED RECORD ---
    rect rgb(240, 240, 255)
        note over Maker, DB: GIAI ĐOẠN 1: MAKER SỬA BẢN GHI ĐÃ DUYỆT (STATUS = 4)
        Maker->>UI: Nhập thông tin thay đổi & bấm "Lưu"
        UI->>UI: Validate Form với Zod Schema
        UI->>API: PUT /api/group-category/{id} (GroupCategoryDTO)
        API->>API: Kiểm tra Status == 4
        API->>API: Đóng gói các trường sửa thành chuỗi JSON (NEW_DATA)
        API->>DB: Save Entity (NEW_DATA = json, STATUS = 3, UPDATED_BY = USER01)
        DB-->>API: Lưu thành công
        API-->>UI: Trả về kết quả (STATUS = 3 - Chờ duyệt)
        UI-->>Maker: Thông báo "Gửi duyệt sửa thành công"
    end

    %% --- PHASE 2: CHECKER BATCH APPROVE ---
    rect rgb(240, 255, 240)
        note over Checker, DB: GIAI ĐOẠN 2: CHECKER DUYỆT HÀNG LOẠT (BATCH APPROVE)
        Checker->>UI: Chọn danh sách ID & bấm "Duyệt hàng loạt"
        UI->>API: POST /api/group-category/batch-approve (List<Long> ids)
        
        loop Cho từng ID trong danh sách (Vòng lặp độc lập Transaction)
            API->>Tx: executeWithoutResult(...)
            Tx->>API: Check Phân quyền (Approver != CreatedBy/UpdatedBy)
            alt Người duyệt trùng người tạo/sửa
                API-->>Tx: Throw IllegalStateException
                Tx-->>API: Rollback chỉ cho ID hiện tại
            else Phân quyền hợp lệ
                alt NEW_DATA != null
                    API->>API: Parse JSON từ NEW_DATA -> Cập nhật đè lên các thuộc tính Entity
                    API->>DB: saveAndFlush(Entity)
                end
                API->>SP: Gọi PROC_APPROVE_GROUP_CATEGORY(p_id, p_user)
                SP->>DB: UPDATE STATUS = 4, IS_DISPLAY = 2, NEW_DATA = NULL
                DB-->>SP: OK
                SP-->>API: Trả về p_status = 1, p_message = "Thành công"
                API->>DB: Ghi Audit Log (Action = "Phê duyệt", Status = 4)
            end
        end
        
        API-->>UI: Trả về danh sách kết quả (List<ResultMap>)
        UI-->>Checker: Hiển thị dialog tổng hợp kết quả duyệt
    end
```

---

## 3. FILE VẼ SƠ ĐỒ TUẦN TỰ DẠNG DRAW.IO
File sơ đồ này được lưu trữ tại [sequence_maker_checker.drawio](file:///e:/PMH/project/docs/diagrams/sequence_maker_checker.drawio) phục vụ cho việc nhúng vào báo cáo đồ án.
