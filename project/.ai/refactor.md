Hãy refactor CHÍNH XÁC 2 component tôi đang cung cấp:

* `ComponentListComponent`
* `CategoryListComponent`

Mục tiêu là sửa các vấn đề clean code mà bạn phát hiện trong 2 component này, nhưng PHẢI giữ nguyên toàn bộ behavior hiện tại của hệ thống.

## Các vấn đề BẮT BUỘC phải xử lý

### 1. Loại bỏ `any` không cần thiết

Trong 2 component, tìm và thay thế các `any` có thể xác định được type.

Đặc biệt kiểm tra:

* `openEditDialog(item: any)`
* `openCopyDialog(item: any)`
* `onViewDetail(item: any)`
* `isSelectable(item: any)`
* `trackByHistoryId(index: number, item: any)`
* `trackByColId(index: number, col: any)`
* các callback/function parameter đang dùng `any`
* các biến response/result/filter đang dùng `any`

Ưu tiên sử dụng các model/interface ĐÃ CÓ trong project.

Ví dụ:

`ComponentListComponent` sử dụng `ProcessingComponentResponse`.

`CategoryListComponent` sử dụng `GroupCategoryResponse`.

KHÔNG tạo interface mới nếu project đã có type tương ứng.

Không được thay `any` bằng một type giả chỉ để compiler hết lỗi.

---

### 2. Sửa việc inject `FormBuilder` bị duplicate

Hiện tại đang có pattern kiểu:

```ts
private fb = inject(FormBuilder);

searchForm: FormGroup = inject(FormBuilder).group(...)
```

Chỉ inject `FormBuilder` MỘT LẦN.

Dùng:

```ts
private readonly fb = inject(FormBuilder);

searchForm = this.fb.group({
   ...
});
```

Hoặc nếu phù hợp với coding style hiện tại:

```ts
searchForm = inject(FormBuilder).group({
   ...
});
```

Không được inject cùng dependency hai lần.

---

### 3. Xóa constructor rỗng

Nếu component có:

```ts
constructor() {
}
```

và không có logic trong constructor thì xóa.

Project hiện tại đã sử dụng Angular `inject()` nên không cần constructor rỗng.

---

### 4. Refactor `onConfirmExecute()`

Đây là phần cần ưu tiên cao.

Hiện tại `onConfirmExecute()` đang chứa nhiều `if/else` cho:

* delete
* sendApproval
* cancelApproval
* batchApprove
* các action tương tự

và mỗi nhánh lại có logic:

* gọi service
* subscribe
* success notification
* error notification
* reload data
* loading state

Hãy refactor để giảm duplication.

Có thể tách thành các method có responsibility rõ ràng, ví dụ:

```ts
executeDelete(...)
executeSendApproval(...)
executeCancelApproval(...)
executeBatchApprove(...)
handleActionSuccess(...)
handleActionError(...)
```

hoặc một abstraction tương đương nếu phù hợp với codebase.

KHÔNG tạo abstraction quá phức tạp chỉ để giảm vài dòng.

Quan trọng:

* Không thay đổi service method.
* Không thay đổi request.
* Không thay đổi API.
* Không thay đổi notification.
* Không thay đổi loading behavior.
* Không thay đổi `loadData()`.
* Không thay đổi confirm dialog behavior.

Chỉ làm code dễ đọc và ít duplicate hơn.

---

### 5. Gộp logic duplicate giữa `onBatchApprove()` và `onBatchReject()`

Hai method hiện đang lặp logic:

1. lấy danh sách hiện tại
2. lấy selected items
3. kiểm tra selected items
4. kiểm tra item selectable
5. kiểm tra trường hợp tất cả item đều không selectable
6. hiển thị notification
7. mở confirm dialog

Hãy extract phần logic CHUNG thành method riêng.

Ví dụ có thể có:

```ts
private validateBatchSelection(...)
```

hoặc tên phù hợp hơn với codebase.

Method này nên chịu trách nhiệm kiểm tra selection.

`onBatchApprove()` và `onBatchReject()` chỉ nên xử lý phần khác nhau của từng action.

KHÔNG thay đổi rule hiện tại của việc item nào được approve/reject.

---

### 6. Refactor `loadData()`

`loadData()` hiện đang làm quá nhiều việc:

* save state
* set loading
* lấy raw filters
* parse filter
* tạo search filters
* tạo sort params
* gọi API
* xử lý success
* xử lý error

Hãy chia thành các method nhỏ nếu thực sự giúp code dễ đọc.

