# Bài toán: Tìm kiếm và Xuất dữ liệu Transaction Log quy mô lớn

---Cần đọc lại các file giao diện, cấu trúc, code ... cảu các module trước để viết giao diện 1 cách nhất quán, sạch

## 1. Bối cảnh

Hệ thống PaymentHub hiện có bảng `TRANSACTION_LOG` trên Oracle Database, lưu trữ toàn bộ lịch sử giao dịch thanh toán. Bảng đã đạt quy mô **~1 triệu bản ghi** và tiếp tục tăng trưởng theo thời gian.

### Schema bảng TRANSACTION_LOG

| Cột | Kiểu dữ liệu | Mô tả |
|-----|-------------|-------|
| `ID` | NUMBER | Khóa chính, tự tăng tuần tự |
| `TRANSACTION_CODE` | VARCHAR2 | Mã giao dịch, ví dụ: `TXN20260912-283` |
| `ACCOUNT_NO` | VARCHAR2 | Số tài khoản |
| `AMOUNT` | NUMBER | Số tiền giao dịch |
| `STATUS` | VARCHAR2 | Trạng thái: `SUCCESS`, `FAILED`, `PENDING` |
| `CREATED_AT` | TIMESTAMP | Thời điểm tạo giao dịch |

> **Lưu ý:** Bảng có thể có UPDATE (cột `STATUS` thay đổi sau khi giao dịch hoàn tất)

---

## 2. Yêu cầu chức năng

### 2.1. Tìm kiếm và phân trang (dựa trên các file module đã làm trước đó)
- Người dùng có thể tìm kiếm transaction log theo các bộ lọc:
  - `STATUS`: SUCCESS / FAILED / PENDING
  - `ACCOUNT_NO`: số tài khoản (tìm kiếm chính xác hoặc gần đúng)
  - `CREATED_AT`: khoảng thời gian từ ngày — đến ngày
- Kết quả hiển thị phân trang, mặc định 10 bản ghi/trang
- Hỗ trợ sắp xếp theo các cột

### 2.2. Xuất file CSV
- Người dùng có thể xuất **toàn bộ kết quả tìm kiếm** ra file CSV
- File CSV bao gồm tất cả bản ghi khớp với bộ lọc đang áp dụng, **không giới hạn số lượng**
- File phải mở được bằng Excel (UTF-8 BOM, dấu phẩy phân cách)
- Người dùng nhận được thông báo tiến độ trong quá trình xuất
- Người dùng nhận được file khi quá trình hoàn thành

---

## 3. Yêu cầu phi chức năng

### 3.1. Hiệu năng
- API tìm kiếm phân trang: phản hồi trong **< 2 giây** ở mọi kích thước dữ liệu
- Bắt đầu xuất file: hệ thống trả về **HTTP 202 Accepted trong < 100ms** (không timeout)

### 3.2. Độ tin cậy
- **Không bị HTTP timeout**: thao tác xuất file không bị giới hạn bởi HTTP timeout 30s
- **Không mất job khi server restart**: trạng thái job phải được lưu bền vững
- **Không OOM (Out of Memory)**: không được load toàn bộ 1 triệu bản ghi vào RAM cùng lúc
- **Không làm quá tải Oracle**: số lượng query lớn chạy đồng thời phải được kiểm soát

### 3.3. Tính đồng thời
- Hỗ trợ **nhiều người dùng** thực hiện tìm kiếm đồng thời
- Hỗ trợ **nhiều người dùng** yêu cầu xuất file đồng thời
- Hệ thống phải có cơ chế **backpressure**: từ chối yêu cầu mới khi đã đạt giới hạn xử lý
- Mỗi người dùng chỉ được có **1 job xuất file** đang chạy tại một thời điểm
- **Giới hạn toàn hệ thống (Global Rate Limit)**: Tối đa **3 job export** chạy song song đồng thời. ThreadPool có queue tối đa 10 job chờ. Khi vượt ngưỡng → từ chối với HTTP 503.
- Export worker phải dùng **DataSource riêng biệt** (connection pool riêng, tối đa 3 connections) để không ảnh hưởng đến 10 connections HikariCP dùng cho Search API.

### 3.4. Phục hồi lỗi
- Nếu worker bị crash giữa chừng: hệ thống tự động phát hiện và đánh dấu job **FAILED**
- Nếu server restart: các job đang chạy phải được phát hiện và xử lý
- Job **không được bị stuck** ở trạng thái PROCESSING vô thời hạn

