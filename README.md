# ĐÁNH GIÁ KẾT QUẢ PHỎNG VẤN DỰ ÁN (TECH LEAD REVIEW)

Chào bạn, tôi đã đọc file và ghi nhận các câu trả lời của bạn. Dưới đây là phần đánh giá, phản hồi chi tiết (Code Review) và giải nghĩa cặn kẽ từng câu hỏi để bạn dễ dàng nắm bắt kiến thức cốt lõi của dự án nhé!

---

## 📝 ĐÁNH GIÁ CHI TIẾT TỪNG CÂU HỎI

### ❓ Câu 1: Cơ chế Maker-Checker & Lưu trữ tạm thời
> Trong dự án này, cơ chế duyệt hai bước (Maker-Checker) khi chỉnh sửa dữ liệu đã duyệt được xử lý như thế nào? Khi người nhập (Maker) sửa một tham số đã phê duyệt thành công trước đó (`STATUS = 4`), dữ liệu thay đổi mới và dữ liệu cũ được lưu trữ ra sao dưới Database Oracle?

* **Câu trả lời của bạn:** 
  > *khi sửa dữ liệu đã duyệt, hệ thống sẽ tạo ra 1 bản mới và lưu dữ liệu đang chờ duyệt mới vào đó, sau khi được duyệt thì sẽ ghi đè dữ liệu từ bảng mới đó vào dữ liệu cũ*
* **💬 Tech Lead Nhận xét (7/10 điểm - Hiểu đúng ý tưởng chính):**
  * **Điểm tốt:** Bạn đã hiểu đúng luồng đi của dữ liệu là hệ thống "tạo ra bản mới để lưu tạm" và "sau khi duyệt thì ghi đè vào dữ liệu cũ".
  * **Điểm cần làm rõ:** Thực tế hệ thống **không tạo ra dòng mới (row) hay bảng mới (table) nào cả**. Hệ thống dùng **chính dòng hiện tại** của bảng `PMH_GROUP_CATEGORY`, sau đó chuyển toàn bộ các trường thay đổi mới thành một chuỗi **JSON** (ví dụ: `{"paramName": "Tên mới", "isActive": 0}`) và lưu vào cột **`NEW_DATA`** của chính dòng đó, đồng thời đổi trạng thái dòng đó về `3 - Chờ duyệt`.
  * **Khi phê duyệt:** Backend sẽ giải mã (parse) chuỗi JSON trong cột `NEW_DATA` ra, cập nhật đè các giá trị mới này vào các cột chính (cột thật), rồi xóa cột `NEW_DATA` về `null` và đặt `STATUS = 4` (Đã duyệt).

---

### ❓ Câu 2: Quản lý Giao dịch Backend (Spring Boot Transactions)
> Trong các phương thức phê duyệt hàng loạt như `batchApprove` tại [GroupCategoryServiceImpl.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/service/impl/GroupCategoryServiceImpl.java), tại sao hệ thống lại sử dụng `TransactionTemplate` với cấu hình propagation là `TransactionDefinition.PROPAGATION_REQUIRES_NEW` cho từng ID thay vì dùng `@Transactional` mặc định của Spring ở mức phương thức? Thiết kế này giải quyết rủi ro nghiệp vụ nào?

* **Câu trả lời của bạn:**
  > *không biết*
* **💬 Tech Lead Giải thích (0/10 điểm - Cần học lại khái niệm Transaction):**
  * **Vấn đề:** Nếu ta dùng `@Transactional` ở mức phương thức, Spring sẽ gộp toàn bộ việc duyệt của danh sách (ví dụ Checker duyệt 10 bản ghi cùng lúc) vào **1 Transaction duy nhất**. Nếu bản ghi thứ 5 bị lỗi (lỗi nghiệp vụ, lỗi DB...), **toàn bộ 10 bản ghi sẽ bị rollback (hủy bỏ duyệt)**. Điều này gây khó chịu vì 9 bản ghi hợp lệ kia cũng bị hủy theo.
  * **Giải pháp:** Sử dụng `TransactionTemplate` kết hợp `PROPAGATION_REQUIRES_NEW` chạy trong vòng lặp `for` giúp mỗi bản ghi được duyệt trong **một giao dịch độc lập hoàn toàn**. Bản ghi nào duyệt lỗi thì chỉ rollback đúng bản ghi đó và tiếp tục duyệt các bản ghi khác trong danh sách.

---

### ❓ Câu 3: Gọi Oracle Stored Procedure từ Spring Data JPA
> Stored Procedure (`PROC_APPROVE_GROUP_CATEGORY`) đóng vai trò gì trong nghiệp vụ phê duyệt? Làm thế nào để tầng Repository trong Spring Boot có thể liên kết và kích hoạt Stored Procedure này trực tiếp từ mã nguồn Java? Hãy chỉ ra file định nghĩa liên kết đó.

* **Câu trả lời của bạn:**
  > *không biết*
