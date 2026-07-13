# Đặc tả API chi tiết (API Specifications)
Tài liệu định nghĩa các cổng API RESTful phục vụ phân hệ quản lý tham số Backoffice.

Mọi API mặc định trả về định dạng bọc dữ liệu:
```json
{
  "code": "00",
  "message": "Thành công",
  "data": { ... }
}
```

---

## 1. Cấu phần xử lý (Processing Components)

### 1.1 Tìm kiếm & Phân trang cấu phần
- **HTTP Method**: `POST`
- **Endpoint**: `/api/components/search`
- **Request Body (JSON)**:
  ```json
  {
    "componentCode": "VNPAY",
    "componentName": "",
    "status": [3, 4],
    "isActive": [1]
  }
  ```
- **Query Parameters**: `page` (mặc định 0), `size` (mặc định 10), `sort` (ví dụ: `updatedDate,desc`).
- **Response Data**: Đối tượng chứa danh sách phân trang (`content`, `totalElements`, `totalPages`).

### 1.2 Thêm mới cấu phần xử lý
- **HTTP Method**: `POST`
- **Endpoint**: `/api/components`
- **Request Body**:
  ```json
  {
    "componentCode": "MOMO",
    "componentName": "Ví điện tử Momo",
    "messageType": "JSON",
    "connectionMethod": "HTTPS_POST",
    "checkToken": "1",
    "isActive": "1"
  }
  ```
- **Response**: Trả về thông tin bản ghi đã tạo tạm kèm `status = 3` (Chờ duyệt).

---

## 2. Tham số danh mục (Group Category)

### 2.1 Tìm kiếm danh mục theo nhóm
- **HTTP Method**: `POST`
- **Endpoint**: `/api/category/search`
- **Request Body**:
  ```json
  {
    "paramType": "SYSTEM",
    "paramValue": "",
    "paramName": "",
    "status": [],
    "isActive": []
  }
  ```
