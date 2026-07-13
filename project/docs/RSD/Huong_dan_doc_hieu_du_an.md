# HƯỚNG DẪN ĐỌC HIỂU VÀ PHÁT TRIỂN DỰ ÁN PAYMENT HUB

Tài liệu này cung cấp cái nhìn chi tiết về cấu trúc thư mục, kiến trúc thiết kế, luồng đi của dữ liệu và hướng dẫn từng bước để đọc hiểu toàn bộ mã nguồn của cả hai phần **Backend (Spring Boot)** và **Frontend (Angular)**.

---

## I. TỔNG QUAN HỆ THỐNG
Dự án **Payment Hub** là hệ thống quản lý danh mục và cấu phần xử lý thanh toán, bao gồm:
* **Backend**: Viết bằng Java (Spring Boot 3.3.1), quản lý dữ liệu qua JPA/Hibernate kết nối cơ sở dữ liệu Oracle, hỗ trợ cơ chế phê duyệt qua Stored Procedure và lưu lịch sử thao tác (Audit Log).
* **Frontend**: Viết bằng Angular (v17+), sử dụng bộ thư viện giao diện Taiga UI, giao tiếp với Backend qua RESTful API.

---

## II. CHI TIẾT CẤU TRÚC THƯ MỤC BACKEND
Backend được tổ chức theo kiến trúc phân tầng chuẩn (**Package-by-Layer**):

```text
code/backend/src/main/
├── java/com/example/paymenthub/
│   ├── common/                           # Các tiện ích và class dùng chung toàn dự án
│   │   ├── base/                         # Chứa các Base Classes
│   │   │   ├── BaseEntity.java           # MappedSuperclass chứa các cột audit (createdBy, createdDate...)
│   │   │   ├── BaseController.java       # Định nghĩa chung các hàm API phản hồi HTTP
│   │   │   ├── BaseService.java          # Interface CRUD dùng Generic <T, ID>
│   │   │   └── BaseServiceImpl.java      # Code triển khai CRUD dùng chung cho các Service
│   │   └── exception/                    # Xử lý ngoại lệ (Exception) tập trung
│   │       ├── GlobalExceptionHandler.java # Bắt và format các lỗi hệ thống trước khi trả về client
│   │       └── CustomBusinessException.java# Lỗi nghiệp vụ tự định nghĩa
│   │
│   ├── config/                           # Các cấu hình hệ thống
│   │   └── CorsConfig.java               # Cấu hình CORS cho phép Frontend truy cập API
│   │
│   ├── controller/                       # TẦNG API (Giao tiếp HTTP)
│   │   ├── ComponentController.java      # API quản lý Cấu phần xử lý
│   │   ├── GroupCategoryController.java  # API quản lý Tham số danh mục theo nhóm
│   │   └── AuditLogController.java       # API xem lịch sử thao tác
│   │
│   ├── dto/                              # DATA TRANSFER OBJECT (Định dạng dữ liệu truyền tải)
│   │   ├── request/                      # Dữ liệu client gửi lên (ComponentDTO, GroupCategoryDTO)
│   │   └── response/                     # Dữ liệu gửi trả về (AuditLogDTO)
│   │
│   ├── entity/                           # TẦNG ÁNH XẠ CƠ SỞ DỮ LIỆU (JPA Entities)
│   │   ├── ProcessingComponent.java      # Ánh xạ bảng PMH_COMPONENTS
│   │   ├── GroupCategory.java            # Ánh xạ bảng PMH_GROUP_CATEGORY
│   │   └── AuditLog.java                 # Ánh xạ bảng PMH_AUDIT_LOG
│   │
│   ├── mapper/                           # CHUYỂN ĐỔI DỮ LIỆU (DTO ⬌ Entity)
│   │   ├── ComponentMapper.java          
│   │   └── GroupCategoryMapper.java      
│   │
│   ├── repository/                       # TẦNG TRUY VẤN DỮ LIỆU (Spring Data JPA)
│   │   ├── ComponentRepository.java      # Thực hiện các câu lệnh SQL với bảng Components
│   │   ├── GroupCategoryRepository.java  # Thực hiện các câu lệnh SQL với bảng GroupCategory & gọi Procedure
│   │   ├── AuditLogRepository.java       # Thực hiện các câu lệnh SQL với bảng AuditLog
│   │   └── specification/                # Tìm kiếm động (Dynamic Query) bằng JPA Specification
│   │       ├── ComponentSpecification.java
│   │       └── GroupCategorySpecification.java
│   │
│   ├── service/                          # TẦNG NGHIỆP VỤ LÕI (Chứa logic xử lý)
│   │   ├── impl/                         # Thư mục bắt buộc chứa code thực thi chi tiết
│   │   │   ├── ComponentServiceImpl.java 
│   │   │   ├── GroupCategoryServiceImpl.java
│   │   │   └── AuditLogServiceImpl.java  
│   │   ├── ComponentService.java         # Interface định nghĩa hành động của cấu phần
│   │   ├── GroupCategoryService.java     # Interface định nghĩa hành động của danh mục theo nhóm
│   │   └── AuditLogService.java          # Interface định nghĩa hành động ghi nhật ký
│   │
│   └── PaymentHubApplication.java        # File chạy chính của ứng dụng Spring Boot
│
└── resources/
    ├── application.yml                   # Cấu hình port, cơ sở dữ liệu (url, username, password)
    └── db/migration/                     # Nơi lưu trữ script thay đổi Database
        └── V1__Init_Tables.sql           # Script khởi tạo cấu trúc bảng và procedure mẫu
```

