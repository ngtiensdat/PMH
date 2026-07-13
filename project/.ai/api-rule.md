# Quy chuẩn Thiết kế API RESTful (API Rule)
## 1. Định dạng tài nguyên (Resources Routing)
- Danh từ số nhiều, phân tách từ bằng dấu gạch ngang (kebab-case).
- *Ví dụ chuẩn:*
  * `GET /api/processing-components`
  * `POST /api/processing-components`
  * `GET /api/processing-components/{id}`
  * `PUT /api/processing-components/{id}`

## 2. Định dạng phản hồi thống nhất (API Response Wrapper)
- Mọi API thành công hay thất bại đều trả về cấu trúc JSON bọc chung bởi `ApiResponse<T>` để phía Frontend dễ dàng xử lý đồng bộ:
  ```json
  {
    "code": "00", 
    "message": "Thành công",
    "data": { ... }
  }
  ```
- Mã code phản hồi thành công mặc định là `"00"`. Các mã lỗi nghiệp vụ khác bắt đầu bằng tiền tố `ERR-` (ví dụ: `ERR-01`: Bản ghi đã tồn tại, `ERR-02`: Không có quyền duyệt).

## 3. Exception Handling trung tâm
- Không dùng khối lệnh `try-catch` rác ở Controller.
- Sử dụng `@RestControllerAdvice` trong Spring Boot để bắt tất cả các Exception kế thừa từ `RuntimeException` toàn cục, log thông tin lỗi chi tiết ra file log server và trả về cho client mã lỗi chuẩn hóa kèm theo HTTP status code phù hợp (400 Bad Request, 401 Unauthorized, 404 Not Found, 500 Internal Error).
