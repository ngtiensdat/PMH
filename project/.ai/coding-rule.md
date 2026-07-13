# Quy tắc Viết mã và Quản lý Mã nguồn (Coding Rule)
## 1. Nguyên lý Sửa đổi Nhỏ nhất (Minimal Patch Protocol)
- Chỉ sửa đúng phạm vi yêu cầu nghiệp vụ được giao.
- **KHÔNG** tự ý định dạng lại toàn bộ file code (Format/Beautify), sắp xếp lại dòng import nếu không thay đổi logic. Việc này giúp việc so sánh code (Git Diff) sạch sẽ, dễ phát hiện lỗi khi kiểm duyệt.
- Giữ nguyên coding style cũ của dự án.

## 2. Đặt tên và Chú thích (Naming & Comments)
- Tên Class/Hàm/Biến phải đặt bằng tiếng Anh mang tính gợi nhớ rõ ràng.
- Đặt tên biến camelCase, tên Class PascalCase, tên hằng số UPPER_SNAKE_CASE.
- Viết chú thích (comments) ngắn gọn bằng tiếng Việt trước các hàm xử lý nghiệp vụ phức tạp.

## 3. Quy trình Git Commit
- Bắt buộc commit code theo chuẩn **Conventional Commits**:
  * `feat(category): thêm tính năng đối chiếu dữ liệu`
  * `fix(components): sửa lỗi không nhận diện ID khi thêm mới`
  * `chore(taiga): gộp imports vào shared module`
- Không commit các file tạm, file log, thư mục cấu hình cá nhân IDE (`.idea`, `.vscode`, `target/`, `node_modules/`). Sử dụng file `.gitignore` chuẩn mực.

## 4. Quy tắc Sửa đổi Cơ sở dữ liệu (Database Modifications)
- **CẤM TUYỆT ĐỐI** tự ý thực hiện thay đổi cấu trúc bảng (DDL), thêm/xóa bảng, chỉnh sửa cột dữ liệu, sửa đổi Stored Procedure hoặc dữ liệu hạt giống (seed data) trong cơ sở dữ liệu (Oracle DB) nếu chưa được sự đồng ý và cấp phép rõ ràng từ Leader hoặc Kiến trúc sư hệ thống.