### 3.5. Bảo mật
- Yêu cầu xác thực: chỉ người dùng có quyền permistion 'view' mới được truy cập

### 3.6. Tính nhất quán dữ liệu
- File xuất ra phải **không chứa bản ghi mới** được insert sau khi yêu cầu xuất bắt đầu — đảm bảo bằng `MAX_EXPORT_ID = MAX(ID)` tại thời điểm $t_0$, mỗi batch chỉ đọc `WHERE ID <= :maxExportId`.
- File xuất ra phản ánh trạng thái dữ liệu **trong cửa sổ thời gian xuất** (READ COMMITTED)
- Chấp nhận: bản ghi bị UPDATE trong quá trình xuất có thể có giá trị tại thời điểm đọc, không phải tại thời điểm bắt đầu job
- **Không sử dụng Oracle Flashback Query (`AS OF SCN`)**: cơ chế Flashback nghiêm ngặt hơn yêu cầu (đóng băng hoàn toàn tại $t_0$, mâu thuẫn với việc chấp nhận READ COMMITTED ở trên), đồng thời có rủi ro thực tế `ORA-01555: snapshot too old` nếu Undo bị tái sử dụng khi job chạy lâu. MAX_EXPORT_ID đơn giản và đủ an toàn cho bài toán này.

---

## 4. Ràng buộc hệ thống

| Ràng buộc | Giá trị |
|-----------|---------|
| HTTP timeout server | 30 giây |
| HikariCP connection pool (Search API) | 10 connections |
| Export Worker connection pool | 3 connections (pool riêng, tối đa 3 job song song) |
| Export ThreadPool | core=3, max=3, queue=10, RejectedExecution → HTTP 503 |
| Database | Oracle (đã cài đặt) |
| Backend framework | Spring Boot |
| Frontend framework | Angular |
| Deployment | Có thể chạy nhiều instance (dùng ShedLock cho Scheduler) |

---

## 5. Tình huống cần xử lý

| Tình huống | Yêu cầu |
|------------|---------|
| 1 người dùng xuất 1 triệu bản ghi | Không timeout, không OOM, hoàn thành trong SLA |
| 10 người dùng đồng thời xuất file | Giới hạn số lượng job song song, các job còn lại xếp hàng chờ |
| 100 người dùng đồng thời bấm xuất | Từ chối yêu cầu vượt quá giới hạn với thông báo rõ ràng |
| Server crash giữa lúc xuất | Job bị detect là FAILED, người dùng được thông báo, có thể thử lại |
| Người dùng tắt trình duyệt giữa chừng | Background job tiếp tục chạy, kết quả sẵn sàng khi mở lại |
| Người dùng bấm "Xuất" nhiều lần liên tục | Hệ thống từ chối tạo job trùng lặp |
| Bản ghi bị UPDATE trong lúc đang xuất | Hệ thống chấp nhận giá trị tại thời điểm đọc (READ COMMITTED) |

---

## 6. Trải nghiệm người dùng (UX)

| Thời điểm | Hệ thống hiển thị |
|-----------|-------------------|
| Bấm "Xuất file" | Nút disabled, spinner, thông báo "Đang khởi tạo yêu cầu..." |
| Job đang xử lý | Thông báo "Đang xử lý: X dòng đã xử lý" (cập nhật theo thời gian thực) |
| Job hoàn thành | Toast success "Đã xử lý xong, bắt đầu tải ngay" → file tự động tải về (kèm nút "Tải về lại" dự phòng) |
| Job thất bại | Toast error "Xuất thất bại: [lý do]" → nút "Thử lại" |
| Bấm xuất khi đã có job đang chạy | Cảnh báo "Đang có yêu cầu xuất đang xử lý, vui lòng chờ" |
| Hệ thống quá tải | Thông báo "Hệ thống đang bận, vui lòng thử lại sau vài phút" |
| Mở lại trình duyệt | Hiển thị thông báo danh sách file xuất hoàn thành (còn hiệu lực trong 12h) kèm đếm ngược và nút "Tải về" |

---

## 7. Câu hỏi mở (cần xác nhận trước khi thiết kế chi tiết)

