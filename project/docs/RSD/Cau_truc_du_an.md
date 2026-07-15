# BẢN ĐỒ CẤU TRÚC CHI TIẾT DỰ ÁN PAYMENT HUB

Tài liệu này vẽ lại toàn bộ sơ đồ cấu trúc mã nguồn thực tế của dự án **Payment Hub** (bao gồm cả Backend Spring Boot và Frontend Angular) cùng với giải nghĩa chi tiết vai trò của từng thư mục, từng file trong hệ thống.

---

## I. SƠ ĐỒ CẤU TRÚC TỔNG THỂ DỰ ÁN
Dự án được tổ chức tách biệt làm hai phần: `code` (mã nguồn ứng dụng) và `project` (tài liệu thiết kế và AI Agent).

```text
PMH/
├── code/
│   ├── backend/                 # Mã nguồn Backend (Spring Boot 3)
│   └── frontend/                # Mã nguồn Frontend (Angular 17)
└── project/
    ├── .agents/                 # Prompts định hình vai trò của các AI Agent
    ├── docs/                    # Tài liệu đặc tả hệ thống (API, ERD, Flow...)
    │   ├── API/
    │   ├── ERD/
    │   ├── Flow/
    │   ├── RSD/                 # Tài liệu hướng dẫn nghiệp vụ & cấu trúc
    │   └── Swagger/
    └── .workflows/              # Cấu hình luồng tự động (Workflows)
```

---

## II. CHI TIẾT CẤU TRÚC BACKEND (SPRING BOOT)

Backend được tổ chức theo mô hình phân lớp tiêu chuẩn **Package-by-Layer** kết hợp Base Classes để tối ưu hóa mã nguồn CRUD.

```text
code/backend/src/main/java/com/example/paymenthub/
├── PaymentHubApplication.java    # File chạy chính của ứng dụng
├── common/                       # Tiện ích cốt lõi dùng chung toàn dự án
│   ├── base/                     # Các class nền tảng (Base Classes)
│   │   ├── BaseController.java   # Định nghĩa chung cho các API controller phản hồi
│   │   ├── BaseEntity.java       # Các cột audit tự động (created_by, created_date...)
│   │   ├── BaseService.java      # Định nghĩa chung cho tầng nghiệp vụ (CRUD generic)
│   │   ├── BaseServiceImpl.java  # Triển khai các hàm CRUD dùng chung
│   │   ├── ApiResponse.java      # Cấu trúc JSON chuẩn trả về Client
│   │   └── PageResponse.java     # Cấu trúc phân trang JSON chuẩn
│   └── exception/                # Bộ xử lý lỗi tập trung
│       ├── CustomBusinessException.java # Định nghĩa lỗi nghiệp vụ tùy biến
│       └── GlobalExceptionHandler.java  # Bắt lỗi toàn hệ thống và format chuẩn JSON
├── config/                       # Các cấu hình hệ thống
│   └── CorsConfig.java           # Cho phép Frontend Angular gọi API không bị chặn CORS
├── controller/                   # Cổng API RESTful đón nhận HTTP Requests
│   ├── AuditLogController.java   # API xem lịch sử thao tác tham số/cấu phần
│   ├── ComponentController.java  # API CRUD và duyệt cấu phần xử lý
│   └── GroupCategoryController.java # API CRUD và duyệt tham số danh mục theo nhóm
├── dto/                          # Định dạng truyền nhận dữ liệu giữa Client và Server
│   ├── request/                  # Dữ liệu Client gửi lên (Chứa validation ràng buộc)
│   │   ├── ComponentDTO.java     
│   │   └── GroupCategoryDTO.java 
│   └── response/                 # Dữ liệu Server trả về cho Client
│       ├── AuditLogDTO.java      
│       ├── ComponentResponseDTO.java
│       └── GroupCategoryResponseDTO.java
├── entity/                       # Ánh xạ cơ sở dữ liệu (JPA Entities)
│   ├── AuditLog.java             # Map với bảng nhật ký PMH_AUDIT_LOG
│   ├── ProcessingComponent.java  # Map với bảng cấu phần PMH_COMPONENTS
│   └── GroupCategory.java        # Map với bảng tham số PMH_GROUP_CATEGORY
├── mapper/                       # Bộ chuyển đổi dữ liệu (DTO ⬌ Entity)
│   ├── ComponentMapper.java      
│   └── GroupCategoryMapper.java  
├── repository/                   # Tầng truy vấn cơ sở dữ liệu (Spring Data JPA)
│   ├── AuditLogRepository.java   # Truy vấn lịch sử
│   ├── ComponentRepository.java  # Truy vấn cấu phần xử lý
│   ├── GroupCategoryRepository.java # Truy vấn danh mục theo nhóm & khai báo Stored Procedure
│   └── specification/            # Truy vấn bộ lọc động (JPA Specification)
│       ├── ComponentSpecification.java
│       └── GroupCategorySpecification.java
└── service/                      # Tầng nghiệp vụ lõi (Chứa logic xử lý chính)
    ├── impl/                     # Hiện thực hóa chi tiết logic nghiệp vụ
    │   ├── AuditLogServiceImpl.java     # Logic ghi nhận vết lịch sử kèm IP client
    │   ├── ComponentServiceImpl.java    # Logic Maker-Checker, Stored Procedure của Cấu phần
    │   └── GroupCategoryServiceImpl.java # Logic Maker-Checker, Stored Procedure của Tham số
    ├── AuditLogService.java      
    ├── ComponentService.java     
    └── GroupCategoryService.java 
```

