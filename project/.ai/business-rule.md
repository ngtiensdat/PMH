# Quy tắc Luồng Nghiệp vụ Duyệt Tham số (Business Rule)
## 1. Quy trình Kiểm soát kép (Maker - Checker Workflow)
Tất cả các thay đổi về cấu hình tham số trong hệ thống Payment Hub đều phải trải qua luồng kiểm duyệt 2 bước:
1. **Maker (Người tạo yêu cầu)**:
   - Thực hiện Thêm mới, Sửa hoặc Hủy hoạt động của một cấu phần/danh mục.
   - Bản ghi chính trên bảng không được thay đổi ngay lập tức. Hệ thống sẽ lưu giữ bản sao dữ liệu thay đổi dưới dạng chuỗi JSON thô trong trường `new_data` (hoặc cột CLOB tương ứng) của bản ghi đó, đồng thời cập nhật trường trạng thái duyệt `status = 3` (Chờ duyệt).
2. **Checker (Người duyệt yêu cầu)**:
   - Checker đăng nhập hệ thống, kiểm tra danh sách các yêu cầu chờ duyệt.
   - Checker **không được phép** tự duyệt các yêu cầu do chính mình tạo ra (quy tắc an toàn thông tin).
   - **Phê duyệt (Approve)**: Hệ thống lấy chuỗi JSON trong `new_data`, giải mã và cập nhật đè vào các trường dữ liệu chính của bản ghi, xóa sạch trường `new_data` và chuyển trạng thái duyệt `status = 4` (Đã phê duyệt).
   - **Từ chối (Reject)**: Yêu cầu cập nhật lý do từ chối. Giữ nguyên dữ liệu chính của bản ghi, chuyển trạng thái duyệt `status = 5` (Từ chối).

## 2. Các trạng thái duyệt mặc định (Status Codes)
- `1`: Tạo mới (Chưa gửi duyệt)
- `3`: Chờ duyệt (Pending Approval)
- `4`: Đã phê duyệt (Approved)
- `5`: Từ chối (Rejected)
- `7`: Hủy duyệt (Canceled)

## 3. Tình trạng hoạt động (Active Status)
- `1`: Hoạt động (Active)
- `0`: Không hoạt động (Inactive)

## 4. Log lịch sử hệ thống (System Audit Trail)
- Mọi thao tác đổi trạng thái phê duyệt phải ghi nhận lại vết lịch sử vào bảng `PMH_AUDIT_LOGS`.
- Lưu giữ thông tin: Người thực hiện (User), Thời gian (Timestamp), Loại thao tác (Thêm/Sửa/Duyệt/Từ chối), Dữ liệu cũ (Old Data), Dữ liệu mới (New Data).