- [x] Số lượng Backend instance tối đa trong môi trường production? → **Không có, đang dùng 1 instance**
- [x] Infrastructure hiện tại có sẵn Redis không? → **Có sẵn**
- [x] Infrastructure hiện tại có sẵn MinIO / shared storage không? → **Sử dụng MinIO Object Storage** làm lưu trữ tập trung file CSV xuất ra. Cấu hình MinIO Bucket Lifecycle để tự động xóa file tạm sau 12h.
- [x] Oracle có cấu hình `UNDO_RETENTION` đủ cho Flashback Query không? → **Oracle tự động điều chỉnh Undo Retention theo thời gian chạy thực tế của từng Job (Automatic Undo Tuning) nhờ cấu hình `AUTOEXTEND ON` trên Undo Tablespace.** ⚠️ _Câu hỏi này đã lỗi thời — quyết định cuối (xem mục 3.6): module export không dùng Flashback Query, do đó không phụ thuộc vào UNDO_RETENTION. `AUTOEXTEND ON` vẫn là best practice chung của DBA, nhưng không phải yêu cầu bắt buộc cho tính năng này._
- [x] SLA cho thời gian xuất 1 triệu bản ghi là bao nhiêu phút? → **không có SLA cụ thể, xong là được**
- [x] File CSV sau khi xuất cần lưu trữ bao lâu trước khi bị xóa? → **12 tiếng** kể từ khi xuất hoàn thành. MinIO tự động dọn dẹp bằng Bucket Lifecycle Rule (Zero Backend Overhead).
- [x] Có cần audit log cho thao tác xuất file không? → **Không cần**

---

## 8. Thiết kế & Kế hoạch triển khai Giao diện (Frontend Architecture & Plan)


Để đảm bảo **nhất quán, sạch sẽ (Clean Code)** và tuân thủ đúng chuẩn kiến trúc hiện tại của dự án (`features/category`, `features/processing-components`), module `Transaction Log` phía Frontend sẽ được xây dựng theo các quy tắc sau:

### 8.1. Cấu trúc thư mục (Folder Structure)
Mã nguồn frontend được đặt tại `src/app/features/transaction-log/`:
```text
src/app/features/transaction-log/
├── components/
│   └── transaction-log-list/
│       ├── transaction-log-list.ts       # Main Component kế thừa BaseListComponent
│       ├── transaction-log-list.html     # Template đồng bộ Taiga UI & Tailwind/CSS
│       └── transaction-log-list.css      # Style riêng cho module
├── services/
│   └── transaction-log.service.ts        # Call API Search, Active Job & Download
├── models/
│   └── transaction-log.model.ts          # Interface DTOs (Request/Response/ExportJob)
└── transaction-log.routes.ts             # Route module với Auth/Permission Guard
```

### 8.2. Kế thừa & Tái sử dụng Component dùng chung (`BaseListComponent`)
* `TransactionLogListComponent` kế thừa từ `BaseListComponent<TransactionLogResponse, number>`:
  * **Columns Def (`TableColumnDef[]`)**: Định nghĩa các cột `checkbox`, `stt`, `transactionCode`, `accountNo`, `amount`, `status`, `createdAt`, `actions`.
  * **Form Tìm kiếm (`FormBuilder`)**: Định nghĩa `searchForm` gồm `status`, `accountNo`, `fromDate`, `toDate`.
  * **State Management**: Sử dụng Angular **Signals** (`logs`, `isLoading`, `exportJob`, `activeJobCountdown`) đồng bộ với codebase mới.
  * **Dialogs & Actions**: Re-use `ConfirmDialogComponent`, `AuditHistoryDialogComponent` và `NotificationService` (wrapper của `TuiNotificationService`).

### 8.3. Quản lý Luồng Xuất File & Polling Tiến Độ ngầm
```text
[Bấm "Xuất file"] ──> POST /export-jobs ──> (202 Accepted) ──> Toast Info "Đang khởi tạo..."
                                                                          │
                                                                          ▼
                                                         Bắt đầu Polling timer(0, 3000)
                                                                          │
                        ┌─────────────────────────────────────────────────┴────────────────────────────────────────────────┐
                        ▼                                                 ▼                                                ▼
           status == 'PROCESSING'                           status == 'COMPLETED'                             status == 'FAILED'
    Cập nhật Toast tiến độ real-time                 1. Tắt Polling loop                              1. Tắt Polling loop
    "Đang xử lý: X / 1.000.000 dòng"                2. Toast Success "Đã xử lý xong"                2. Toast Error + nút Thử lại
                                                     3. Tự động trigger download blob file
                                                     4. Bật nút "Tải lại" trên Toast
```

