# Quy trình Sửa lỗi logic (Fix Bug Workflow)
1. **Đọc Log lỗi**: Thu thập log chi tiết từ console của Backend và cửa sổ Inspect Console của Frontend để xác định lỗi xuất phát từ lớp nào.
2. **Tái hiện lỗi**: Tạo kịch bản nhập liệu giống hệt để tái hiện lỗi cục bộ.
3. **Sửa code tối thiểu (Minimal Patch)**:
   - Sửa đúng đoạn code gây lỗi (ví dụ: thiếu kiểm tra Null, sai logic điều kiện).
   - Tuyệt đối không thay đổi sang thuật toán khác hoặc cấu trúc lại code xung quanh nếu không được yêu cầu.
4. **Xác thực**: Chạy lại kịch bản lỗi để đảm bảo lỗi đã được khắc phục và không gây ảnh hưởng đến các tính năng khác xung quanh.
