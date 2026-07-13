# Sơ đồ Quan hệ Cơ sở dữ liệu (ERD Schema)
Dưới đây là sơ đồ thực thể mối quan hệ cơ sở dữ liệu của các bảng cấu hình tham số trong Oracle Database.

Sơ đồ sử dụng cú pháp **Mermaid Markdown**. Bạn có thể sử dụng các công cụ đọc Mermaid để xem trực quan sơ đồ quan hệ.

```mermaid
erDiagram
    PMH_COMPONENTS {
        NUMBER ID PK "Khóa chính tự tăng"
        VARCHAR2 COMPONENT_CODE UK "Mã cấu phần xử lý (Duy nhất)"
        VARCHAR2 COMPONENT_NAME "Tên hiển thị cấu phần"
        VARCHAR2 CONNECTION_METHOD "Phương thức kết nối (HTTP, TCP,...)"
        VARCHAR2 MESSAGE_TYPE "Chuẩn định dạng tin điện"
        VARCHAR2 CHECK_TOKEN "Kiểm tra Token/ký số (0/1)"
        NUMBER STATUS "Trạng thái phê duyệt (3:Chờ duyệt, 4:Đã duyệt,...)"
        NUMBER IS_ACTIVE "Tình trạng hoạt động (0/1)"
        VARCHAR2 NEW_DATA "Chuỗi JSON chứa dữ liệu thay đổi chờ duyệt"
        VARCHAR2 CREATED_BY "Người tạo"
        DATE CREATED_DATE "Ngày tạo"
        VARCHAR2 UPDATED_BY "Người cập nhật gần nhất"
        DATE UPDATED_DATE "Ngày cập nhật gần nhất"
    }
    
    PMH_GROUP_CATEGORIES {
        NUMBER ID PK "Khóa chính tự tăng"
        VARCHAR2 PARAM_TYPE "Loại nhóm danh mục"
        VARCHAR2 PARAM_VALUE "Giá trị thành phần tham số"
        VARCHAR2 PARAM_NAME "Tên mô tả chi tiết của tham số"
        VARCHAR2 COMPONENT_CODE FK "Mã cấu phần xử lý liên kết"
        VARCHAR2 DESCRIPTION "Mô tả chi tiết"
        DATE EFFECTIVE_DATE "Ngày hiệu lực tham số"
        DATE END_EFFECTIVE_DATE "Ngày hết hiệu lực tham số"
        NUMBER STATUS "Trạng thái duyệt"
        NUMBER IS_ACTIVE "Tình trạng hoạt động"
        VARCHAR2 NEW_DATA "Chuỗi JSON chứa dữ liệu thay đổi chờ duyệt"
        VARCHAR2 CREATED_BY "Người tạo"
        DATE CREATED_DATE "Ngày tạo"
        VARCHAR2 UPDATED_BY "Người cập nhật"
        DATE UPDATED_DATE "Ngày cập nhật"
    }
    
    PMH_AUDIT_LOGS {
        NUMBER ID PK "Khóa chính tự tăng"
        VARCHAR2 TABLE_NAME "Tên bảng tác động (PMH_COMPONENTS,...)"
        NUMBER RECORD_ID "ID của bản ghi bị tác động"
        VARCHAR2 ACTION "Hành động (THEM/SUA/DUYET/TU_CHOI)"
        VARCHAR2 PERFORMED_BY "Người thực hiện thao tác"
        DATE PERFORMED_DATE "Thời điểm thực hiện"
        CLOB OLD_DATA "Chuỗi dữ liệu cũ (JSON)"
        CLOB NEW_DATA "Chuỗi dữ liệu mới (JSON)"
        VARCHAR2 REASON "Lý do từ chối (nếu có)"
    }
    
    PMH_COMPONENTS ||--o{ PMH_GROUP_CATEGORIES : "liên kết qua COMPONENT_CODE"
```
