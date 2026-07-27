# 📄 ĐẶC TẢ CHI TIẾT CA SỬ DỤNG HỆ THỐNG PAYMENT HUB (PMH)

**Dự án:** Payment Hub (PMH) - Hệ thống Quản lý Tham số Danh mục & Cấu phần Xử lý
**Tác giả:** Đội ngũ Phát triển Hệ thống / Sinh viên thực hiện Đồ án

---

## 1. TỔNG QUAN HỆ THỐNG CA SỬ DỤNG

Hệ thống **Payment Hub (PMH)** gồm 3 phân hệ nghiệp vụ chính:
1. **Phân hệ Quản lý Danh mục Tham số (`GroupCategory`)**
2. **Phân hệ Quản lý Cấu phần Xử lý (`ProcessingComponent`)**
3. **Phân hệ Nhật ký Kiểm toán (`AuditLog`)**

| Mã Use Case | Tên Use Case | Phân hệ | Tác nhân chính | Tóm tắt chức năng |
| :--- | :--- | :--- | :--- | :--- |
| **UC-GC-01** | Tạo mới Tham số Danh mục | `GroupCategory` | Maker (`USER01`) | Tạo tham số mới ở trạng thái khởi tạo (`STATUS = 1`). |
| **UC-GC-02** | Chỉnh sửa Tham số Danh mục | `GroupCategory` | Maker (`USER01`) | Nếu bản ghi đã duyệt (`STATUS = 4`), thay đổi sẽ được đóng gói thành JSON và lưu tạm vào cột `NEW_DATA`, đổi `STATUS = 3`. |
| **UC-GC-03** | Hủy yêu cầu chỉnh sửa / Xóa | `GroupCategory` | Maker (`USER01`) | Hủy thay đổi chưa duyệt (khôi phục về bản duyệt cũ) hoặc xóa vật lý nếu chưa từng duyệt. |
| **UC-GC-04** | Duyệt hàng loạt Danh mục | `GroupCategory` | Checker (`USER02`) | Giải mã JSON `NEW_DATA`, gọi Stored Procedure Oracle (`PROC_APPROVE_GROUP_CATEGORY`) ghi đè cột thật và đổi `STATUS = 4`. |
| **UC-GC-05** | Từ chối hàng loạt Danh mục | `GroupCategory` | Checker (`USER02`) | Gọi Stored Procedure Oracle (`PROC_REJECT_GROUP_CATEGORY`) chuyển `STATUS = 5` kèm lý do. |
| **UC-GC-06** | Xuất Dữ liệu Raw / Excel | `GroupCategory` | Maker / Checker | Xuất danh sách tham số dạng dữ liệu thô phục vụ báo cáo. |
| **UC-PC-01** | Quản lý Cấu phần Xử lý (CRUD) | `ProcessingComponent` | Maker (`USER01`) | Thêm, sửa, xóa, tìm kiếm danh mục các Cấu phần xử lý. |
| **UC-PC-02** | Duyệt / Từ chối Cấu phần | `ProcessingComponent` | Checker (`USER02`) | Duyệt hoặc từ chối duyệt các cấu phần xử lý chờ duyệt. |
| **UC-PC-03** | Truy vấn Cấu phần Hoạt động | `ProcessingComponent` | Maker / System | Lấy danh sách cấu phần đang hoạt động cho Dropdown tại màn hình Danh mục. |
| **UC-AL-01** | Tra cứu Nhật ký Kiểm toán | `AuditLog` | Auditor / Admin | Tìm kiếm và hiển thị biến động dữ liệu (`OLD_DATA` -> `NEW_DATA`) theo Module và Record ID. |

---

## 2. RÀNG BUỘC NGHIỆP VỤ CỐT LÕI (BUSINESS CONSTRAINTS)

1. **Nguyên tắc Phê duyệt 2 Bước (Four-Eyes Principle):**
   *   Người thực hiện phê duyệt (`approver`) **tuyệt đối không được trùng** với người tạo hoặc người cập nhật (`createdBy`, `updatedBy`).
   *   Nếu trùng, hệ thống dừng xử lý ID đó và trả về thông báo lỗi: *"Người phê duyệt không được trùng với người tạo/cập nhật yêu cầu!"*.
