# FILE HỌC CODE THỰC CHIẾN - PHẦN 2 (LEARN02)

Chúc mừng bạn đã xuất sắc vượt qua phần 1 với kết quả xuất sắc!
Dưới đây là phần code của lớp Controller và DTO ở Spring Boot Backend, nơi tiếp nhận dữ liệu được gửi lên từ Frontend.

---

## 💻 Đoạn code 1: Controller tiếp nhận API

Đoạn code này nằm trong file [ComponentController.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/controller/ComponentController.java).

```java
    @PostMapping
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> create(
            @Valid @RequestBody ComponentDTO dto,
            @RequestHeader(value = "X-Username", defaultValue = "SYSTEM") String username
    ) {
        ProcessingComponent created = service.create(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ComponentResponseDTO.fromEntity(created), "Tạo mới cấu phần thành công"));
    }
```

---

## 💻 Đoạn code 2: DTO chứa các ràng buộc kiểm lỗi (Validation)

Đoạn code này nằm trong file [ComponentDTO.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/dto/request/ComponentDTO.java).

```java
public class ComponentDTO {

    @NotBlank(message = "Mã cấu phần không được để trống")
    @Size(max = 20, message = "Mã cấu phần tối đa 20 ký tự")
    private String componentCode;

    @NotBlank(message = "Tên cấu phần không được để trống")
    @Size(max = 150, message = "Tên cấu phần tối đa 150 ký tự")
    private String componentName;

    @Pattern(regexp = "^[YN]$", message = "Kiểm tra Token chỉ nhận giá trị Y hoặc N")
    private String checkToken;

    // ... các thuộc tính khác
}
```

---

## ❓ Câu hỏi dành cho bạn:

### 1. Cơ chế bóc tách tham số (Binding & Header):
*   Annotation `@RequestBody` có vai trò gì? Dữ liệu được gửi từ Angular Service (`this.http.post(url, dto)`) sẽ chui vào biến nào trong hàm `create`?
*   Biến `username` lấy giá trị từ đâu? Nếu phía Frontend không truyền thông tin này lên thì giá trị mặc định của `username` sẽ là gì?

### 2. Kiểm lỗi dữ liệu (Validation):
*   Cờ hiệu `@Valid` đặt trước `@RequestBody ComponentDTO dto` có tác dụng gì? Nếu thiếu cờ hiệu này, Spring Boot có kiểm tra các ràng buộc như `@NotBlank` hay `@Size` trong class `ComponentDTO` không?
*   Hãy so sánh điều kiện `@Pattern(regexp = "^[YN]$", ...)` của trường `checkToken` ở Backend với cách Angular xử lý checkbox ở Frontend (đã làm ở phần 1). Tại sao lại có sự khác biệt về kiểu dữ liệu (Boolean ở FE vs String `Y`/`N` ở BE)?

---

## ✍️ Phần trả lời của bạn:
1.
 annotation 'RequestBody' có vai trò đánh dấu cho việc hàm này sẽ xử lý các yêu cầu được gửi từ fe, dữ liệu được gửi từ fe được chui vào service.create(dto, username) sau đó hiển thị ra thông báo
biến username sẽ lấy giứa trị từ X-Username và giá trị mặc định là SYSTEM
2. cờ Valid đặt trước @RequestBody ComponentDTO dto có tác dụng là ép spring kiểm tra các giá trị có trong các annotation của class ComponentDTO 