### Giải nghĩa vai trò các file Backend chính:

| File / Thư mục | Vai trò cụ thể trong hệ thống |
| :--- | :--- |
| **`BaseEntity.java`** | Lớp cha tích hợp sẵn `@MappedSuperclass` chứa các trường `createdBy`, `createdDate`, `updatedBy`, `updatedDate` giúp mọi bảng tự động ghi nhận vết audit thời gian mà không cần viết lại. |
| **`GlobalExceptionHandler.java`** | Bắt toàn bộ lỗi (như Validation lỗi, lỗi DB, lỗi Logic) để định dạng lại thành chuỗi JSON phản hồi thống nhất `{ "code": "99", "message": "Chi tiết lỗi" }` giúp Frontend xử lý trực quan hơn. |
| **`AuditLogServiceImpl.java`** | Triển khai ghi log lịch sử. Sử dụng `RequestContextHolder` để lấy địa chỉ IP của Client gửi đến và thực hiện ghi log trong một Transaction mới cô lập hoàn toàn (`PROPAGATION_REQUIRES_NEW`) tránh lỗi lan truyền. |
| **`GroupCategoryServiceImpl.java`** | Chứa logic Maker-Checker nghiệp vụ: Khi sửa dữ liệu đã duyệt, nó không cập nhật đè mà chuyển đổi thành JSON lưu vào cột `NEW_DATA` chờ duyệt. Khi duyệt, nó trích xuất ngược JSON ghi đè cột thật và gọi Oracle Stored Procedure. |
| **`GroupCategoryRepository.java`** | Chứa phương thức ánh xạ `@Procedure` để gọi trực tiếp các Stored Procedure duyệt dữ liệu (`PROC_APPROVE_GROUP_CATEGORY`) nằm trong nhân DB Oracle. |

---

## III. CHI TIẾT CẤU TRÚC FRONTEND (ANGULAR)

Frontend được viết bằng Angular 17+, sử dụng mô hình kiến trúc module hóa **Core-Shared-Features (CSF)** giúp quản lý code sạch sẽ, dễ mở rộng và hỗ trợ Lazy Loading.