Ưu tiên cấu trúc kiểu:

```ts
loadData(): void
buildSearchFilters(): ...
buildSortParams(): ...
handleLoadSuccess(...): void
handleLoadError(...): void
```

Không bắt buộc phải dùng đúng tên trên.

Đặc biệt:

### KHÔNG thay đổi logic filter.

Ví dụ các field:

* componentCode
* componentName
* status
* isActive

phải tạo request giống hệt hiện tại.

Không thay đổi format request gửi backend.

---

### 7. Tách logic parse array number bị duplicate

Hiện tại logic parse filter cho `status` và `isActive` có dạng gần giống nhau:

```ts
Array.isArray(...)
.map(...)
.filter(...)
```

Hãy kiểm tra project xem trong `shared`, `utils`, `common` đã có utility xử lý việc này chưa.

Nếu ĐÃ CÓ thì REUSE.

Nếu CHƯA CÓ và logic này thực sự dùng chung, có thể tạo một utility nhỏ, ví dụ:

```ts
toNumberArray(value: unknown): number[]
```

Utility phải generic và không phụ thuộc vào component cụ thể.

Ví dụ:

```ts
private toNumberArray(value: unknown): number[] {
   ...
}
```

hoặc shared utility nếu logic được dùng ở nhiều nơi.

Không tạo utility nếu chỉ được dùng một lần và làm code khó hiểu hơn.

---

### 8. Không tạo object map mới trong `stringifyStatus()` mỗi lần gọi

Hiện tại `stringifyStatus()` tạo `labels` và status map trong mỗi lần gọi.

Hãy refactor để tránh việc tạo map không cần thiết khi method được gọi nhiều lần từ template/table.

Nhưng phải lưu ý:

`LanguageService.labels()` có thể thay đổi theo language.

Vì vậy KHÔNG được cache static một lần nếu điều đó làm label không cập nhật khi language thay đổi.

Hãy sử dụng pattern phù hợp với cách `LanguageService` hiện tại đang hoạt động.

Mục tiêu:

* không tạo map dư thừa
* vẫn cập nhật đúng khi language thay đổi
* không thay đổi text hiển thị

---

### 9. Kiểm tra `statusCodes` và `activeCodes`

Hiện tại có các magic string như:

```ts
['1', '3', '4', '5', '7']
['1', '0']
```

Kiểm tra project xem đã có:

* enum
* constants
* status definitions

hay chưa.

Nếu đã có thì REUSE.

Nếu chưa có thì chỉ tạo constant/enum nếu nó thực sự giúp code rõ hơn.

Không tạo duplicate enum/constant.

Không thay đổi giá trị hiện tại.

---

### 10. Refactor `displayColumns`

Hiện tại `displayColumns` có getter thực hiện permission check:

```ts
this.authService.hasPermission(...)
```

Mỗi lần Angular evaluate getter có thể thực hiện permission check lại.

Hãy xem xét tính toán permission ở state phù hợp, ví dụ:

```ts
readonly canApprove = ...
```

sau đó `displayColumns` chỉ dựa trên state đó.

NHƯNG phải đảm bảo permission vẫn phản ánh đúng behavior hiện tại.

Nếu `hasPermission()` có khả năng thay đổi runtime/reactive thì phải dùng cách phù hợp với architecture hiện tại.

Không cache permission nếu việc đó có thể làm permission bị stale.

---

### 11. Tạo type cho table column

Hiện tại column object đang bị suy luận type lỏng và các method như:

```ts
onResizeStart(event: MouseEvent, col: any)
trackByColId(index: number, col: any)
```

đang sử dụng `any`.

Kiểm tra project xem đã có `Column`/`TableColumn` interface chưa.

Nếu có thì REUSE.

Nếu chưa có, tạo một interface nhỏ phù hợp:

```ts
interface TableColumn {
   id: string;
   label: string;
   isFixed?: boolean;
   width?: number;
   ...
}
```

Chỉ khai báo những property thực sự đang sử dụng.

Sau đó dùng type này cho:

* `columns`
* `onResizeStart`
* `trackByColId`
* các method liên quan đến column

Không thay đổi cấu trúc column hiện tại.

---

### 12. Type cho `getHistoryFn`

Không dùng:

```ts
(code: any, ...)
(id: any, ...)
```

Dùng type đúng với API hiện tại.

Ví dụ:

`ComponentListComponent` dùng type của `componentCode`.

`CategoryListComponent` dùng type của category ID.

Không thay đổi API service.

---

### 13. Refactor `toggleSort()` của CategoryListComponent