---

## III. CHI TIẾT CẤU TRÚC THƯ MỤC FRONTEND
Frontend được tổ chức theo mô hình **Core-Shared-Features (CSF)** sạch sẽ và module hóa:

```text
code/frontend/src/app/
├── core/                                 # Các singleton dùng chung toàn cục (Khởi tạo 1 lần)
│   ├── constants/                        # Hằng số hệ thống
│   ├── guards/                           # Chặn định tuyến bảo mật (nếu có)
│   ├── interceptors/                     # Can thiệp HTTP requests (gắn token, xử lý lỗi chung)
│   └── services/                         # Dịch vụ toàn cục (Ví dụ: LanguageService xử lý ngôn ngữ)
│
├── shared/                               # Các thành phần tái sử dụng ở nhiều màn hình khác nhau
│   ├── components/                       # Các UI Component cơ bản (button, input, select, datepicker, notification)
│   ├── models/                           # Các Interface/Type dùng chung
│   ├── validators/                       # Các Schema kiểm tra dữ liệu đầu vào (Zod Schema)
│   └── utils/                            # Hàm phụ trợ dùng chung
│
├── layout/                               # Khung giao diện chính (Shell Layout)
│   ├── header/                           # Thanh điều hướng trên cùng (logo, chuông báo, đổi ngôn ngữ, profile)
│   └── sidebar/                          # Thanh điều hướng dọc (sidebar chính chứa icon và sidebar phụ sub-menu)
│
├── features/                             # CÁC MODULE NGHIỆP VỤ (Mỗi folder là một tính năng độc lập)
│   ├── category/                         # Module quản lý Danh mục theo nhóm
│   │   ├── components/                   # Giao diện con (list, detail, dialog form)
│   │   ├── services/                     # Dịch vụ gọi API riêng cho module category
│   │   └── category.routes.ts            # Định tuyến riêng của module category
│   │
│   └── processing-components/            # Module quản lý Cấu phần xử lý
│       ├── components/                   # Giao diện con (list, detail, dialog form)
│       ├── services/                     # Dịch vụ gọi API riêng cho module components
│       └── processing-components.routes.ts # Định tuyến riêng của module components
│
├── app.html                              # Template gốc (Layout Shell chứa app-header, app-sidebar, router-outlet)
├── app.ts                                # Component gốc khởi chạy ứng dụng
├── app.css                               # Style chung cho khung Layout
├── app.routes.ts                         # Định tuyến chính (Lazy Loading các features)
└── app.config.ts                         # Cấu hình ứng dụng Angular 17+ (Router, HttpClient, Animations)
```

---

## IV. HƯỚNG DẪN ĐỌC HIỂU DỰ ÁN THEO LUỒNG ĐI DỮ LIỆU

Để hiểu sâu dự án, hãy đọc code theo thứ tự đi của dữ liệu từ Database lên đến API tiếp nhận yêu cầu:

### Bước 1: Đọc Cơ sở dữ liệu và Ánh xạ thực thể (JPA Entity)
Đọc cách các bảng dữ liệu liên kết với nhau và cách Java biểu diễn chúng:
1. **Database schema**: Mở [V1__Init_Tables.sql](file:///e:/PMH/code/backend/src/main/resources/db/migration/V1__Init_Tables.sql), xem cấu trúc của bảng `PMH_GROUP_CATEGORY` có những cột nào, kiểu dữ liệu gì và khóa ngoại tham chiếu đến bảng cha `PMH_COMPONENTS`.
2. **JPA Entity**: Mở [GroupCategory.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/entity/GroupCategory.java).
   * Chú ý các Annotation như `@Entity`, `@Table`, `@Column` được dùng để khai báo ánh xạ cột.
   * Lớp này kế thừa [BaseEntity.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/common/base/BaseEntity.java) để tự động nhận các trường quản lý thời gian tạo (`createdDate`), cập nhật (`updatedDate`) và người tạo (`createdBy`).

### Bước 2: Đọc tầng truy vấn dữ liệu (Repository)
Hiểu cách Java giao tiếp và thực hiện các lệnh SQL với cơ sở dữ liệu:
1. Mở [GroupCategoryRepository.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/repository/GroupCategoryRepository.java).
   * Lớp này kế thừa `JpaRepository<GroupCategory, Long>` nên tự động có các hàm cơ bản (CRUD).
   * Chú ý annotation `@Procedure(name = "PROC_APPROVE_GROUP_CATEGORY")`: Đây là cách Spring Boot gọi trực tiếp Stored Procedure viết dưới DB Oracle để duyệt bản ghi.

### Bước 3: Đọc Định dạng truyền nhận dữ liệu (DTO và Mapper)
Xem cách dữ liệu được định dạng và kiểm tra (validate) trước khi đi vào xử lý nghiệp vụ:
1. Mở [GroupCategoryDTO.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/dto/request/GroupCategoryDTO.java).
   * Xem các Annotation `@NotBlank`, `@Size(max = 255)`... Đây là tầng bảo vệ đầu tiên của Backend, giúp từ chối ngay các request gửi thiếu hoặc sai định dạng dữ liệu.
2. Mở [GroupCategoryMapper.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/mapper/GroupCategoryMapper.java) để xem cách sao chép dữ liệu từ DTO sang Entity và ngược lại.

### Bước 4: Đọc Nghiệp vụ hệ thống (Service - Phần quan trọng nhất)
Nơi chứa toàn bộ logic xử lý chính của dự án:
1. Mở [GroupCategoryServiceImpl.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/service/impl/GroupCategoryServiceImpl.java).
2. **Logic Phê Duyệt Sửa Đổi (NEW_DATA)**:
   * Hãy đọc kỹ **hàm `update`**: Nếu một bản ghi đã được duyệt trước đó (`entity.getStatus() == 4`), hệ thống không cập nhật đè ngay lên Database. Nó sẽ chuyển các giá trị thay đổi thành một chuỗi JSON, lưu vào cột `NEW_DATA`, và đưa trạng thái về chờ duyệt (`STATUS = 3`).
   * Hãy đọc kỹ **hàm `batchApprove`**: Khi người kiểm soát duyệt bản ghi sửa đổi, hệ thống sẽ đọc chuỗi JSON từ cột `NEW_DATA`, áp dụng (apply) ngược lại vào các trường tương ứng của Entity, rồi mới gọi Stored Procedure để hoàn tất duyệt.
3. **Logic Ghi Log Lịch Sử (Audit Log)**:
   * Hầu hết mọi hành động thêm/sửa/xóa/gửi duyệt/phê duyệt đều gọi đến `auditLogService.log` để lưu trạng thái trước và sau khi đổi vào bảng `PMH_AUDIT_LOG`.

### Bước 5: Đọc Cổng API (Controller)
Xem cách API tiếp nhận yêu cầu và trả về dữ liệu cho Frontend:
1. Mở [GroupCategoryController.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/controller/GroupCategoryController.java).
   * Các annotation `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` tương ứng với các phương thức truyền HTTP.
   * `@Valid` kích hoạt cơ chế kiểm tra dữ liệu của DTO.
   * Trả về dữ liệu bọc trong `ResponseEntity.ok(...)` để có HTTP Status thích hợp (200 OK, 201 Created...).

---

## V. HƯỚNG DẪN KHỞI CHẠY VÀ DEBUG DỰ ÁN

### 1. Khởi chạy Backend
* **Từ Terminal**: Đi tới thư mục `code/backend` và chạy lệnh maven (nếu máy có cài Maven):
  ```powershell
  mvn spring-boot:run
  ```
* **Từ VS Code**:
  1. Mở file [PaymentHubApplication.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/PaymentHubApplication.java).
  2. Click vào chữ **`Run`** xuất hiện ngay phía trên hàm `main` để khởi động.
  3. Cổng chạy mặc định là **8080** (xem cấu hình tại [application.yml](file:///e:/PMH/code/backend/src/main/resources/application.yml)).

### 2. Khởi chạy Frontend
* **Từ Terminal**:
  1. Di chuyển vào thư mục frontend:
     ```powershell
     cd e:\PMH\code\frontend
     ```
  2. Khởi động môi trường phát triển:
     ```powershell
     npm start
     ```
  3. Truy cập trình duyệt tại địa chỉ: **[http://localhost:4200](http://localhost:4200)**.