```text
code/frontend/src/app/
├── app.config.ts                 # Cấu hình ứng dụng Angular (Router, HttpClient, Animations...)
├── app.routes.ts                 # Định tuyến chính (Lazy loading các feature modules)
├── app.ts                        # Component gốc khởi chạy ứng dụng
├── app.html                      # Layout khung chính của trang (Header, Sidebar, Router Outlet)
├── app.css                       # Stylesheet chung cho khung layout
├── layout/                       # Giao diện khung trang web
│   ├── header/                   # Thanh công cụ trên cùng (Thay đổi ngôn ngữ, User profile)
│   └── sidebar/                  # Thanh điều hướng bên trái (Quản lý các cấp menu)
├── core/                         # Các thành phần singleton (Khởi tạo duy nhất 1 lần)
│   ├── constants/                
│   │   └── labels.ts             # Lưu trữ dữ liệu text song ngữ Anh - Việt
│   └── services/                 
│       └── language.service.ts   # Quản lý ngôn ngữ và chuyển đổi đa ngôn ngữ dùng Angular Signals
├── shared/                       # Các thành phần tái sử dụng ở nhiều màn hình
│   ├── components/               # UI Components dùng chung (Input, Dialog, Notification...)
│   │   └── notification/         # Dịch vụ hiển thị thông báo popup Toast
│   ├── models/                   # Các Interfaces định nghĩa kiểu dữ liệu truyền nhận
│   │   ├── api-response.model.ts 
│   │   ├── audit-log.model.ts    
│   │   ├── component.model.ts    
│   │   └── group-category.model.ts
│   ├── constants/                
│   │   └── status.constants.ts   # Định nghĩa màu sắc Badges trạng thái và Action Pills
│   ├── validators/               # Bộ xác thực dữ liệu đầu vào
│   │   ├── category.schema.ts    # Zod Schema validate form danh mục tham số
│   │   └── component.schema.ts   # Zod Schema validate form cấu phần xử lý
│   └── utils/                    
│       └── date.utils.ts         # Tiện ích chuyển đổi và hiển thị ngày tháng
└── features/                     # Các module chức năng độc lập (Feature Modules)
    ├── category/                 # Chức năng: Tham số danh mục theo nhóm
    │   ├── category.routes.ts    # Định tuyến riêng của module category
    │   ├── services/             
    │   │   └── category.service.ts # Dịch vụ API kết nối Backend cho Category
    │   └── components/           
    │       ├── category-list/    # Màn hình hiển thị danh sách, bộ lọc, xem lịch sử thao tác
    │       ├── category-dialog/  # Form nhập liệu (Thêm mới, sửa đổi, sao chép)
    │       └── category-detail/  # Màn hình xem chi tiết thông tin tham số
    └── processing-components/    # Chức năng: Cấu phần xử lý
        ├── processing-components.routes.ts # Định tuyến riêng của module component
        ├── services/             
        │   └── component.service.ts # Dịch vụ API kết nối Backend cho Component
        └── components/           
            ├── component-list/   # Màn hình danh sách cấu phần và duyệt cấu phần
            ├── component-dialog/ # Form nhập liệu cấu phần
            └── component-detail/ # Màn hình xem chi tiết cấu phần
```

### Giải nghĩa vai trò các file Frontend chính:

| File / Thư mục | Vai trò cụ thể trong hệ thống |
| :--- | :--- |
| **`app.config.ts`** | Nơi cấu hình toàn bộ ứng dụng Angular 17. Nó loại bỏ việc khai báo `AppModule` truyền thống, thay vào đó cung cấp các Services hệ thống trực tiếp (như `provideRouter`, `provideHttpClient`...). |
| **`language.service.ts`** | Dịch vụ đa ngôn ngữ. Sử dụng Angular **Signals** (`labels = signal(...)`) giúp giao diện tự động thay đổi từ Tiếng Anh sang Tiếng Việt ngay lập tức khi người dùng click đổi ngôn ngữ mà không cần load lại trang. |
| **`component.schema.ts`** | Khai báo quy tắc kiểm tra dữ liệu bằng thư viện **Zod v4**. Đồng thời chứa 2 hàm chuyển đổi `zodFieldValidator` và `zodFormValidator` để tích hợp các quy tắc này vào Reactive Form của Angular. |
| **`category-list.ts`** | Code TypeScript quản lý màn hình danh sách tham số. Chứa các hàm xử lý phân trang, tìm kiếm động (JPA), sắp xếp cột động, kéo thả sắp xếp lại cột và xử lý bật Dialog lịch sử thao tác phân trang thực tế. |
| **`category-list.html`** | Giao diện HTML của danh sách tham số. Chứa mã giao diện thiết kế theo chuẩn Taiga UI (sử dụng `tui-tabs`, `tui-textfield`, `tui-pagination` thực tế dưới chân bảng dữ liệu chính và dialog lịch sử). |