### 8.4. Tích hợp Quả chuông trên Header (`HeaderComponent` - Notification Center)
Tận dụng Icon Quả chuông `<tui-icon icon="@tui.bell">` có sẵn tại `src/app/layout/header/`:
* **State Binding**: `HeaderComponent` kết nối với `ExportNotificationService` qua Signal `notificationCount()`.
* **Popover Lịch sử Export (Dropdown khi click Quả chuông)**:
  * Hiển thị danh sách các Export Jobs của User trong **12 tiếng gần nhất** (cả `COMPLETED` và `FAILED`).
  * Mỗi thẻ thông báo bao gồm:
    * Tên file & số dòng (VD: `Transaction_Log_1M.csv` - 1.000.000 dòng).
    * Trạng thái + Bộ đếm ngược thời gian lưu trữ local (`Còn hiệu lực: HH:mm:ss`).
    * **Nút Action**: `[Tải về]` (nếu COMPLETED) hoặc `[Thử lại]` (nếu FAILED).
* **Tự động đồng bộ (Re-hydration)**: Khi người dùng F5 hoặc mở lại trình duyệt, `HeaderComponent` gọi `GET /api/v1/transaction-log/export-jobs/my-jobs` để lấy danh sách thông báo và cập nhật số lượng Badge trên quả chuông.

### 8.5. Khôi phục Trạng thái khi F5 / Mở lại Trình duyệt
* **Khi Init Component (`ngOnInit`)**:
  1. FE tự động kiểm tra Active Jobs của User.
  2. Nếu có Job `PROCESSING` $\rightarrow$ Tự động bật lại Polling loop 3s và hiển thị Toast tiến độ real-time.
  3. Nếu có Job `COMPLETED` hoặc `FAILED` $\rightarrow$ Đẩy vạch đếm Badge màu đỏ lên Icon Quả chuông trên Header và mở sẵn Toast thông báo.

### 8.6. Quy tắc Code Clean & UX Protections
1. **Chống Spam Request**: Disable nút "Xuất file" khi đang có Job `PROCESSING` của chính user đó.
2. **Fallback Auto-Download**: Nếu trình duyệt chặn auto-download, nút "Tải về" trên Quả chuông / Toast luôn sẵn sàng kích hoạt theo `User Gesture`.
3. **Giải phóng Bộ nhớ (Memory Leak Prevention)**: Unsubscribe tất cả Polling Observables khi `ngOnDestroy()`.

---

## 9. Kiến trúc Lưu trữ & Dự phòng Sự cố (Storage & HA Architecture)

### 9.1. Lưu trữ File CSV kết quả (MinIO Object Storage)
* **Tách biệt dữ liệu**: Oracle DB chỉ lưu thông tin metadata của Job trong bảng `EXPORT_JOB`. Toàn bộ file kết quả `.csv` (150MB - 300MB) được tải và lưu trữ trên **MinIO Object Storage**.
* **Tự động dọn dẹp (Bucket Lifecycle Expiration)**:
  * Cấu hình **MinIO Object Lifecycle Rule** tự động xóa vĩnh viễn các file CSV trong Bucket `export-logs` sau **12 tiếng** (`Expiration: 12 Hours / 1 Day`).
  * Backend Spring Boot **không cần** chạy CronJob dọn dẹp thủ công, giải phóng hoàn toàn CPU/RAM cho Server Backend.
* **Hỗ trợ Multi-Instance / Cluster**: Cho phép nhiều Server Backend cùng đọc/ghi file CSV tập trung qua MinIO Presigned URL.
* **Linh hoạt nâng cấp (`FileStorageService` Interface)**:
  * Định nghĩa `FileStorageService` Interface bọc các thao tác `store()`, `load()`, `delete()`.
  * Môi trường Dev/Standalone dùng `LocalStorageServiceImpl` (`./storage/exports/`).
  * Môi trường Prod/Cluster dùng `MinioStorageServiceImpl` kết nối MinIO Cluster.

### 9.2. Giải pháp Dự phòng Database (High Availability / Disaster Recovery)
* **Oracle Active Data Guard (Primary - Standby)**:
  * Database chính (Primary DB) đồng bộ Redo Logs thời gian thực (Real-time) sang Database dự phòng (Standby DB).
  * Khi đĩa hoặc server Primary DB gặp sự cố, hệ thống Oracle kích hoạt **Failover** chuyển vai trò Standby DB thành Primary DB mới.
