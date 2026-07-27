# 🚀 HƯỚNG DẪN PROMPT HỆ THỐNG DỰ ÁN PAYMENT HUB (PMH)

> **Mục đích:** File này lưu trữ toàn bộ kiến thức nghiệp vụ, thiết kế cơ sở dữ liệu và **quy tắc bố cục sơ đồ trực quan chống chồng chéo** của hệ thống Payment Hub. Mỗi khi khởi tạo phiên làm việc mới với AI, chỉ cần tham chiếu đến file này để AI luôn đọc và vẽ ra các sơ đồ đẹp mắt, chuẩn xác và dễ nhìn nhất.

---

## 📌 1. TỔNG QUAN PHẠM VI DỰ ÁN (PROJECT SCOPE)

Hệ thống **Payment Hub (PMH)** gồm 3 phân hệ chính:
1. **Phân hệ Quản lý Danh mục Tham số (`GroupCategory`):** Quản lý bộ tham số danh mục theo nhóm, lưu JSON tạm vào `NEW_DATA`, duyệt/từ chối hàng loạt.
2. **Phân hệ Quản lý Cấu phần Xử lý (`ProcessingComponent`):** Quản lý danh mục các cấu phần xử lý thanh toán (REST/SOAP/ISO8583).
3. **Phân hệ Nhật ký Kiểm toán (`AuditLog`):** Tra cứu vết biến động dữ liệu (`OLD_DATA` -> `NEW_DATA`) và tự động ghi nhật ký hệ thống.

---

## 🔐 2. RÀNG BUỘC NGHIỆP VỤ CỐT LÕI (CORE CONSTRAINTS)

### 2.1 Nguyên tắc Phê duyệt 2 Bước (Four-Eyes Principle)
*   **Maker (Người nhập/Tạo/Sửa):** Tạo hoặc chỉnh sửa dữ liệu.
*   **Checker (Người duyệt):** Phê duyệt (`batchApprove`) hoặc Từ chối (`batchReject`).
*   **Ràng buộc bảo mật:** Người duyệt (`approver`) **tuyệt đối không được trùng** với người tạo hoặc cập nhật bản ghi (`createdBy`, `updatedBy`). Nếu trùng, hệ thống trả về lỗi rủi ro phân quyền và rollback ID đó.

### 2.2 Cơ chế Lưu trữ Tạm (`NEW_DATA`)
*   Khi sửa bản ghi đã duyệt (`STATUS = 4`), hệ thống không sửa trực tiếp cột thật mà đóng gói thông tin thay đổi thành chuỗi **JSON** và lưu vào cột **`NEW_DATA`**, đổi `STATUS = 3` (Chờ duyệt).
*   Khi Checker duyệt: Giải mã JSON trong `NEW_DATA`, ghi đè vào các cột thật, xóa `NEW_DATA = null` và đổi `STATUS = 4` (Đã duyệt), `IS_DISPLAY = 2`.

### 2.3 Quản lý Trạng thái & Cơ chế Hủy/Xóa
*   **Danh sách `STATUS`:** `1` (Khởi tạo), `3` (Chờ duyệt), `4` (Đã duyệt / Đang vận hành), `5` (Từ chối).
*   **Danh sách `IS_DISPLAY`:** `1` (Chưa từng duyệt), `2` (Đã từng duyệt vận hành).
*   **Cơ chế Hủy sửa:** Nếu bản ghi có `IS_DISPLAY == 2` và `STATUS IN (3, 5)`, thao tác "Xóa" đóng vai trò **Hủy yêu cầu sửa** (Khôi phục `STATUS = 4` và xóa `NEW_DATA = null`).
*   **Cơ chế Xóa vật lý:** Chỉ áp dụng khi `IS_DISPLAY == 1` (Bản ghi mới khởi tạo chưa từng được duyệt).

---

## 📐 3. QUY TẮC BỐ CỤC & TÍNH TOÁN KÍCH THƯỚC TRÁNH CHỒNG CHÉO (LAYOUT & VISUAL RULES)

Khi vẽ bất kỳ sơ đồ nào (Use Case, Sequence, ERD, Flowchart) cho dự án này trên Draw.io hoặc Mermaid, AI **bắt buộc tuân thủ các nguyên tắc thiết kế trực quan** sau:

