# Quy trình Đồng bộ hóa Frontend và Backend (Sync Flow)
1. **Cập nhật Model**: Khi Backend thay đổi cấu trúc dữ liệu DTO, hãy mở ngay file model TypeScript tương ứng ở Frontend (`src/app/shared/models/`) và cập nhật lại cấu trúc thuộc tính cho khớp.
2. **Cập nhật Validator**: Chỉnh sửa Schema Zod tương ứng của form để đảm bảo tính đồng bộ về validation đầu vào (độ dài ký tự tối đa, ký tự đặc biệt).
3. **Kiểm tra đồng bộ**: Chạy lệnh build cả hai phân hệ để đảm bảo không có lỗi biên dịch kiểu dữ liệu (Type compilation error).
