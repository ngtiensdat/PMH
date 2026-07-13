# Quy trình phân tích và triển khai từ RSD (Implement RSD Workflow)
1. **Phân tích tài liệu**: Đọc kỹ tài liệu RSD PDF trong thư mục `project/docs/RSD/` để nắm rõ các yêu cầu nghiệp vụ và cấu trúc dữ liệu mô tả.
2. **Thiết kế cơ sở dữ liệu**:
   - Xác định các bảng dữ liệu chính cần tác động.
   - Viết các script SQL tạo/sửa bảng, thêm các cột mới tương ứng.
3. **Lập trình API Backend**:
   - Tạo Java JPA Entity ánh xạ với bảng cơ sở dữ liệu.
   - Tạo Repository và Service xử lý logic lưu trữ/duyệt (Maker - Checker).
   - Expose API thông qua Controller.
4. **Lập trình Giao diện Frontend**:
   - Xây dựng Zod validation schema khớp với định dạng dữ liệu Backend.
   - Lập trình Service gọi API, liên kết dữ liệu vào Component Angular.
   - Thiết kế giao diện HTML/CSS tích hợp component Taiga UI.
5. **Kiểm thử liên thông (Integration Testing)**: Chạy thử toàn bộ luồng từ giao diện bấm Lưu -> Dữ liệu lưu bảng tạm -> Checker duyệt -> Cập nhật bảng chính.
