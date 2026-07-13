# Quy trình Rà soát mã nguồn trước khi Commit (Pre-commit Review)
1. **Kiểm tra Git Status**: Chạy lệnh `git status` và `git diff` để rà soát toàn bộ các thay đổi trong mã nguồn.
2. **Dọn dẹp code rác**: Xóa bỏ toàn bộ các dòng log thử nghiệm (`console.log`, `System.out.println`), các chú thích nháp không có giá trị lâu dài.
3. **Kiểm tra biên dịch**: Đảm bảo dự án build thành công ở máy cá nhân trước khi thực hiện commit.
4. **Viết thông điệp Commit**: Viết commit message theo chuẩn Conventional Commits mô tả đúng những gì đã thay đổi.
