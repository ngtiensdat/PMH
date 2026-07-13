# Đặc tả Kiến trúc Hệ thống (System Architecture)
## 1. Mô hình tổng quát (Client-Server Split)
Hệ thống được chia làm hai phần tách biệt vật lý để bảo mật và độc lập triển khai:
- **Frontend Layer**: Chạy Single Page Application (SPA) trên Client, sử dụng Angular để tối ưu bộ nhớ trình duyệt và giao diện người dùng mượt mà.
- **Backend RESTful API**: Spring Boot đóng vai trò không trạng thái (Stateless Service), cung cấp dữ liệu qua JSON.

## 2. Kiến trúc Backend (Layered Architecture & SOLID)
Tuân thủ nghiêm ngặt mô hình 4 tầng độc lập:
```
[HTTP Request] 
      │
      ▼
┌──────────────┐
│  Controller  │  <-- Tiếp nhận HTTP request, phân quyền, validate định dạng sơ bộ.
└──────┬───────┘
       │ (DTO)
       ▼
┌──────────────┐
│   Service    │  <-- Nơi duy nhất chứa Logic nghiệp vụ và điều phối Transaction.
└──────┬───────┘
       │ (JPA Entity)
       ▼
┌──────────────┐
│  Repository  │  <-- Giao tiếp dữ liệu qua JPA/Spring Data hoặc Stored Procedures.
└──────┬───────┘
       │ (SQL Call)
       ▼
┌──────────────┐
│ Oracle DB 19c│  <-- Cơ sở dữ liệu quan hệ lưu trữ thông tin nghiệp vụ và log.
└──────────────┘
```

## 3. Kiến trúc Frontend (Feature-Driven Standalone)
Tổ chức code theo từng Module nghiệp vụ độc lập, giúp tránh hiện tượng "Monolithic component" khó bảo trì:
- **Core Layer**: Chứa Interceptor (đính kèm header bảo mật, xử lý lỗi chung), Guards (phân quyền tuyến), Service dùng chung (đa ngôn ngữ).
- **Features Layer**: Mỗi phân hệ lớn (ví dụ: `category`, `processing-components`) tự chứa:
  - `components/`: Các màn hình danh sách, chi tiết, popup thêm sửa.
  - `services/`: Dịch vụ gọi API cụ thể của module đó.
  - `routes/`: Định cấu hình Lazy Loading routing riêng của module.
- **Shared Layer**: Validator schema (Zod), models dùng chung, constants trạng thái.
