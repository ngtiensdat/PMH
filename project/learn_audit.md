# 📚 AUDIT KIẾN THỨC DỰ ÁN PAYMENT HUB
> **Tổng hợp toàn bộ kiến thức từ Learn 01 → Learn 11**  
> Gồm: Định nghĩa · Lý thuyết · Cách dùng · Cấu trúc · Ví dụ thực chiến từ dự án  
> Sắp xếp từ nền tảng → nâng cao → thực chiến

---

## MỤC LỤC

| # | Chủ đề |
|---|--------|
| I | [Kiến trúc tổng thể hệ thống Payment Hub](#i-kiến-trúc-tổng-thể-hệ-thống-payment-hub) |
| II | [Java Core: OOP, Collections, Stream, Time](#ii-java-core) |
| III | [Spring Boot: Annotation, Bean, DI, IoC](#iii-spring-boot-annotation-bean-di-ioc) |
| IV | [JPA / Hibernate: Entity, Transaction, Specification](#iv-jpa--hibernate) |
| V | [Bảo mật: JWT, Spring Security, Maker-Checker](#v-bảo-mật) |
| VI | [Nghiệp vụ Ngân hàng: Luồng trạng thái, Phê duyệt](#vi-nghiệp-vụ-ngân-hàng) |
| VII | [Angular Frontend: Forms, Lifecycle, Taiga UI](#vii-angular-frontend) |
| VIII | [Hiệu năng: Batch, Scheduler, DOM Optimization](#viii-hiệu-năng) |
| IX | [Lộ trình Production & Technical Debts](#ix-lộ-trình-production--technical-debts) |

---

## I. KIẾN TRÚC TỔNG THỂ HỆ THỐNG PAYMENT HUB

### 1. Payment Hub là gì?
- **Định nghĩa:** Hệ thống quản lý cấu hình & tham số thanh toán cho ngân hàng (Payment Configuration Hub).
- **Mục đích:** Cho phép nhân viên ngân hàng cấu hình các tham số điều khiển hệ thống thanh toán (nhóm danh mục, cấu phần xử lý) theo đúng quy trình kiểm soát 2 người (Maker - Checker).
- **Stack công nghệ:**
  - **Backend:** Java 17+, Spring Boot 3.x, Oracle DB, JPA/Hibernate, MapStruct, Lombok, Jackson
  - **Frontend:** Angular 18, Taiga UI, RxJS, Angular Signals, Zod Validator

### 2. Sơ đồ kiến trúc 4 tầng

```
┌────────────────────────────────────────────────────────────────┐
│  TẦNG TRÌNH DUYỆT (Angular 18 SPA)                            │
│  • Angular Signals (State Management)                          │
│  • Zod Schema Validation (Form kiểm tra dữ liệu)              │
│  • Taiga UI (Thư viện giao diện ngân hàng cao cấp)            │
└──────────────────────────┬─────────────────────────────────────┘
          HTTP Request + Bearer JWT Token
┌──────────────────────────▼─────────────────────────────────────┐
│  TẦNG BẢO MẬT (Spring Security)                               │
│  • JwtAuthenticationFilter (Xác thực Token mọi request)        │
│  • SecurityContextHolder (Lưu thông tin người dùng đang login) │
│  • SecurityConfig (Phân quyền theo Role: MAKER / CHECKER)      │
└──────────────────────────┬─────────────────────────────────────┘
┌──────────────────────────▼─────────────────────────────────────┐
│  TẦNG NGHIỆP VỤ (Spring Service Layer)                        │
│  • GroupCategoryServiceImpl / ComponentServiceImpl             │
│  • AuditLogServiceImpl (Ghi lịch sử mọi thao tác)            │
│  • isDtoDifferentFromEntity (Chặn gửi duyệt không đổi)        │
└──────────────────────────┬─────────────────────────────────────┘
┌──────────────────────────▼─────────────────────────────────────┐
│  TẦNG CƠ SỞ DỮ LIỆU (Oracle DB)                              │
│  • Spring Data JPA Repositories (CRUD, tìm kiếm phân trang)   │
│  • Stored Procedures: PROC_APPROVE / PROC_REJECT               │
│  • Bảng: PMH_GROUP_CATEGORY, PMH_COMPONENTS, PMH_AUDIT_LOGS   │
└────────────────────────────────────────────────────────────────┘
```

### 3. Luồng đi của 1 HTTP Request (Mermaid)

```
Client (Angular)
  → JwtAuthenticationFilter (Xác thực token)
  → Controller (Nhận DTO từ @RequestBody)
  → Service (Xử lý nghiệp vụ, gọi Repository)
  → Repository/Stored Procedure → Oracle DB
  → Service trả Entity → Controller chuyển thành DTO
  → Client nhận JSON Response
```

**Khi có lỗi:** Exception tự động "bắn ngược lên" các tầng → `GlobalExceptionHandler` bắt → trả về JSON lỗi chuẩn (không lộ StackTrace thô ra ngoài).

---

## II. JAVA CORE

### 1. OOP - 4 Tính chất nền tảng

#### A. Đóng gói (Encapsulation)
- **Định nghĩa:** Che giấu chi tiết cài đặt, chỉ lộ ra các phương thức công khai.
- **Cách dùng:** Khai báo biến `private`, truy cập qua `public` Getter/Setter.
- **Trong dự án:** Toàn bộ Entity/DTO dùng `private` field + `@Getter`/`@Setter` của Lombok.

#### B. Kế thừa (Inheritance)
- **Định nghĩa:** Lớp con thừa hưởng tất cả thuộc tính và phương thức từ lớp cha.
- **Từ khóa:** `extends`
- **Trong dự án:** `GroupCategory extends BaseEntity`, `ProcessingComponent extends BaseEntity` → tự động có sẵn 10 thuộc tính lõi: `status`, `isActive`, `isDisplay`, `newData`, `effectiveDate`, `endEffectiveDate`, `createdBy`, `createdDate`, `updatedBy`, `updatedDate`.

#### C. Đa hình (Polymorphism)
- **Định nghĩa:** Cùng một hành động nhưng được thực hiện theo nhiều cách tùy đối tượng.
- **Ghi đè (Overriding):** Lớp con viết lại hàm của lớp cha (Runtime).
- **Nạp chồng (Overloading):** Nhiều hàm cùng tên khác tham số trong 1 class (Compile-time).
- **Trong dự án:** Interface `GroupCategoryService` định nghĩa hợp đồng → `GroupCategoryServiceImpl` triển khai (Override) cụ thể.

#### D. Trừu tượng (Abstraction)
- **Định nghĩa:** Chỉ định nghĩa "làm cái gì", không quan tâm "làm thế nào".
- **Cách dùng:** `interface` hoặc `abstract class`
- **Trong dự án:** `interface GroupCategoryService` chứa các chữ ký hàm (`create`, `update`, `delete`, `batchApprove`...), `GroupCategoryServiceImpl` implements và cài đặt chi tiết.

---

### 2. Constructor (Hàm khởi tạo)

| Đặc điểm | Mô tả |
|----------|-------|
| Tên | Trùng với tên Class (kể cả chữ hoa/thường) |
| Kiểu trả về | Không có (kể cả `void`) |
| Khi gọi | Tự động chạy 1 lần khi `new ClassName()` |
| `this(...)` | Gọi constructor khác của cùng class — phải ở dòng đầu tiên |
| `super(...)` | Gọi constructor của class cha — phải ở dòng đầu tiên của class con |

**Best Practice — Constructor Injection:**
```java
// ✅ CHUẨN: Dùng Constructor Injection (dự án Payment Hub áp dụng)
@RequiredArgsConstructor  // Lombok tự tạo constructor từ các biến final
public class GroupCategoryServiceImpl {
    private final GroupCategoryRepository repository;    // final = bất biến
    private final ObjectMapper objectMapper;             // Dễ Mock khi Unit Test
}
```

---

### 3. Kiểu nguyên thủy (Primitive) vs Wrapper

| Tiêu chí | Primitive (`int`, `long`, `boolean`) | Wrapper (`Integer`, `Long`, `Boolean`) |
|----------|--------------------------------------|----------------------------------------|
| Vùng nhớ | Stack — rất nhanh | Stack (con trỏ) + Heap (dữ liệu thật) |
| Giá trị null | **Không thể** (`0` hoặc `false` mặc định) | **Có thể** (`null`) |
| Dùng Collection | ❌ `List<int>` → lỗi | ✅ `List<Integer>` |

**Tại sao Entity và DTO BẮT BUỘC dùng Wrapper?**
1. DB có cột chưa nhập → giá trị `NULL`. Nếu dùng `int`, Java ép thành `0` → sai nghiệp vụ (`STATUS = null` bị biến thành `STATUS = 0`).
2. Tìm kiếm động: `Integer status = null` → không thêm điều kiện lọc. `int status = 0` → lọc sai sang status = 0.

---

### 4. Java Stream API & Lambda Expression

**Định nghĩa:** Từ Java 8, Stream API cho phép xử lý tập hợp dữ liệu theo phong cách lập trình hàm (Functional Programming): ngắn gọn, tường minh.

**Cấu trúc pipeline:**
```
Nguồn dữ liệu (List) → .stream() → [Thao tác trung gian] → [Thao tác đầu cuối]
```

**Bảng thao tác Stream:**

| Thao tác | Loại | Ý nghĩa | Ví dụ trong Payment Hub |
|----------|------|---------|------------------------|
| `.filter(Predicate)` | Trung gian | Lọc phần tử thỏa điều kiện | `results.stream().filter(r -> Boolean.TRUE.equals(r.get("success")))` |
| `.map(Function)` | Trung gian | Biến đổi từng phần tử (Entity → DTO) | `categories.stream().map(GroupCategoryResponseDTO::fromEntity)` |
| `.collect(Collectors.toList())` | Đầu cuối | Thu thập kết quả thành `List` | `.collect(Collectors.toList())` |
| `.count()` | Đầu cuối | Đếm số phần tử thỏa mãn | `.filter(...).count()` |
| `.findFirst()` | Đầu cuối | Lấy phần tử đầu tiên (trả `Optional`) | `list.stream().filter(...).findFirst()` |

**Lambda vs Method Reference:**
```java
// Lambda:
categories.stream().map(entity -> GroupCategoryResponseDTO.fromEntity(entity));

// Method Reference (:: — ngắn gọn hơn):
categories.stream().map(GroupCategoryResponseDTO::fromEntity);
```

---

### 5. Optional<T> — Diệt tận gốc NullPointerException

**Định nghĩa:** "Hộp chứa" — bên trong có thể có dữ liệu hoặc rỗng. Buộc lập trình viên xử lý trường hợp null một cách tường minh.

```java
// ❌ Cách cũ — dễ quên check null:
GroupCategory entity = repository.findByIdOld(id);
if (entity == null) throw new ResourceNotFoundException("...");

// ✅ Cách chuẩn trong Payment Hub:
GroupCategory entity = repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tham số ID: " + id));
```

**Bảng phương thức Optional:**

| Phương thức | Ý nghĩa |
|-------------|---------|
| `.orElseThrow(Supplier)` | Có → trả về; Rỗng → ném Exception (chuẩn 404) |
| `.orElse(defaultValue)` | Rỗng → gán giá trị mặc định |
| `.ifPresent(Consumer)` | Chỉ chạy đoạn code nếu có dữ liệu |
| `.map(Function)` | Biến đổi dữ liệu bên trong nếu tồn tại |

---

### 6. Java Collections Framework

```
Collection
├── List (Có thứ tự, cho phép trùng lặp)
│   ├── ArrayList: Mảng động, truy xuất index O(1) — dùng nhiều nhất
│   └── LinkedList: Thêm/xóa 2 đầu O(1)
└── Set (Không trùng lặp)
    ├── HashSet: Bảng băm, tìm kiếm O(1)
    └── TreeSet: Tự sắp xếp theo thứ tự

Map (Cặp Key → Value)
├── HashMap: Bảng băm O(1)
└── ConcurrentHashMap: Thread-safe cho đa luồng
```

**Quy tắc Vàng `equals()` & `hashCode()`:**
- Khi đưa đối tượng vào `HashSet`/`HashMap`, Java gọi `hashCode()` → tìm bucket → gọi `equals()` để xác nhận chính xác.
- **QUY TẮC:** Nếu `obj1.equals(obj2) == true` thì **BẮT BUỘC** `obj1.hashCode() == obj2.hashCode()`.
- Nếu chỉ Override một trong hai → `HashSet` lưu nhầm thành 2 phần tử khác nhau dù dữ liệu giống hệt.

---

### 7. Xử lý Ngày Giờ Chuẩn Java 8+ (`java.time.*`)

**Tại sao bỏ `java.util.Date`?**
1. Không Thread-Safe (có thể bị sửa đổi từ nhiều luồng).
2. Tháng chạy từ 0–11 (tháng 1 là 0) — gây nhầm lẫn.
3. Không phân biệt rõ Date / Time / TimeZone.

**Các lớp hiện đại:**

| Lớp | Ý nghĩa | Dùng trong dự án |
|-----|---------|-----------------|
| `LocalDate` | Chỉ Ngày (yyyy-MM-dd) | Ngày sinh, ngày lễ ngân hàng |
| `LocalTime` | Chỉ Giờ (HH:mm:ss) | Giờ mở/đóng cửa giao dịch |
| `LocalDateTime` | Ngày + Giờ (không múi giờ) | `EFFECTIVE_DATE`, `END_EFFECTIVE_DATE` |
| `Instant` | Mốc thời gian tuyệt đối UTC | Timestamp trong Audit Log |
| `ZonedDateTime` | Ngày + Giờ + Múi giờ | Giao dịch thanh toán quốc tế SWIFT |

---

### 8. Design Pattern: Builder Pattern & Lombok

**Vấn đề "Constructor xếp tầng" (Telescoping Constructor):**
```java
// ❌ Dễ nhầm thứ tự tham số khi có nhiều trường:
ApiResponse res = new ApiResponse(true, "Thành công", null, LocalDateTime.now());
```

**Giải pháp với `@Builder` của Lombok:**
```java
// Khai báo class:
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
}

// Sử dụng — rõ ràng, không nhầm lẫn:
ApiResponse.builder()
    .success(true)
    .message("Tạo mới thành công")
    .data(dtoResult)
    .timestamp(LocalDateTime.now())
    .build();
```

---

### 9. Vòng đời Annotation (Retention Policy) & Reflection

| Retention | Tồn tại đến | Ví dụ |
|-----------|-------------|-------|
| `SOURCE` | Chỉ trong mã nguồn `.java` | `@Override`, Lombok (`@Getter`, `@Setter`) |
| `CLASS` | Được lưu vào `.class` nhưng JVM không nạp lên | Mặc định |
| `RUNTIME` | JVM nạp lên bộ nhớ khi chạy | `@Service`, `@Repository`, `@Transactional` |

Spring Boot dùng **Java Reflection** để quét RUNTIME annotations lúc khởi động → tự động tạo Bean.

---

## III. SPRING BOOT: ANNOTATION, BEAN, DI, IOC

### 1. Bảng Annotation theo nhóm

#### A. Spring Core & Web (Routing)

| Annotation | Ý nghĩa |
|-----------|---------|
| `@SpringBootApplication` | Khai báo lớp chính, tích hợp `@ComponentScan` + `@EnableAutoConfiguration` |
| `@RestController` | Controller trả về JSON tự động (= `@Controller` + `@ResponseBody`) |
| `@Service` | Tầng nghiệp vụ |
| `@Repository` | Tầng truy cập DB, dịch lỗi JDBC thành Spring Exception |
| `@Component` | Bean thông thường |
| `@Autowired` | Tiêm Dependency tự động |
| `@RequestMapping("/path")` | Khai báo tiền tố URL cho cả Controller |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | HTTP Method cụ thể cho từng endpoint |
| `@PathVariable` | Đọc tham số từ URL động: `/api/{id}` |
| `@RequestParam` | Đọc query string: `?paramName=MOMO` |
| `@RequestBody` | Đọc JSON Payload từ Client, chuyển thành DTO |
| `@RequestHeader` | Đọc HTTP Header — ví dụ: `X-Username` |

**So sánh `@Controller` vs `@RestController`:**

| | `@Controller` | `@RestController` |
|--|--------------|------------------|
| Bản chất | MVC truyền thống (Server-Side Rendering) | `@Controller` + `@ResponseBody` |
| Trả về | Tên View template (`.html`, `.jsp`) | Dữ liệu JSON/XML tự động |
| Muốn trả JSON | Phải gắn thêm `@ResponseBody` từng hàm | Mặc định 100% trả JSON |

#### B. JPA / Hibernate Annotations

| Annotation | Ý nghĩa |
|-----------|---------|
| `@Entity` | Java Class ánh xạ tới Table trong DB |
| `@Table(name = "...")` | Chỉ định tên bảng |
| `@Id` | Khóa chính (Primary Key) |
| `@GeneratedValue` | Cơ chế tự tạo khóa chính |
| `@Column` | Cấu hình chi tiết cột (nullable, length, updatable) |
| `@Version` | Khóa lạc quan (Optimistic Locking) — chống xung đột đồng thời |
| `@PrePersist` / `@PreUpdate` | Trigger tự động gán ngày tạo/sửa trước khi lưu DB |
| `@MappedSuperclass` | Lớp cha chứa thuộc tính dùng chung (không tạo bảng riêng) |
| `@PersistenceContext` | Tiêm `EntityManager` |
| `@Transactional` | Đánh dấu ranh giới giao dịch, tự động Commit/Rollback |
| `@Query` | SQL hoặc JPQL tùy chỉnh trên Repository method |
| `@Procedure` | Gọi Stored Procedure từ Oracle |

#### C. Lombok Annotations

| Annotation | Sinh ra |
|-----------|---------|
| `@Getter` / `@Setter` | Hàm getter/setter cho tất cả trường |
| `@NoArgsConstructor` | Constructor không tham số |
| `@AllArgsConstructor` | Constructor đầy đủ tham số |
| `@RequiredArgsConstructor` | Constructor cho các trường `final` / `@NonNull` |
| `@Builder` | Builder Pattern (Fluent API) |
| `@Slf4j` | Tự tạo `log` object của SLF4J |

#### D. Validation Annotations (Kiểm định DTO)

| Annotation | Ý nghĩa |
|-----------|---------|
| `@Valid` | Kích hoạt validation toàn bộ DTO khi nhận từ Client |
| `@NotNull` | Không được null |
| `@NotBlank` | Không được null, rỗng, hoặc chỉ khoảng trắng |
| `@Size(min, max)` | Giới hạn độ dài chuỗi hoặc kích thước mảng |
| `@Pattern(regexp)` | Kiểm tra theo Regular Expression |

**Ví dụ trong dự án:**
```java
public class ComponentDTO {
    @NotBlank(message = "Mã cấu phần không được để trống")
    @Size(max = 20, message = "Mã cấu phần tối đa 20 ký tự")
    private String componentCode;

    @Pattern(regexp = "^[YN]$", message = "checkToken chỉ nhận Y hoặc N")
    private String checkToken;
}
```

#### E. Exception Handling

| Annotation | Ý nghĩa |
|-----------|---------|
| `@RestControllerAdvice` | Xử lý lỗi tập trung toàn cục cho mọi RestController |
| `@ExceptionHandler(Ex.class)` | Bắt exception cụ thể và chuyển thành JSON lỗi chuẩn |

---

### 2. Dependency Injection (DI) & Inversion of Control (IoC)

**IoC:** Spring Container tự quản lý việc tạo và kết nối các Bean thay vì lập trình viên tự `new`.

**3 cách tiêm phụ thuộc:**

```java
// ❌ Cách 1 - Field Injection: Khó test, không khai báo final được
@Autowired private GroupCategoryRepository repository;

// ❌ Cách 2 - Setter Injection: Đối tượng có thể bị thay đổi sau khởi tạo
@Autowired public void setRepository(GroupCategoryRepository r) { this.repository = r; }

// ✅ Cách 3 - Constructor Injection (CHUẨN - dự án đang dùng):
@RequiredArgsConstructor  // Lombok tự sinh
public class GroupCategoryServiceImpl {
    private final GroupCategoryRepository repository;  // Bất biến, dễ mock
}
```

### 3. Scope (Phạm vi tồn tại) của Spring Bean

| Scope | Số lượng instance | Dùng cho |
|-------|------------------|----------|
| **Singleton** (Mặc định) | 1 duy nhất / ứng dụng | 99% Service, Repository, Controller |
| **Prototype** | Tạo mới mỗi lần tiêm | Đối tượng stateful (chứa trạng thái riêng) |
| **Request** | Mới theo HTTP Request | Lưu dữ liệu trong 1 request |
| **Session** | Mới theo phiên đăng nhập | Lưu giỏ hàng, session người dùng |

---

### 4. Chuẩn hóa HTTP Status Code

| Code | Tên | Khi nào dùng |
|------|-----|-------------|
| `200 OK` | Thành công | Lấy/cập nhật/xóa thành công |
| `201 Created` | Tạo mới thành công | Thêm mới bản ghi |
| `400 Bad Request` | Dữ liệu sai | Vi phạm validation |
| `401 Unauthorized` | Chưa xác thực | Thiếu/hết hạn token |
| `403 Forbidden` | Bị từ chối quyền | Maker tự duyệt bản ghi của mình |
| `404 Not Found` | Không tìm thấy | Bản ghi không tồn tại |
| `500 Internal Server Error` | Lỗi hệ thống | Lỗi chưa được xử lý |

---

### 5. ObjectMapper (Jackson) — Xử lý JSON

```java
// Serialization: Java Object → JSON String (lưu vào cột NEW_DATA)
String jsonString = objectMapper.writeValueAsString(dto);

// Deserialization: JSON String → Java Object (đọc từ NEW_DATA)
GroupCategoryDTO dto = objectMapper.readValue(entity.getNewData(), GroupCategoryDTO.class);
```

**Cấu hình chuẩn trong dự án (`JacksonConfig.java`):**
- Đăng ký `JavaTimeModule` → xử lý `LocalDateTime` / `Instant` đúng.
- `FAIL_ON_UNKNOWN_PROPERTIES = false` → không lỗi khi JSON có thuộc tính thừa.

---

## IV. JPA / HIBERNATE

### 1. JPA là gì?

- **JPA (Java Persistence API):** Không phải phần mềm mà là **Đặc tả (Specification)** — một tập quy ước về cách ánh xạ Java Object sang bảng DB.
- **Hibernate:** Phần mềm triển khai thực tế (Implementation) phổ biến nhất của JPA.
- **ORM (Object-Relational Mapping):** Cơ chế ánh xạ thuộc tính Java Class → cột DB Table.

**Khi gọi `save(entity)`:**
- Chưa có ID → `persist()` → `INSERT INTO ...`
- Đã có ID → `merge()` → `UPDATE ...`

---

### 2. Vòng đời của Entity (4 trạng thái)

```
new Entity()
    → TRANSIENT (Mới tạo, chưa quản lý, chưa có trong DB)
    ↓ save() / persist()
    → MANAGED (Đang quản lý bởi EntityManager)
    ↓ close() / clear()              ↓ delete() / remove()
    → DETACHED (Ngắt kết nối)       → REMOVED (Sẽ xóa khi commit)
    ↓ merge()
    → MANAGED
```

**Tính chất Dirty Checking (ở trạng thái MANAGED):**
- Khi Entity đang MANAGED, mọi thay đổi thuộc tính sẽ **tự động** sinh `UPDATE` khi transaction kết thúc **mà không cần gọi `.save()` lại**.

```java
@Transactional
public void updateStatus() {
    ProcessingComponent comp = repository.findById(304).orElseThrow(...);
    comp.setStatus(1);  // Chỉ cần set
    // KHÔNG cần gọi repository.save(comp) — JPA tự UPDATE!
}
```

---

### 3. @Transactional & Transaction Management

#### A. Tính chất ACID

| Chữ cái | Tính chất | Ý nghĩa |
|---------|----------|---------|
| **A** | Atomicity (Nguyên tử) | Tất cả cùng thành công, hoặc tất cả cùng thất bại |
| **C** | Consistency (Nhất quán) | Dữ liệu luôn đúng theo ràng buộc |
| **I** | Isolation (Cô lập) | Các transaction không can thiệp lẫn nhau |
| **D** | Durability (Bền vững) | Dữ liệu sau commit tồn tại vĩnh viễn |

#### B. Cơ chế Rollback

DB ghi tất cả thay đổi vào **Undo Log / WAL** trong bộ nhớ đệm trước. Khi có lỗi → Spring gửi lệnh `ROLLBACK` → DB dùng Undo Log khôi phục lại dữ liệu gốc.

#### C. Cơ chế Lan truyền (Propagation)

| Propagation | Hành vi | Khi có lỗi |
|------------|---------|-----------|
| `REQUIRED` (Mặc định) | Dùng chung transaction đang có; nếu chưa có thì tạo mới | Rollback cả transaction cha lẫn con |
| `REQUIRES_NEW` | **Luôn tạo transaction mới độc lập**, tạm dừng transaction cũ | Chỉ rollback transaction mới, không ảnh hưởng transaction cũ |

**Ứng dụng trong dự án — Batch Approve:**
```java
// Phê duyệt hàng loạt: Mỗi bản ghi có transaction riêng
// → Bản ghi lỗi chỉ rollback chính nó, không ảnh hưởng bản ghi khác
TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
transactionTemplate.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
for (Long id : ids) {
    results.add(approveSingleCategory(id, approver, transactionTemplate));
}
```

#### D. 3 cách dùng Transaction

| Cách | Khi dùng |
|------|---------|
| `@Transactional` | Thêm/Sửa/Xóa thông thường |
| `@Transactional(readOnly = true)` | Truy vấn SELECT — tắt Dirty Checking, tăng tốc |
| `TransactionTemplate` | Batch Processing — cần kiểm soát transaction thủ công từng phần tử |

#### E. `@Transactional` trên `private` — KHÔNG CÓ TÁC DỤNG!

- Spring dùng **AOP Proxy (CGLIB)** để bọc Bean và chặn lời gọi từ bên ngoài.
- `private` method → Proxy **không thể can thiệp** → `@Transactional` vô hiệu.
- **Self-invocation:** `publicA()` gọi `publicB()` (có `@Transactional`) trong cùng class → Transaction của B **cũng không hoạt động** vì đi qua `this`, không qua Proxy.

---

### 4. JPA Specification — Truy vấn động

**Định nghĩa:** Cơ chế tạo câu lệnh SQL động, an toàn (chống SQL Injection) dựa trên Criteria API.

**Tại sao không dùng hàm Repository thông thường?**
- `findByParamType(...)`, `findByStatus(...)` → chỉ lọc theo 1 điều kiện cố định.
- Specification → kết hợp N điều kiện linh hoạt tùy người dùng lọc ô nào.

**Cú pháp:**
```java
public static Specification<GroupCategory> filter(String paramType, List<Integer> statuses) {
    return (Root<GroupCategory> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
        List<Predicate> predicates = new ArrayList<>();

        // Chỉ thêm điều kiện nếu người dùng có nhập
        if (paramType != null && !paramType.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("paramType")),
                "%" + paramType.trim().toLowerCase() + "%"));
        }
        if (statuses != null && !statuses.isEmpty()) {
            predicates.add(root.get("status").in(statuses));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    };
}
```

**3 thành phần then chốt:**

| Tham số | Vai trò | Tương đương SQL |
|---------|---------|-----------------|
| `Root<T> root` | Bảng dữ liệu chính | `FROM PMH_GROUP_CATEGORY` |
| `CriteriaBuilder cb` | Nhà máy tạo điều kiện | `WHERE`, `LIKE`, `IN`, `AND`, `OR` |
| `CriteriaQuery<?> query` | Toàn bộ câu truy vấn | Toàn bộ câu `SELECT` |

**SQL tương đương:**
```sql
-- cb.like(cb.lower(root.get("paramType")), "%momo%")
WHERE LOWER(PARAM_TYPE) LIKE '%momo%'
```

---

## V. BẢO MẬT

### 1. JWT (JSON Web Token)

**Cấu trúc:** `Header.Payload.Signature` (3 phần, ngăn cách bằng dấu chấm)

**Luồng xác thực trong Payment Hub:**
```
1. Người dùng đăng nhập → AuthController trả về JWT Token
2. Frontend lưu Token vào LocalStorage
3. Mọi Request tiếp theo → auth.interceptor.ts tự gắn vào Header:
   Authorization: Bearer <jwt_token>
4. JwtAuthenticationFilter bắt Request → giải mã Token → trích xuất Username + Roles
5. Đặt vào SecurityContextHolder → Service lấy thông tin người dùng từ đây
```

### 2. SecurityUtils — Lấy Username từ Token (không từ Client)

```java
// ✅ Chuẩn: Lấy từ Token đã xác thực (không thể giả mạo)
String username = SecurityUtils.getCurrentUsername();

// ❌ Sai: Lấy từ tham số Client gửi lên (có thể giả mạo)
String username = request.getHeader("X-Username");
```

### 3. Phân quyền theo Role

| Role | Quyền |
|------|-------|
| `MAKER` | Tạo, Sửa, Xóa, Gửi duyệt, Hủy duyệt |
| `CHECKER` | Phê duyệt, Từ chối — **KHÔNG được là người tạo bản ghi đó** |

```java
@PreAuthorize("hasRole('MAKER')")
public ResponseEntity<?> create(...) { ... }

@PreAuthorize("hasRole('CHECKER')")
public ResponseEntity<?> batchApprove(...) { ... }
```

### 4. CORS (Cross-Origin Resource Sharing)

- Angular chạy tại `http://localhost:4200`, Backend tại `http://localhost:8080` → Khác origin → Trình duyệt chặn.
- `SecurityConfig` cấu hình CORS cho phép Angular truy cập Backend.

---

## VI. NGHIỆP VỤ NGÂN HÀNG

### 1. Hai hệ thống trạng thái độc lập

Mỗi bản ghi tham số có **2 trường trạng thái riêng biệt**:

#### A. `STATUS` — Phục vụ quy trình phê duyệt nội bộ

| Giá trị | Tên | Ý nghĩa |
|---------|-----|---------|
| `1` | Tạo mới (`NEW`) | Maker vừa tạo, chưa gửi duyệt |
| `3` | Chờ duyệt (`PENDING`) | Maker đã gửi lên cho Checker |
| `4` | Đã duyệt (`APPROVED`) | Checker phê duyệt, chính thức có hiệu lực |
| `5` | Từ chối (`REJECTED`) | Checker từ chối, kèm lý do |
| `7` | Hủy duyệt (`CANCELED`) | Maker rút lại để chỉnh sửa |

#### B. `IS_ACTIVE` — Phục vụ hệ thống Payment chạy thực tế

| Giá trị | Ý nghĩa |
|---------|---------|
| `1` | Đang hoạt động — ngày hiện tại trong khoảng `[effectiveDate, endEffectiveDate]` |
| `0` | Không hoạt động — ngoài khoảng ngày hiệu lực |

> Được cập nhật tự động bởi `ActiveStatusScheduler` chạy mỗi 5 giây.

---

### 2. Ma trận trạng thái — Được phép sửa

| STATUS | Được sửa? | Lý do |
|--------|----------|-------|
| `1 - Tạo mới` | ✅ | Chưa gửi duyệt, được sửa thoải mái |
| `3 - Chờ duyệt` | ❌ | Đang trong hàng đợi Checker, cấm can thiệp |
| `4 - Đã duyệt` | ❌ | Đang chạy thật. Phải Hủy duyệt (7) trước |
| `5 - Từ chối` | ✅ | Maker được sửa lại và gửi duyệt lại |
| `7 - Hủy duyệt` | ✅ | Maker sửa → lưu tạm vào `NEW_DATA` |

---

### 3. Cơ chế lưu nháp qua cột `NEW_DATA`

**Vấn đề:** Khi Maker muốn sửa bản ghi đã được duyệt (`STATUS = 4`), không thể ghi đè trực tiếp vì hệ thống Payment Hub đang dùng bản ghi đó.

**Giải pháp:**
1. Maker thực hiện Hủy duyệt → `STATUS = 7`.
2. Maker sửa nội dung → Backend **serialization** DTO thành JSON → lưu vào cột `NEW_DATA`.
3. `IS_DISPLAY = 2` (đánh dấu đã từng được duyệt).
4. Hệ thống Payment Hub tiếp tục đọc **dữ liệu cũ** trong các cột chính, không bị gián đoạn.
5. Khi Checker bấm Duyệt → Backend **deserialization** JSON từ `NEW_DATA` → ghi đè vào các cột chính → xóa `NEW_DATA`.

**Màn hình Chi tiết:** Hiển thị **Form đối chiếu 2 cột** (Dữ liệu cũ vs Dữ liệu mới trong `NEW_DATA`) để Checker so sánh.

---

### 4. Nguyên tắc 4 Mắt (Maker - Checker / Four-Eyes Principle)

**Định nghĩa:** Không một cá nhân nào (kể cả Admin) được vừa tạo vừa tự phê duyệt yêu cầu của chính mình.

**Cài đặt trong dự án:**
```java
// Trong approveSingleCategory():
if (approver.equalsIgnoreCase(entity.getCreatedBy()) ||
    approver.equalsIgnoreCase(entity.getUpdatedBy())) {
    throw new ForbiddenAccessException("Người phê duyệt không được trùng với người tạo/cập nhật!");
}
```

---

### 5. Quy tắc bảo vệ dữ liệu lịch sử (`IS_DISPLAY`)

| IS_DISPLAY | Tên | Ý nghĩa |
|-----------|-----|---------|
| `1` | `INITIAL` | Bản ghi mới, chưa từng được duyệt — **được phép xóa** |
| `2` | `ONCE_APPROVED` | Đã từng được duyệt — **TUYỆT ĐỐI KHÔNG ĐƯỢC XÓA** |

```java
// Trong delete():
if (entity.getIsDisplay() == DisplayStatus.ONCE_APPROVED.getCode()) {
    throw new IllegalStateException("Bản ghi đã từng được phê duyệt, không được phép xóa!");
}
```

---

### 6. Chặn gửi duyệt khống (`isDtoDifferentFromEntity`)

- Backend so sánh từng trường của DTO gửi lên với Entity trong DB.
- Nếu không có bất kỳ trường nào thay đổi → ném `IllegalStateException("Dữ liệu trùng khớp 100%")`.
- Frontend cũng có hàm `hasFormChanged()` kiểm tra tương tự trước khi gọi API.

---

### 7. Enum chuẩn hóa — Thay thế Magic Numbers

```java
// ❌ Magic Number — khó hiểu, dễ sai:
if (entity.getStatus() == 4) { ... }

// ✅ Enum — rõ ràng, type-safe:
if (entity.getStatus() == ParamStatus.APPROVED.getCode()) { ... }
```

**Các Enum trong dự án:**
- `ParamStatus`: `NEW(1)`, `PENDING(3)`, `APPROVED(4)`, `REJECTED(5)`, `CANCELED(7)`
- `DisplayStatus`: `INITIAL(1)`, `ONCE_APPROVED(2)`
- `ActiveStatus`: `INACTIVE(0)`, `ACTIVE(1)`
- `AuditAction`: `CREATE`, `UPDATE`, `DELETE`, `SEND_APPROVAL`, `APPROVE`, `REJECT`, `CANCEL_APPROVAL`

---

### 8. Audit Log (Lịch sử thao tác)

Sau **mỗi thao tác thành công**, Service đều gọi `auditLogService.log()` để ghi lại vào `PMH_AUDIT_LOGS`:
- Module nào (GroupCategory / Component)
- ID bản ghi
- Hành động (CREATE / APPROVE / REJECT...)
- Người thực hiện (từ JWT)
- Dữ liệu trước/sau khi thay đổi

---

### 9. Stored Procedure — Gọi Oracle SP từ Java

```java
// Khai báo và gọi Stored Procedure qua EntityManager:
StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_APPROVE_GROUP_CATEGORY");
query.registerStoredProcedureParameter("p_id",      Long.class,    ParameterMode.IN);
query.registerStoredProcedureParameter("p_user",    String.class,  ParameterMode.IN);
query.registerStoredProcedureParameter("p_status",  Integer.class, ParameterMode.OUT);
query.registerStoredProcedureParameter("p_message", String.class,  ParameterMode.OUT);

query.setParameter("p_id", id);
query.setParameter("p_user", approver);
query.execute();

// Đọc kết quả trả về từ tham số OUT:
Integer spStatus  = (Integer) query.getOutputParameterValue("p_status");
String  spMessage = (String)  query.getOutputParameterValue("p_message");
boolean success   = (spStatus != null && spStatus == 1);
```

**Phân biệt `IN` và `OUT`:**
- `ParameterMode.IN`: Truyền **vào** Procedure (dữ liệu đầu vào)
- `ParameterMode.OUT`: Nhận **ra** từ Procedure (kết quả trả về)

---

## VII. ANGULAR FRONTEND

### 1. Reactive Forms — Khởi tạo Form

```typescript
private initForm() {
    this.dialogForm = this.fb.group({
        componentCode:    ['',    zodFieldValidator(ComponentSchema, 'componentCode')],
        componentName:    ['',    zodFieldValidator(ComponentSchema, 'componentName')],
        messageType:      [[],    zodFieldValidator(ComponentSchema, 'messageType')],   // [] = mảng, chọn nhiều
        checkToken:       [false, zodFieldValidator(ComponentSchema, 'checkToken')],    // boolean → checkbox
        isActive:         [1,     zodFieldValidator(ComponentSchema, 'isActive')],
        effectiveDate:    ['',    zodFieldValidator(ComponentSchema, 'effectiveDate')],
    }, { validators: zodFormValidator(ComponentSchema) });
}
```

**Giá trị khởi tạo:**
- `''` (chuỗi rỗng) → TextBox — nhập ký tự
- `[]` (mảng rỗng) → Multi-select / Checkbox group — chọn nhiều
- `false` / `true` → Checkbox đơn
- `0` / `1` → Số nguyên (trạng thái)

**Zod Validator:**
- `zodFieldValidator(Schema, 'fieldName')` → kiểm tra từng trường riêng lẻ
- `zodFormValidator(Schema)` → kiểm tra toàn bộ form (cross-field validation)
- Dữ liệu sai → Angular chặn lại **trước khi gửi lên Server**.

---

### 2. @Input() và @Output() — Truyền dữ liệu giữa Component

**@Input() — Cha gửi xuống Con:**
```typescript
// Trong component Con:
@Input() title: string = '';
@Input() rows: ComparisonRow[] = [];

// Trong HTML của Cha:
<app-comparison-card [title]="'Dữ liệu cũ'" [rows]="oldDataRows"></app-comparison-card>
```

**@Output() — Con báo lên Cha:**
```typescript
// Trong component Con:
@Output() confirmed = new EventEmitter<void>();
onConfirmClick() { this.confirmed.emit(); }

// Trong HTML của Cha:
<app-con (confirmed)="handleParentConfirm()"></app-con>
```

---

### 3. Lifecycle Hooks — Vòng đời Component

| Thứ tự | Hook | Trạng thái | Dùng để |
|--------|------|-----------|--------|
| 1 | `constructor()` | `@Input` **chưa có giá trị** | Tiêm Service (`inject`) |
| 2 | `ngOnChanges()` | `@Input` vừa nhận giá trị | Phản ứng với thay đổi từ cha |
| 3 | `ngOnInit()` | `@Input` **sẵn sàng** | **Gọi API tải dữ liệu ban đầu** |
| 4 | `ngAfterViewInit()` | DOM đã render xong | Thao tác DOM, Chart |
| 5 | `ngOnDestroy()` | Component sắp bị hủy | Hủy subscription, dọn bộ nhớ |

**Tại sao gọi API trong `ngOnInit()` thay vì `constructor()`?**
- Trong `constructor`, các `@Input()` vẫn là `undefined`.
- Trong `ngOnInit`, tất cả `@Input()` đã được nạp đầy đủ.

---

### 4. Hàm `inject()` — Tiêm phụ thuộc hiện đại (Angular 14+)

```typescript
// Cách cũ:
constructor(private router: Router, private authService: AuthService) {}

// Cách mới (Angular 14+):
private router = inject(Router);
private authService = inject(AuthService);
```

**Ưu điểm:**
1. Kế thừa class cha (`extends BaseComponent`) không cần gọi `super(dep1, dep2...)` phức tạp.
2. Có thể dùng trong Functional Guard/Interceptor ngoài class.

**Lưu ý:** Chỉ gọi trong **Injection Context** (lúc khai báo biến class hoặc trong constructor). Gọi bên trong click handler sẽ crash.

---

### 5. Angular Signals — Quản lý trạng thái

```typescript
// Khai báo Signal:
categories = signal<GroupCategoryResponse[]>([]);
isLoading  = signal<boolean>(false);

// Cập nhật:
this.categories.set(data);
this.isLoading.set(true);

// Đọc trong template:
@if (isLoading()) { <span>Đang tải...</span> }
@for (item of categories(); track item.id) { ... }
```

---

### 6. Toán tử `||` vs `??`

| Toán tử | Khi nào kích hoạt mặc định | Nguy hiểm với |
|---------|--------------------------|--------------|
| `\|\|` | Giá trị là **Falsy**: `false, 0, "", null, undefined, NaN` | `0` và `""` hợp lệ bị ghi đè |
| `??` | Giá trị là **Nullish**: chỉ `null` hoặc `undefined` | Bảo toàn `0`, `false`, `""` |

```typescript
const discount = 0 || 5;   // = 5 ❌ (Sai: người dùng giảm giá 0% bị đổi thành 5%)
const discount = 0 ?? 5;   // = 0 ✅ (Đúng: giữ nguyên 0%)
```

---

### 7. Taiga UI — Thư viện UI ngân hàng

**Checkbox với Taiga UI:**
```html
<!-- ❌ Sai: [checked] không hoạt động với tuiCheckbox -->
<input tuiCheckbox type="checkbox" [checked]="isAllSelected()">

<!-- ✅ Đúng: Phải dùng [ngModel] vì tuiCheckbox dùng ControlValueAccessor -->
<input tuiCheckbox type="checkbox" [ngModel]="isAllSelected()" (change)="toggleSelectAll($event)">
```

**DatePicker Taiga UI — Value Transformer:**

Vấn đề: Taiga UI yêu cầu `[TuiDay, TuiTime]`, Backend yêu cầu `ISO String`.

Giải pháp: `DateTimeTransformer` tự động chuyển đổi 2 chiều:
```typescript
@Injectable()
export class DateTimeTransformer implements TuiValueTransformer<[TuiDay, TuiTime | null] | null, string> {
    // Form → Lịch (ISO String → [TuiDay, TuiTime])
    fromControlValue(value: string | null): [TuiDay, TuiTime | null] | null {
        if (!value) return null;
        const d = new Date(value);
        return [TuiDay.fromLocalNativeDate(d), TuiTime.fromLocalNativeDate(d)];
    }

    // Lịch → Form ([TuiDay, TuiTime] → ISO String)
    toControlValue(value: [TuiDay, TuiTime | null] | null): string {
        if (!value) return '';
        const [day, time] = value;
        const d = day.toLocalNativeDate();
        if (time) d.setHours(time.hours, time.minutes, time.seconds);
        return d.toISOString();
    }
}
```

---

### 8. Interceptor — Tự động đính kèm JWT

```typescript
// auth.interceptor.ts — Tự động thêm Token vào mọi request:
export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const token = localStorage.getItem('token');
    if (token) {
        req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }
    return next(req).pipe(
        catchError(err => {
            if (err.status === 401 || err.status === 403) {
                authService.logout();  // Tự động logout khi token hết hạn
            }
            return throwError(() => err);
        })
    );
};
```

---

## VIII. HIỆU NĂNG

### 1. Batch Processing — Phê duyệt hàng loạt

**Vấn đề:** Gửi 10 request độc lập lên server = 10 lần mở kết nối DB.

**Giải pháp:** Frontend gửi 1 request với danh sách ID:
```typescript
// Frontend:
this.service.batchApprove(selectedIds).subscribe(...);

// Backend - mỗi ID có transaction riêng:
for (Long id : ids) {
    results.add(approveSingleCategory(id, approver, transactionTemplate));
}
// Kết quả: { id: 1, success: true } , { id: 2, success: false, message: "lỗi..." }
```

---

### 2. Scheduler — Tự động cập nhật IS_ACTIVE mỗi 5 giây

```java
@Scheduled(fixedRate = 5000)
@Transactional
public void updateActiveStatuses() {
    // Bulk UPDATE — cập nhật hàng ngàn bản ghi chỉ 1 câu SQL:
    entityManager.createNativeQuery(
        "UPDATE PMH_GROUP_CATEGORY SET IS_ACTIVE = CASE " +
        "WHEN EFFECTIVE_DATE <= CURRENT_TIMESTAMP AND " +
        "     (END_EFFECTIVE_DATE IS NULL OR CURRENT_TIMESTAMP <= END_EFFECTIVE_DATE) THEN 1 " +
        "ELSE 0 END"
    ).executeUpdate();
}
```

---

### 3. TrackBy — Tối ưu DOM Rendering

**Vấn đề:** Mỗi khi data thay đổi (phân trang, tìm kiếm), Angular xóa hết DOM cũ và vẽ lại từ đầu → chớp nháy màn hình.

**Giải pháp:** `trackBy` giúp Angular chỉ cập nhật đúng các DOM node thay đổi:
```typescript
trackById(index: number, item: GroupCategoryResponse): number {
    return item.id;
}
```
```html
@for (item of categories(); track item.id) { ... }
```

---

### 4. Resize cột bảng — 60 FPS

```typescript
// requestAnimationFrame giúp đồng bộ với chu kỳ vẽ của trình duyệt → 60 FPS:
onMouseMove(event: MouseEvent) {
    requestAnimationFrame(() => {
        this.columnWidths[this.resizingColIndex] = newWidth;
    });
}

// Chống kích hoạt nhầm Sort sau khi resize:
onMouseUp() {
    this.justResized = true;
    setTimeout(() => { this.justResized = false; }, 150);
}
```

---

## IX. LỘ TRÌNH PRODUCTION & TECHNICAL DEBTS

### 1. Đánh giá hiện trạng

**Đã hoàn thiện:**
- ✅ Khóa xóa vật lý bản ghi đã từng duyệt (`isDisplay = 2`)
- ✅ Nguyên tắc 4 mắt: Maker không tự phê duyệt bản ghi của mình
- ✅ Chặn gửi duyệt khống (`isDtoDifferentFromEntity` + `hasFormChanged`)
- ✅ Xử lý 100% ngoại lệ tập trung (GlobalExceptionHandler)
- ✅ Lấy username từ JWT (không từ Client)

### 2. 6 Vấn đề tồn đọng cần nâng cấp cho Production

| # | Vấn đề | Giải pháp | Tiêu chí nghiệm thu |
|---|--------|-----------|-------------------|
| 1 | `NEW_DATA` lưu Plaintext JSON | Mã hóa AES-256 bằng `CryptoUtils` | DB không còn Plaintext JSON |
| 2 | Logout không thu hồi JWT | Redis JWT Blacklist + Refresh Token Rotation | Token cũ sau logout không gọi được API |
| 3 | `/api/auth/login` không giới hạn thử sai | Bucket4j Rate Limiter | Sai 5 lần → HTTP 429, khóa 15 phút |
| 4 | Angular Bundle > 500KB | Lazy Load Taiga UI Icons | `ng build` không còn cảnh báo budget |
| 5 | Không có Unit Test tự động | JUnit 5 + Mockito (Backend) + Jasmine (Frontend) | Code Coverage Service > 80% |
| 6 | Giám sát thủ công qua log file | Spring Actuator + Prometheus + Grafana | Biểu đồ CPU/RAM/Connection Pool realtime |

### 3. Lộ trình Go-Live (6 tuần)

```
GIAI ĐOẠN 1 — DEVSEC OPS HARDENING (Tuần 1-2):
  ├── Tích hợp Redis JWT Blacklist khi Logout
  ├── Mã hóa AES-256 cho NEW_DATA
  └── Rate Limiter Bucket4j cho Endpoint Đăng nhập

GIAI ĐOẠN 2 — QUALITY ASSURANCE & TESTING (Tuần 3-4):
  ├── JUnit 5 + Mockito cho toàn bộ Service Layer Backend
  ├── Jasmine Component Tests cho Angular Frontend
  └── Penetration Test & SonarQube Security Scanning

GIAI ĐOẠN 3 — OPTIMIZATION & MONITORING (Tuần 5-6):
  ├── Lazy Load Taiga UI Icons (Bundle < 500KB)
  ├── Spring Boot Actuator + Prometheus + Grafana
  └── Docker Container + K8s Deployment Manifests
```

---

## BẢNG TÓM TẮT TOÀN BỘ KIẾN THỨC

| Bài | Chủ đề cốt lõi |
|-----|---------------|
| Learn 01 | Reactive Forms Angular, `zodFieldValidator`, giá trị khởi tạo Form |
| Learn 02 | `@RequestBody`, `@Valid`, `@NotBlank`, `@Pattern`, Controller binding |
| Learn 03 | Maker-Checker, Stored Procedure, `ParameterMode.IN/OUT`, Transaction Rollback |
| Learn 04 | JPA Specification, CriteriaBuilder, `cb.like()`, `cb.lower()`, `cb.conjunction()` |
| Learn 05 | Batch Processing, `TransactionTemplate`, `PROPAGATION_REQUIRES_NEW` |
| Learn 06 | Toàn bộ Annotation Spring Boot, OOP 4 tính chất, Luồng Request 3 tầng, Interface vs Abstract Class, JPA bản chất, Entity States, `@Transactional` |
| Learn 07 | `||` vs `??`, `@Input()/@Output()`, Lifecycle Hooks, `inject()`, DateTimeTransformer, Taiga UI ngModel |
| Learn 08 | Hai trạng thái `STATUS`/`IS_ACTIVE`, State Machine luồng phê duyệt, `NEW_DATA`, Dirty Checking, Batch Approve, Scheduler, trackBy, Resize 60FPS |
| Learn 09 | Stream API, Optional, DI 3 cách, Collections + hashCode/equals, Builder Pattern, Java Time API, JPA Specification nâng cao |
| Learn 10 | Audit toàn bộ codebase: Security package, Controller, Service, Entity, Enum, DTO, Frontend Feature, Nghiệp vụ Ngân hàng lõi, Ma trận lỗi |
| Learn 11 | Technical Debts: AES-256, JWT Blacklist, Rate Limiting, Bundle Optimization, Unit Test, Grafana Monitoring |

---

> **Ghi chú:** Tài liệu này được tổng hợp từ 12 file học thực chiến (`learn01` → `learn11`), phản ánh toàn bộ kiến thức đã học và áp dụng trong dự án **Payment Hub** — Hệ thống quản lý cấu hình tham số thanh toán ngân hàng.
