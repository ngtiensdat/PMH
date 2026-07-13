# Quy trình Phát triển Tính năng mới (Build Feature Workflow)
1. **Chuẩn bị nhánh Git**: Tạo nhánh mới từ `main` theo định dạng: `feature/ten-tinh-nang`.
2. **Thống nhất API Contract**: Định nghĩa chính xác Request DTO và Response DTO, ghi nhận vào tài liệu đặc tả API [api-spec.md](file:///e:/PMH/project/docs/API/api-spec.md).
3. **Thực thi Backend**: Code theo đúng các tầng lớp kiến trúc, viết unit test nếu cần thiết.
4. **Thực thi Frontend**: Import các component giao diện từ SharedTaigaModule, viết logic xử lý dữ liệu động bằng Signals.
5. **Kiểm tra cục bộ**: Chạy lệnh build để phát hiện sớm các lỗi cú pháp biên dịch:
   - Backend: `mvn clean compile`
   - Frontend: `npm run build`