* **💬 Tech Lead Giải thích (0/10 điểm - Cần học cách JPA gọi Stored Procedure):**
  * **Vai trò:** Stored Procedure thực hiện cập nhật trực tiếp trong DB Oracle (set `STATUS = 4`, `IS_DISPLAY = 2`) để đảm bảo an toàn nghiệp vụ ở mức tối đa (nhân database tự kiểm soát).
  * **Cách Java gọi:** Spring Boot hỗ trợ annotation `@Procedure` khai báo trên interface của Repository. Bạn hãy xem tại [GroupCategoryRepository.java:L18-24](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/repository/GroupCategoryRepository.java#L18-L24):
    ```java
    @Procedure(name = "PROC_APPROVE_GROUP_CATEGORY")
    void approveGroupCategory(
        @Param("p_id") Long id,
        @Param("p_user") String user,
        @Param("p_status") Long[] status,
        @Param("p_message") String[] message
    );
    ```
    Khi Java gọi hàm `approveGroupCategory(...)`, Spring Data JPA sẽ tự động gọi Stored Procedure tương ứng dưới DB Oracle.

---

### ❓ Câu 4: Ứng dụng Angular Signals & Phân trang Lịch sử
> Cơ chế Angular **Signals** mới (v17+) được áp dụng thế nào trong chức năng thay đổi ngôn ngữ (`language.service.ts`) và chức năng phân trang Dialog lịch sử thao tác của tham số danh mục (`category-list.ts`)? Ưu điểm của cơ chế này so với việc dùng các biến thông thường hoặc RxJS là gì?

* **Câu trả lời của bạn:**
  > *không biết*
* **💬 Tech Lead Giải thích (0/10 điểm - Điểm cốt lõi của Angular 17+):**
  * **Cách áp dụng:**
    * Ở `language.service.ts`: Biến `labels` được khai báo dưới dạng Signal (`labels = signal(...)`). Khi người dùng đổi ngôn ngữ, Signal phát tín hiệu, toàn bộ giao diện tự động dịch sang tiếng Việt mà không cần load lại trang.
    * Ở `category-list.ts`: Biến `historyData` và `historyPage` là Signals. Ta dùng hàm `computed()` để tính toán mảng con đã phân trang `paginatedHistoryData` tự động cập nhật mỗi khi trang hiện tại hoặc dữ liệu lịch sử thay đổi.
  * **Ưu điểm:** Cực kỳ nhẹ, không cần quản lý việc hủy subscribe (như RxJS) tránh rò rỉ bộ nhớ, và cập nhật giao diện cực kỳ chính xác (chỉ vẽ lại đúng phần DOM thay đổi chứ không render lại cả trang).

---

### ❓ Câu 5: Ràng buộc biểu mẫu bằng Zod Schema Validation
> Tại sao dự án lại tích hợp thư viện **Zod** ở Frontend thay vì sử dụng bộ `Validators` mặc định của Angular? Dự án đã bắc cầu (tạo adapter) để Zod Schema hoạt động ăn khớp với hệ thống Reactive Forms của Angular thông qua các hàm tiện ích nào?

* **Câu trả lời của bạn:**
  > *không biết*
* **💬 Tech Lead Giải thích (0/10 điểm - Cách tối ưu hóa validate trong Angular):**
  * **Tại sao dùng Zod:** Zod giúp viết quy tắc validate cực kỳ ngắn gọn dạng chuỗi nối tiếp (chaining), dễ đọc và tự động suy luận ra kiểu TypeScript (`z.infer`) giúp đồng bộ Model và Rule. Validators mặc định của Angular rất rườm rà và khó viết các luật validate phức tạp (như ngày hết hạn phải sau ngày hiệu lực).
  * **Hàm cầu nối:** Được định nghĩa tại [component.schema.ts:L126-167](file:///e:/PMH/code/frontend/src/app/shared/validators/component.schema.ts#L126-L167):
    * `zodFieldValidator(schema, field)`: Lấy schema Zod validate cho 1 ô nhập liệu đơn lẻ.
    * `zodFormValidator(schema)`: Validate cho toàn bộ Form Group (dùng để so sánh chéo 2 trường ngày tháng).

---

## 🏆 ĐÁNH GIÁ TỔNG QUAN

* **Tổng điểm:** **1.5 / 5** (Hoặc **3.0 / 10** theo thang điểm 10).
* **Nhận xét của Tech Lead:** 
  > Bạn đã nắm được tư duy logic nghiệp vụ Maker-Checker ở Câu 1 (đây là nghiệp vụ khó nhất của dự án!). Các câu hỏi kỹ thuật về Spring Boot và Angular còn lại do bạn là người mới nên chưa nắm rõ là điều hoàn toàn bình thường.
  > 
  > **Lời khuyên học tập:** Hãy mở trực tiếp các đường link file code tôi đính kèm ở các câu hỏi trên để xem cách dự án triển khai thực tế. Việc đọc hiểu các đoạn code mẫu này sẽ giúp bạn nâng cao trình độ rất nhanh!
