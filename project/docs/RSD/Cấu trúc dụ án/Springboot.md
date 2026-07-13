├── src/main/java/com/example/paymenthub/
│   ├── Application.java                  # File chạy chính của Spring Boot
│   │
│   ├── common/                           # Các tiện ích và class cốt lõi dùng chung
│   │   ├── base/                         # Chứa các Base Classes
│   │   │   ├── BaseEntity.java           # (@MappedSuperclass có id, created_at, updated_at)
│   │   │   ├── BaseController.java       # (Định nghĩa chung các hàm API phản hồi HTTP)
│   │   │   ├── BaseService.java          # (Interface CRUD dùng Generics <T, ID>)
│   │   │   └── BaseServiceImpl.java      # (Code thực thi CRUD dùng chung)
│   │   ├── constants/                    # Hằng số (ErrorCodes, SystemConstants, RegexConstants)
│   │   ├── exception/                    # Xử lý lỗi toàn hệ thống
│   │   │   ├── GlobalExceptionHandler.java # (@RestControllerAdvice bắt lỗi tập trung)
│   │   │   └── CustomBusinessException.java# Định nghĩa lỗi nghiệp vụ riêng
│   │   └── utils/                        # Các hàm phụ trợ (JwtUtils, DateUtils, PasswordEncoder)
│   │
│   ├── config/                           # Cấu hình Framework
│   │   ├── SecurityConfig.java           # Cấu hình phân quyền, chặn API, CORS
│   │   ├── OpenApiConfig.java            # Cấu hình tài liệu Swagger API
│   │   └── DatabaseConfig.java           # Cấu hình kết nối nhiều DB (nếu cần)
│   │
│   ├── controller/                       # TẦNG API (Giao tiếp HTTP)
│   │   ├── admin/                        # Nhóm API cho Admin (ví dụ: AdminUserController)
│   │   └── student/                      # Nhóm API cho Sinh viên (ví dụ: StudentProjectController)
│   │
│   ├── dto/                              # DATA TRANSFER OBJECT (Chứa object gửi/nhận)
│   │   ├── request/                      # Dữ liệu Client gửi lên (UserCreateReq, LoginReq) - chứa @Valid
│   │   └── response/                     # Dữ liệu trả về (UserDetailRes, PaginatedRes)
│   │
│   ├── entity/                           # TẦNG MAP VỚI DATABASE (JPA)
│   │   ├── UserEntity.java               # Map với bảng users
│   │   └── ProjectEntity.java            # Map với bảng projects
│   │
│   ├── repository/                       # TẦNG TRUY VẤN DỮ LIỆU
│   │   ├── custom/                       # Nơi chứa interface gọi Stored Procedure / Native SQL phức tạp
│   │   └── UserRepository.java           # Kế thừa JpaRepository cho các câu lệnh đơn giản
│   │
│   └── service/                          # TẦNG NGHIỆP VỤ LÕI
│       ├── impl/                         # Thư mục bắt buộc chứa code thực thi thật
│       │   └── UserServiceImpl.java      # Xử lý logic, tính toán, gọi Repository
│       └── UserService.java              # Chỉ chứa Interface (Định nghĩa hành động)
│
│   ├── unitest/                           # Cài thêm unit test để kiểm thử
│   ├── sonarLint/                           # Cài sonarlint để quản lý chất lượng mã nguồn
├── Dockerfile, .dockerignore, docker compose         # Nếu sử dụng Docker để build
└── resources/
    ├── application.yml                   # Cấu hình port, database credentials
    ├── text/                   # Cấu hình ResourceBundle để lưu text, không hard text vào code
    └── db/migration/                     # Nơi viết các thay đổi DB
        ├── V1__Init_Tables.sql           # Script tạo bảng CSDL