* **Cấu hình Spring Boot JDBC Failover (TAF)**:
  * Khai báo JDBC URL dạng Transparent Application Failover (TAF) chứa danh sách IP của cả Primary DB và Standby DB.
  * HikariCP Connection Pool tự động chuyển kết nối sang DB dự phòng trong vài giây nếu DB chính bị ngắt kết nối, đảm bảo dữ liệu `EXPORT_JOB` và `TRANSACTION_LOG` không bị gián đoạn.

### 9.3. Vai trò của Redis (Progress Cache & Rate Limiting)
* **Cache Tiến độ xuất file (Giảm 100% tải Polling lên Oracle DB)**:
  * Worker tiến trình ghi CSV liên tục cập nhật tiến độ (`processed_rows`) vào Redis Key `export:job:{jobId}:progress` trên RAM (~0.1ms).
  * Khi Frontend thực hiện Polling 3s/lần để hiển thị tiến độ cho User, Backend trả dữ liệu trực tiếp từ Redis RAMCache $\rightarrow$ **Triệt tiêu 100% các câu query SELECT trạng thái dội vào Oracle DB**.
* **Chống Spam Request & User Lock (Per-User Rate Limit)**:
  * Sử dụng Redis Key `export:user:{userId}:active_job` kèm TTL.
  * Chặn đứng trong 1ms các thao tác cố tình bấm "Xuất file" liên tục nhiều lần từ 1 user, ngăn chặn tình trạng tạo dư thừa job trùng lặp.

---

## 10. Tổng hợp các Bảng Database cần có (Database Schema Summary)

Hệ thống yêu cầu **2 Bảng Database** chính trên Oracle:

### 10.1. Bảng 1: `TRANSACTION_LOG` (Bảng dữ liệu gốc — Đã có sẵn 1M bản ghi)
```sql
CREATE TABLE TRANSACTION_LOG (
    ID               NUMBER PRIMARY KEY,
    TRANSACTION_CODE VARCHAR2(64) NOT NULL,
    ACCOUNT_NO       VARCHAR2(32) NOT NULL,
    AMOUNT           NUMBER(19, 2) NOT NULL,
    STATUS           VARCHAR2(20) NOT NULL, -- SUCCESS, FAILED, PENDING
    CREATED_AT       TIMESTAMP NOT NULL
);

-- Index tối ưu truy vấn tìm kiếm & phân trang
CREATE INDEX IDX_TXN_LOG_FILTER ON TRANSACTION_LOG (CREATED_AT, STATUS, ACCOUNT_NO, ID);
```

### 10.2. Bảng 2: `EXPORT_JOB` (Bảng quản lý Vòng đời & Metadata của Export Job — Bảng mới)
```sql
CREATE TABLE EXPORT_JOB (
    JOB_ID            VARCHAR2(64) PRIMARY KEY,      -- UUID của Job
    USER_ID           VARCHAR2(100) NOT NULL,        -- Username người tạo job (Ownership check)
    FILTER_CRITERIA   VARCHAR2(2000),                -- JSON chứa bộ lọc (status, accountNo, dates). 2000 ký tụ dư dả cho các bộ lọc hiện tại; nếu sau này thêm range AMOUNT hoặc nhiều trường hơn, cần benchmark lại độ dài JSON.
    STATUS            VARCHAR2(20) NOT NULL,         -- PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED
    PROCESSED_ROWS    NUMBER DEFAULT 0,              -- Số dòng đã ghi thành công
    FILE_PATH         VARCHAR2(500),                 -- Đường dẫn file MinIO Object Key (VD: exports/20260914/job_uuid.csv)
    ERROR_MESSAGE     VARCHAR2(1000),                -- Thông báo lỗi chi tiết nếu FAILED
    -- EXPORT_SCN đã bị loại bỏ (xem quyết định mục 3.6: không dùng Flashback AS OF SCN)
    MAX_EXPORT_ID     NUMBER NOT NULL,               -- Snapshot Boundary: MAX(ID) tại t0, batch chỉ đọc WHERE ID <= MAX_EXPORT_ID
    LAST_HEARTBEAT_AT TIMESTAMP DEFAULT SYSTIMESTAMP, -- Thời điểm heartbeat để detect worker crash
    CREATED_AT        TIMESTAMP DEFAULT SYSTIMESTAMP,-- Thời điểm tạo yêu cầu
    COMPLETED_AT      TIMESTAMP,                     -- Thời điểm xuất hoàn thành
    EXPIRES_AT        TIMESTAMP                      -- Thời điểm hết hạn (COMPLETED_AT + 12h)
);

-- Index hỗ trợ tìm kiếm active jobs theo user và quét dọn nợ job
CREATE INDEX IDX_EXPORT_JOB_USER_STATUS ON EXPORT_JOB (USER_ID, STATUS, EXPIRES_AT);
```