### 3.1 Tính toán Tọa độ & Khoảng cách (Positioning & Spacing Rules)
1. **Tuyệt đối không đè các khối/bong bóng (No Overlapping Nodes):**
   *   Mỗi bong bóng/khối Use Case phải có chiều rộng tối thiểu `150px - 180px` và chiều cao `50px - 60px`.
   *   Khoảng cách chiều ngang (Horizontal Gap) giữa 2 khối tối thiểu là `60px - 90px`.
   *   Khoảng cách chiều dọc (Vertical Gap) giữa 2 khối tối thiểu là `25px - 40px`.
2. **Phân vùng Ranh giới Hệ thống (Swimlane / System Boundary):**
   *   Tất cả các Use Cases thuộc cùng một phân hệ phải nằm gọn trong khung phân khu (Sub-container) của phân hệ đó.
   *   Tọa độ các Use Cases bên trong khung phải tính theo tọa độ tương đối so với góc trên bên trái của khung (`parent="container_id"`).

### 3.2 Quy tắc Đường đi Mũi tên & Điểm neo (Connector & Anchor Point Rules)
1. **Chỉ định điểm đính chính xác (Anchor Points):**
   *   Mũi tên xuất phát từ Tác nhân bên trái (Maker/Auditor) phải nối từ mép phải tác nhân (`exitX=1; exitY=0.5`) đến mép trái của khối Use Case (`entryX=0; entryY=0.5`).
   *   Mũi tên xuất phát từ Tác nhân bên phải (Checker/Database) phải nối từ mép trái tác nhân (`exitX=0; exitY=0.5`) đến mép phải của khối Use Case (`entryX=1; entryY=0.5`).
2. **Không cho đường nối đè qua chữ hoặc ô khác (Clean Edge Routing):**
   *   Tránh để đường mũi tên cắt ngang qua thân của ô chữ nhật hoặc bong bóng Use Case khác.
   *   Đường nối `<<include>>` đứt nét phải sử dụng `labelBackgroundColor=none` hoặc nhãn nổi để không che phủ chữ trong bong bóng.

### 3.3 Phối màu sắc & Nhận diện Trực quan (Color Palette & Styling)
*   **Maker / Tác nhân Nhập liệu:** Màu Xanh lam nhạt (`#dae8fc`, viền `#6c8ebf`).
*   **Checker / Tác nhân Phê duyệt:** Màu Hồng nhạt (`#f8cecc`, viền `#b85450`).
*   **Auditor / Tác nhân Kiểm toán:** Màu Xanh lá nhạt (`#d5e8d4`, viền `#82b366`).
*   **Hành động Phê duyệt thành công:** Ô Ellipse tô màu Xanh lá nhạt.
*   **Hành động Từ chối:** Ô Ellipse tô màu Hồng/Đỏ nhạt.
*   **Database Oracle:** Khối Datastore tô màu Vàng nhạt (`#fff2cc`, viền `#d6b656`).

---

## 📂 4. DANH MỤC FILE TÀI LIỆU DỰ ÁN

| Tên File | Mô Tả Chi Tiết |
| :--- | :--- |
| [01_USECASE_SPECIFICATION.md](file:///e:/PMH/project/docs/01_USECASE_SPECIFICATION.md) | Đặc tả chi tiết tất cả Use Case của 3 phân hệ (`GroupCategory`, `ProcessingComponent`, `AuditLog`). |
| [02_SEQUENCE_DIAGRAM_SPEC.md](file:///e:/PMH/project/docs/02_SEQUENCE_DIAGRAM_SPEC.md) | Đặc tả sơ đồ tuần tự tương tác chi tiết từ UI -> Controller -> TransactionTemplate -> Stored Procedure. |
| [03_DATABASE_DESIGN.md](file:///e:/PMH/project/docs/03_DATABASE_DESIGN.md) | Thiết kế CSDL chi tiết cho 3 bảng `PMH_GROUP_CATEGORY`, `PMH_COMPONENTS`, `PMH_AUDIT_LOG` kèm ERD. |
| [usecase_diagram.drawio](file:///e:/PMH/project/docs/diagrams/usecase_diagram.drawio) | File sơ đồ Use Case chuẩn XML cho Draw.io (Đã tối ưu tọa độ chống chồng chéo 100%). |
| [sequence_maker_checker.drawio](file:///e:/PMH/project/docs/diagrams/sequence_maker_checker.drawio) | File sơ đồ Tuần tự chuẩn XML cho Draw.io mô tả luồng duyệt Maker-Checker. |
