# Tài liệu Tổng quan Dự án Payment Hub (PMH)
## 1. Giới thiệu dự án
Dự án **Payment Hub (PMH)** là hệ thống lõi Backoffice dùng để vận hành, cấu hình cổng thanh toán và quản lý các đối tác liên kết. Hệ thống được thiết kế với cơ chế bảo mật cao, luồng duyệt Maker - Checker (Kiểm soát kép) cho toàn bộ thay đổi cấu hình, nhằm tránh rủi ro thao tác sai sót gây ảnh hưởng đến hệ thống thanh toán thời gian thực.

## 2. Các Phân hệ chính
- **Tham số Cấu phần xử lý (Processing Components)**: Cấu hình chuẩn kết nối (Connection Method), chuẩn tin điện (ISO8583, JSON, XML,...), kiểm tra chữ ký số/token bảo mật.
- **Tham số Danh mục theo nhóm (Group Category)**: Cấu hình các tham số phân hệ chi tiết tương ứng với từng cấu phần xử lý.
- **Lịch sử phê duyệt & Đối chiếu (Audit Log & Comparison)**: Theo dõi vết thay đổi dữ liệu (Audit Trail) và trực quan hóa điểm khác biệt (diff) giữa dữ liệu cũ và dữ liệu mới chờ duyệt.

## 3. Tech Stack Standard
- **Frontend Core**: Angular v17.0+ (Standalone architecture, Signals reactive state).
- **Frontend UI Framework**: Taiga UI (V4+) - Hệ thống Design System hướng ngân hàng, nghiêm ngặt về accessibility.
- **Backend Core**: Spring Boot v3.2+, Java 17 LTS.
- **Database**: Oracle Database 19c.
- **ORM & Connectivity**: Spring Data JPA, Hibernate Core, Hikari Connection Pool.