---

## 11. Kế hoạch triển khai chi tiết từng bước (Step-by-Step Roadmap)

### 🔹 Bước 1: Database Migration & Indexing (Oracle)
- Tạo Script DDL tạo bảng `EXPORT_JOB` và cấp quyền truy cập ứng dụng.
- Đánh giá Execution Plan và bổ sung Composite Index `IDX_TXN_LOG_FILTER` trên bảng `TRANSACTION_LOG`.
- Bổ sung Index phụ `(ACCOUNT_NO, CREATED_AT, ID)` trên `TRANSACTION_LOG` để tối ưu query filter theo tài khoản kèm ORDER BY ID (keyset pagination).
- _(Best practice chung)_ DBA đảm bảo Undo Tablespace `AUTOEXTEND ON` để tránh Undo Tablespace đầy đột ngột trong các transaction lớn — **không phải yêu cầu riêng cho export**, vì module này không dùng Flashback Query (xem mục 3.6).

### 🔹 Bước 2: Backend Core & File Storage (Spring Boot)
- Khai báo Entities `TransactionLog` và `ExportJob` cùng Repositories tương ứng.
- Viết `FileStorageService` Interface + `MinioStorageServiceImpl` (MinIO Object Storage) + `LocalStorageServiceImpl` (Fallback local).
- Cấu hình **MinIO Bucket Lifecycle Expiration Rule (12 Hours)** tự động xóa file tạm.

### 🔹 Bước 3: Backend Worker, CSV Protection & Recovery (Spring Boot)
- Viết `ExportJobService` — **thứ tự kiểm tra khi tạo job** (quan trọng, dev cần follow đúng trình tự này):
  1. Kiểm tra **Redis User Lock** (`export:user:{userId}:active_job`) — từ chối người dùng này nếu đã có job đang chạy (HTTP 409)
  2. Kiểm tra **Global ThreadPool capacity** (queue đầy chưa) — từ chối toàn hệ thống nếu queue đầy (HTTP 503)
  3. Lấy `MAX(ID)` tại $t_0$ → lưu vào `MAX_EXPORT_ID`, submit `@Async` task, trả về HTTP 202 Accepted (< 100ms)
  
  _Ý nghĩa: kiểm tra user-lock trước (rẻ hơn, từ Redis) giúp loại sớm 80% yêu cầu không hợp lệ trước khi cần kiểm tra global capacity. Nếu đảo ngược: một user có thể bị cập lock Redis vô ích khi hệ thống đầy._
- Viết `@Async` `ExportWorkerService`: Fetch batch 10.000 dòng bằng JDBC Template với **READ COMMITTED thông thường** — câu query mỗi batch:
  ```sql
  SELECT ... FROM TRANSACTION_LOG
  WHERE ID <= :maxExportId  -- Snapshot boundary: không chứa bản ghi mới sau t0
    AND ID > :lastCursorId   -- Keyset cursor: tránh OFFSET
    AND <filter_criteria>    -- Bộ lọc từ FILTER_CRITERIA
  ORDER BY ID               -- Đảm bảo keyset deterministic
  FETCH FIRST 10000 ROWS ONLY
  ```
  _(Không dùng `AS OF SCN`, không cần Undo Retention đặc biệt, không có rủi ro ORA-01555)_
  - Sử dụng `Apache Commons CSV` xử lý RFC 4180 escaping (commas, quotes, newlines) & chống CSV Injection (`'`, `=`, `+`, `@`).
  - Ghi File Temp Local $\rightarrow$ Upload MinIO khi thành công $\rightarrow$ Xóa File Temp trong `finally`.
  - Cập nhật tiến độ `processed_rows` vào Redis theo từng Batch. Cập nhật `COMPLETED_AT` và `EXPIRES_AT = COMPLETED_AT + 12h`.
