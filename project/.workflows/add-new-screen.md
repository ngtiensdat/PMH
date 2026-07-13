# Quy trình Thêm màn hình mới (Add New Screen Workflow)
1. **Tạo Component**: Sử dụng lệnh Angular CLI để tạo component standalone mới.
2. **Định tuyến Route**: Khai báo tuyến định tuyến mới trong file Router tương ứng của module nghiệp vụ, luôn cấu hình Lazy Loading bằng hàm `loadComponent`:
   ```typescript
   {
     path: 'my-screen',
     loadComponent: () => import('./components/my-screen/my-screen').then(m => m.MyScreenComponent)
   }
   ```
3. **Thiết kế Layout HTML/CSS**: Sử dụng hệ thống lưới (Grid) của Taiga UI để bố trí các trường thông tin cân đối.
4. **Liên kết Logic**: Sử dụng Signal và inject các Service cần thiết để tải dữ liệu lên màn hình.
