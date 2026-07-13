# Quy trình Rà soát trước khi Triển khai (Pre-deploy Review)
1. **Kiểm tra file cấu hình**: Đảm bảo tất cả các file cấu hình kết nối Database (Oracle), thông tin tài khoản, khóa bảo mật đã được chuyển sang chế độ môi trường Production.
2. **Chạy Build thành phẩm**: Chạy build nén production tối đa cho cả hai phân hệ.
3. **Kiểm tra kịch bản Rollback**: Đảm bảo có sẵn bản backup cơ sở dữ liệu Oracle và mã nguồn phiên bản cũ đang chạy ổn định để có thể quay xe (rollback) lập tức nếu quá trình deploy xảy ra sự cố lớn.