- Viết `ExportRecoveryScheduler`: Quét phát hiện Stuck Job (`LAST_HEARTBEAT_AT > 5 phút` hoặc `PENDING > 5m`) $\rightarrow$ Chuyển `FAILED`. Bảo vệ bằng **ShedLock** (`@SchedulerLock` + Redis) để chỉ 1 instance chạy Scheduler tại một thời điểm, tránh race condition khi scale multi-instance.

### 🔹 Bước 4: Backend REST Controllers & Security Guard
- Kiểm tra phân quyền: Yêu cầu Permission `TXN_LOG_EXPORT` cho API tạo job export và `TXN_LOG_VIEW` cho API search list.
- Kiểm tra Ownership (Anti-IDOR): Tất cả API Download & Get Job History bắt buộc xác thực `currentUser.username == job.userId`.
- `POST /api/v1/transaction-log/export-jobs`: Khởi tạo Job (xác nhận < 100ms).
- `GET /api/v1/transaction-log/export-jobs/active`: Polling lấy tiến độ từ Redis RAMCache.
- `GET /api/v1/transaction-log/export-jobs/my-jobs`: Danh sách jobs 12h cho Header Notification Bell.
- `GET /api/v1/transaction-log/export-jobs/{jobId}/download`: Trả về **MinIO Presigned URL** (có thời hạn bằng `EXPIRES_AT`) kèm Ownership check. **Không stream file qua Spring Boot** (tốn thread pool, băng thông, dễ timeout với file 150-300MB).
- `GET /api/v1/transaction-log` (Search List): **Lưu ý khi implement phân trang** — tránh dùng `COUNT(*)` để đếm tổng bản ghi/tổng trang trên tập filter lớn (1M+ dòng có thể chậm). Nên dùng keyset cursor (hiển thị "có trang tiếp" thay vì tổng số trang). Nếu vẫn cần tổng số bản ghi: đơn hàng hóa riêng 1 query COUNT, benchmark thực tế trước khi đưa lên production.

### 🔹 Bước 5: Frontend List Component (`src/app/features/transaction-log/`)
- Tạo `TransactionLogListComponent` kế thừa `BaseListComponent<TransactionLogResponse, number>`.
- Xây dựng Reactive Form Tìm kiếm (`searchForm`), Bảng hiển thị kết quả, Phân trang Keyset cho Export.
- Tích hợp Polling progress 3s/lần và Toast thông báo tiến độ.

### 🔹 Bước 6: Frontend Header Notification Center Integration (`src/app/layout/header/`)
- Tích hợp Signal `notificationCount()` trên `HeaderComponent`.
- Xây dựng Popover Lịch sử Export khi click Quả chuông (`<tui-icon icon="@tui.bell">`).
- Hiển thị danh sách file export completed trong 12h kèm **bộ đếm ngược thời gian lưu trữ local** (`EXPIRES_AT - Date.now()`) và nút `[Tải về]`.

### 🔹 Bước 7: Kiểm thử & Nghiệm thu (Verification & End-to-End Testing)
- Kiểm thử xuất 1.000.000 bản ghi: Xác nhận HTTP 202 < 100ms, file CSV UTF-8 BOM tiếng Việt không lỗi font, mở được bằng Excel, RFC 4180 chuẩn.
- Kiểm thử `MAX_EXPORT_ID`: Insert bản ghi mới sau khi Job bắt đầu $\rightarrow$ Xác nhận file CSV không chứa bản ghi đó.
- Kiểm thử tính nhất quán: UPDATE bản ghi giữa chừng $\rightarrow$ Xác nhận file CSV phản ánh giá trị tại thời điểm đọc batch (READ COMMITTED, không bị đóng băng tại $t_0$).
- Kiểm thử Backpressure: Gửi đồng thời > 3 job export từ nhiều user $\rightarrow$ Xác nhận đúng số job bị reject với HTTP 503.
- Kiểm thử Global Rate Limit: 100 request tạo job đồng thời $\rightarrow$ Chỉ tối đa 13 job được chấp nhận (3 chạy + 10 queue), còn lại nhận 503.
- Kiểm thử Bảo mật: Giả lập IDOR thay đổi `jobId` của user khác $\rightarrow$ Trả về `403 Forbidden`.
- Kiểm thử giả lập Worker crash: Kill tiến trình giữa chừng $\rightarrow$ Scheduler chuyển job về `FAILED` và dọn dẹp file temp local.