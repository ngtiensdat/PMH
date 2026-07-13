# Quy tắc phát triển Frontend Angular 17+ (Angular Rule)
## 1. Standalone Components
- Bắt buộc khai báo Component, Directive và Pipe dưới dạng **Standalone**. Không dùng `NgModule` để gom Component.
- Lệnh tạo Component chuẩn:
  ```bash
  ng generate component features/my-feature/components/my-new-screen --standalone --skip-tests
  ```
- Khai báo tường minh tất cả các module phụ thuộc trực tiếp tại thuộc tính `imports` của decorator `@Component`.

## 2. Sử dụng Signals cho State Management
- Quản lý trạng thái UI bằng **Signals** để đạt hiệu năng cập nhật Change Detection tối ưu (OnPush).
- Sử dụng `computed` để tính toán trạng thái phái sinh tự động (ví dụ: tính tổng số trang).
- Quy tắc: Không ghi đè (mutate) giá trị signal một cách trực tiếp từ template HTML, hãy gọi qua phương thức xử lý ở component.

## 3. RxJS & HttpClient Boundaries
- Không tiêm `HttpClient` trực tiếp vào Component. Component chỉ tương tác với Service.
- Lệnh tạo Service:
  ```bash
  ng generate service features/my-feature/services/my-service --skip-tests
  ```
- Chuyển đổi từ Observable của HttpClient sang Signal bằng `toSignal()` để hiển thị dữ liệu tinh gọn, hoặc tự động giải phóng bộ nhớ (unsubscribe) bằng `takeUntilDestroyed(this.destroyRef)`.

## 4. Xử lý Form và Kiểm tra dữ liệu (Validation)
- Bắt buộc dùng **Reactive Forms** (sử dụng `FormBuilder` và `FormGroup`).
- Sử dụng thư viện **Zod** để định nghĩa schema validation. Chạy validate bằng hàm trợ giúp tập trung trước khi gửi API:
  ```typescript
  const result = ComponentSchema.safeParse(this.searchForm.value);
  if (!result.success) {
     // Hiển thị lỗi Zod lên form control tương ứng
  }
  ```
- Cấm sử dụng kiểu dữ liệu `any` khi xử lý form. Hãy định nghĩa interface Request/Response rõ ràng.