`CategoryListComponent` có logic riêng cho trường hợp:

* JPA/server-side sorting
* native/joined data sorting

Phần native sorting hiện khá dài.

Hãy tách phần sorting local thành method riêng, ví dụ:

```ts
sortJoinedCategories(...)
```

và nếu cần:

```ts
compareValues(...)
```

Mục tiêu là `toggleSort()` chỉ điều phối.

KHÔNG thay đổi:

* field sorting
* direction
* null handling
* string comparison
* number comparison
* date fallback
* pagination behavior
* server-side sorting behavior

Kết quả sort phải giống hệt trước refactor.

---

### 14. Refactor column resize nếu đã có shared utility/directive

Hai component đang có logic `onResizeStart()` rất giống nhau.

TRƯỚC KHI viết code mới:

Hãy search toàn bộ project trong:

```text
shared
utils
common
directives
```

xem đã có logic:

* resize column
* drag column
* reorder column
* table column resize

hay chưa.

Nếu đã có → REUSE.

Nếu chưa có → KHÔNG nhất thiết phải tạo abstraction lớn.

Chỉ extract thành shared directive/service nếu logic này thực sự dùng chung hoặc hai component đang duplicate rõ ràng.

Không thay đổi behavior resize/reorder hiện tại.

---

# 15. Giữ nguyên những phần đang tốt

Không refactor chỉ để "cho khác".

Các phần đã có utility dùng chung như:

```ts
computeNextSort
reorderTableColumns
isItemSelectable
isAllItemsSelected
toggleAllSelection
```

hoặc các utility tương tự:

→ ưu tiên tiếp tục sử dụng.

Không viết lại logic tương đương.

---

# 16. KHÔNG làm những việc sau

Tuyệt đối KHÔNG:

* đổi API endpoint
* đổi service contract
* đổi model backend
* đổi request payload
* đổi response mapping
* đổi permission key
* đổi route
* đổi localStorage key
* đổi form control name
* đổi template behavior
* đổi CSS
* đổi UI
* đổi business rule
* đổi validation rule
* đổi status code
* đổi approval/reject rule
* đổi pagination
* đổi sorting result
* đổi export format

Không migrate framework/library.

Không chuyển sang NgRx.

Không tạo BaseComponent generic nếu chưa cần.

Không áp dụng Clean Architecture toàn project.

Không refactor những file ngoài phạm vi nếu không cần thiết.

---

# 17. Nguyên tắc khi tạo file mới

Trước khi tạo bất kỳ:

```text
*.util.ts
*.utils.ts
*.helper.ts
*.service.ts
*.directive.ts
*.interface.ts
```

phải search project trước.

Nếu chức năng tương tự đã tồn tại:

> REUSE FILE HIỆN TẠI.

Chỉ tạo file mới khi thực sự không có nơi phù hợp.

---

# 18. Validation bắt buộc

Sau khi refactor:

1. Kiểm tra TypeScript compile.
2. Chạy lint nếu project có.
3. Chạy test nếu project có.
4. Chạy build production/dev phù hợp.

Nếu có lỗi build/lint/test do refactor:

> Phải sửa trước khi hoàn thành.

Không được để code ở trạng thái build fail.

---

# 19. Cuối cùng báo cáo chính xác

Sau khi hoàn thành, chỉ báo cáo:

### Changed

Các file đã thay đổi.

### Reused

Các utility/shared/service có sẵn đã được reuse.

### Refactored

Liệt kê:

* removed unnecessary `any`
* fixed duplicate FormBuilder injection
* removed empty constructor
* refactored `onConfirmExecute`
* extracted batch selection validation
* refactored `loadData`
* optimized status mapping
* typed table columns
* refactored local sorting
* các thay đổi tương ứng thực tế

### Validation

```text
Build: PASS/FAIL
Lint: PASS/FAIL
Tests: PASS/FAIL
```

### Behavior

Xác nhận:

> Behavior trước và sau refactor được giữ nguyên.

Nếu có bất kỳ thay đổi behavior nào bắt buộc phải thực hiện, phải nói rõ chính xác thay đổi đó và lý do.

## Quan trọng

Đây là REFACTORING, không phải viết lại chức năng.

Hãy ưu tiên:

**existing architecture → existing shared/utils → existing models → minimal new abstraction → minimal code change.**

Không cố làm code "đẹp" bằng cách tạo quá nhiều layer.

Bắt đầu bằng việc phân tích chính xác 2 component và các shared/utils liên quan, sau đó mới chỉnh sửa.
