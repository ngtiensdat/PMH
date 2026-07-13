# Quy trình Sửa lỗi giao diện (Fix UI Workflow)
1. **Xác định lỗi**: Sử dụng Developer Tools của Chrome (F12) để xác định chính xác phần tử HTML/CSS bị lỗi hiển thị.
2. **Kiểm tra CSS Scope**: Kiểm tra xem file CSS của component bị lỗi có đang sử dụng thuộc tính đè CSS không đúng cách làm ảnh hưởng đến các component khác không.
3. **Sửa đổi an toàn**:
   - Không sửa đổi trực tiếp css gốc của Taiga UI.
   - Luôn sử dụng biến CSS variables hệ thống hoặc định nghĩa class riêng biệt.
4. **Kiểm tra Responsive**: Kiểm tra lại giao diện hiển thị trên các độ phân giải màn hình thông dụng (Desktop, Tablet).
