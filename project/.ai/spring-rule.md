# Quy tắc phát triển Backend Spring Boot (Spring Rule)
## 1. Java 17 Coding Standards
- Sử dụng kiểu dữ liệu **Record** cho các DTO (Data Transfer Objects) chỉ chứa dữ liệu tĩnh để code ngắn gọn, bất biến (Immutable).
- Sử dụng cơ chế Dependency Injection thông qua **Constructor Injection** thay vì ghi `@Autowired` trực tiếp lên thuộc tính. Khuyên dùng `@RequiredArgsConstructor` của Lombok.

## 2. Quản lý Transactions chuyên sâu
- Đối với các thao tác cập nhật đơn lẻ (Single Entity): Dùng `@Transactional(rollbackFor = Exception.class)`.
- Đối với luồng phê duyệt hàng loạt hoặc chạy xử lý lô (Batch Processing):
  * **CẤM** đặt `@Transactional` ở mức hàm cha bao bọc vòng lặp duyệt phần tử. Vì nếu một phần tử bị lỗi DB, toàn bộ các phần tử hợp lệ trước đó cũng sẽ bị rollback (lỗi dây chuyền).
  * **GIẢI PHÁP**: Dùng `TransactionTemplate` chạy thủ công cho từng vòng lặp:
    ```java
    transactionTemplate.execute(status -> {
        // Thực hiện xử lý lưu/cập nhật cho 1 phần tử cụ thể
        return repository.save(entity);
    });
    ```

## 3. Gọi Stored Procedures trong Oracle DB
- Khai báo gọi stored procedures thông qua JPA `StoredProcedureQuery` để quản lý tham số đầu vào (IN) và đầu ra (OUT) an toàn, tránh lỗi rò rỉ kết nối (connection leak):
  ```java
  StoredProcedureQuery query = entityManager.createStoredProcedureQuery("DEMO.PKG_PMH.PRC_APPROVE");
  query.registerStoredProcedureParameter("p_id", Long.class, ParameterMode.IN);
  query.registerStoredProcedureParameter("p_user", String.class, ParameterMode.IN);
  query.registerStoredProcedureParameter("p_out_code", String.class, ParameterMode.OUT);
  query.setParameter("p_id", id);
  query.setParameter("p_user", username);
  query.execute();
  String outCode = (String) query.getOutputParameterValue("p_out_code");
  ```

## 4. Biên dịch và quản lý thư viện Maven
- Lệnh biên dịch chuẩn, bỏ qua chạy unit tests khi build sản phẩm:
  ```bash
  mvn clean package -DskipTests -Pprod
  ```
- Thường xuyên chạy lệnh phân tích dependency dư thừa hoặc thiếu sót:
  ```bash
  mvn dependency:analyze
  ```
