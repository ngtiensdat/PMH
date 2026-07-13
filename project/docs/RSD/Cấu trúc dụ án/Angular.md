frontend/src/app/
│── core/                   # Chứa các singleton chỉ khởi tạo 1 lần (Auth guards, interceptors)
│── shared/                 # Chứa các thành phần dùng chung (UI components, directives, pipes)
│── features/               # Các module theo từng nghiệp vụ (VD: auth, dashboard, user-management)
│   └── user/               # Module Quản lý User
│       ├── components/     # Giao diện con (list, detail, form)
│       ├── services/       # Call API riêng cho module user
│       └── user.routes.ts  # Định tuyến riêng của module
│── layout/                 # Các thành phần khung trang web (header, sidebar, footer)
│── pages/                  # Các component toàn trang (nếu không dùng feature module)
│── app.component.ts        # Component gốc
│── app.config.ts           # Cấu hình ứng dụng (Angular 17+), router, HTTP client
└── app.routes.ts           # Định tuyến chính của ứng dụng
