# Tài liệu API Swagger (Online API Document)
Để thuận tiện cho việc kiểm thử và tích hợp giữa các bên, dự án tích hợp tài liệu trực tuyến **Swagger UI / OpenAPI 3**.

## Cách thức truy cập:
1. Đảm bảo Backend Spring Boot đã được khởi động.
2. Mở trình duyệt web và truy cập đường dẫn:
   `http://localhost:8080/swagger-ui/index.html`
3. Hoặc tải đặc tả file JSON OpenAPI thô tại:
   `http://localhost:8080/v3/api-docs`

## Các tính năng hỗ trợ trên Swagger:
- **Try it out**: Cho phép gửi thử dữ liệu trực tiếp lên API server để kiểm tra kết quả trả về thời gian thực.
- **Schemas Models**: Định nghĩa chi tiết cấu trúc kiểu dữ liệu của các DTO đầu vào và đầu ra kèm theo các thuộc tính bắt buộc, giúp lập trình viên Frontend dễ dàng viết Interface định nghĩa kiểu dữ liệu TypeScript tương ứng.