2. **Cơ chế Lưu trữ Tạm (`NEW_DATA`):**
   *   Khi chỉnh sửa bản ghi đang ở trạng thái đã duyệt (`STATUS = 4`), hệ thống **không sửa dữ liệu gốc ngay**.
   *   Hệ thống chuyển thông tin thay đổi thành định dạng JSON và lưu tạm vào cột **`NEW_DATA`**, đồng thời đổi `STATUS = 3` (Chờ duyệt).
   *   Khi Checker phê duyệt, hệ thống mới giải mã JSON và ghi đè vào các cột chính, sau đó làm sạch cột `NEW_DATA = null`.
3. **Quy tắc Phân trang Độc lập & Giao dịch Độc lập (Transaction Isolation):**
   *   Xử lý hàng loạt (`batchApprove`, `batchReject`) sử dụng `TransactionTemplate` với propagation `PROPAGATION_REQUIRES_NEW` cho từng bản ghi. Một bản ghi bị lỗi rollback không làm ảnh hưởng tới các bản ghi khác trong cùng danh sách batch.

---

## 3. ĐẶC TẢ CHI TIẾT TỪNG USE CASE CHÍNH

### 📌 UC-GC-01: Tạo mới Tham số Danh mục
*   **Tác nhân:** Maker (`USER01`).
*   **Tiền điều kiện:** Maker đã đăng nhập hệ thống.
*   **Các bước thực hiện:**
    1. Maker chọn nút "Tạo mới".
    2. Hệ thống mở biễu mẫu với Zod Schema Validation.
    3. Maker nhập thông tin: Tên tham số, Giá trị tham số, Loại tham số, Cấu phần liên kết, Ngày hiệu lực.
    4. Hệ thống kiểm tra trùng lặp bộ 3 `(PARAM_NAME, PARAM_VALUE, PARAM_TYPE)`.
    5. Hệ thống lưu bản ghi với `STATUS = 1`, `IS_DISPLAY = 1` và tự động ghi `PMH_AUDIT_LOG`.
*   **Ngoại lệ:** Trùng bộ 3 tham số -> Cảnh báo lỗi và dừng lưu.

---

### 📌 UC-GC-03: Hủy yêu cầu chỉnh sửa / Xóa bản ghi
*   **Tác nhân:** Maker (`USER01`).
*   **Kiểm tra phân quyền:** Chỉ tài khoản thuộc nhóm Chuyên viên (Maker) mới có quyền xóa/hủy.
*   **Các bước thực hiện:**
    *   **Trường hợp 1 (`IS_DISPLAY == 2` - Bản ghi đã từng duyệt vận hành):**
        *   Nếu `STATUS` đang là `3` (Chờ duyệt) hoặc `5` (Bị từ chối):
        *   Hệ thống xóa giá trị cột `NEW_DATA = null`, khôi phục `STATUS = 4` (Trở về bản ghi cũ đang hoạt động) và ghi Audit Log.
    *   **Trường hợp 2 (`IS_DISPLAY == 1` - Bản ghi chưa từng được duyệt):**
        *   Hệ thống xóa vật lý dòng dữ liệu khỏi Oracle Database.

---

### 📌 UC-GC-04: Phê duyệt Hàng loạt (Batch Approve)
*   **Tác nhân:** Checker (`USER02`).
*   **Các bước thực hiện:**
    1. Checker chọn danh sách các ID cần duyệt và bấm "Duyệt hàng loạt".
    2. Vòng lặp Backend xử lý từng ID trong một `TransactionTemplate` riêng biệt.
    3. Kiểm tra người duyệt: `if (approver.equalsIgnoreCase(createdBy) || approver.equalsIgnoreCase(updatedBy))` -> Báo lỗi rủi ro phân quyền.
    4. Nếu có `NEW_DATA != null`: Giải mã JSON và cập nhật vào các cột thuộc tính của Entity.
    5. Kích hoạt Stored Procedure `PROC_APPROVE_GROUP_CATEGORY(p_id, p_user)`.
    6. Stored Procedure set `STATUS = 4`, `IS_DISPLAY = 2`, `NEW_DATA = null`.
    7. Ghi nhận `PMH_AUDIT_LOG` cho hành động "Phê duyệt".
