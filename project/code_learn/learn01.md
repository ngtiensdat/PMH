# FILE HỌC CODE THỰC CHIẾN - PHẦN 1 (LEARN01)

Chào mừng bạn đến với phương pháp học tập chủ động qua phản xạ code! 
Dưới đây là một đoạn code thực tế từ giao diện Frontend của dự án. Nhiệm vụ của bạn là đọc code, phân tích và trả lời các câu hỏi bên dưới để rèn luyện tư duy đọc hiểu dự án.

---

## 💻 Đoạn code: Khởi tạo Form trong Angular (Reactive Forms)

Đoạn code này nằm trong file [component-dialog.ts](file:///e:/PMH/code/frontend/src/app/features/processing-components/components/component-dialog/component-dialog.ts).

```typescript
  private initForm() {
    this.dialogForm = this.fb.group(
      {
        componentCode:    ['', zodFieldValidator(ComponentSchema, 'componentCode')],
        componentName:    ['', zodFieldValidator(ComponentSchema, 'componentName')],
        messageType:      [[], zodFieldValidator(ComponentSchema, 'messageType')],
        connectionMethod: [[], zodFieldValidator(ComponentSchema, 'connectionMethod')],
        checkToken:       [false, zodFieldValidator(ComponentSchema, 'checkToken')],
        description:      ['', zodFieldValidator(ComponentSchema, 'description')],
        isActive:         [1,  zodFieldValidator(ComponentSchema, 'isActive')],
        effectiveDate:    ['', zodFieldValidator(ComponentSchema, 'effectiveDate')],
        endEffectiveDate: ['', zodFieldValidator(ComponentSchema, 'endEffectiveDate')]
      },
      { validators: zodFormValidator(ComponentSchema) }
    );
  }
```

---

## ❓ Câu hỏi dành cho bạn:

### 1. Phân tích tham số khởi tạo:
*   Giá trị mặc định của trường `checkToken` là gì? Khi hiển thị lên giao diện HTML, theo bạn trường này sẽ dùng loại Input nào (textbox, select, checkbox, hay radio button)?
*   Giá trị mặc định của trường `isActive` là gì?

### 2. Ý nghĩa của `zodFieldValidator` và `zodFormValidator`:
*   Hai hàm này có vai trò gì trong việc kiểm soát dữ liệu người dùng nhập? 
*   Nếu người dùng nhập sai định dạng (ví dụ: `componentCode` chứa khoảng trắng), điều gì sẽ xảy ra trước khi dữ liệu được gửi lên server?

### 3. Phân biệt `[]` và `''`:
*   Tại sao `messageType` được khởi tạo là `[[]]` (mảng rỗng bên trong mảng cấu hình) còn `componentCode` được khởi tạo là `['']` (chuỗi rỗng)? Cú pháp này đại diện cho kiểu chọn đơn hay chọn nhiều trên giao diện?

---

## ✍️ Phần trả lời của bạn:
1. giá trị mặc định là false, hiện là checkbox
2. `zodFieldValidator` và `zodFormValidator` là 2 hàm dùng để xác minh dữ liệu đầu vào ở fe, nếu nhập sai quy định thì dữ liệu được chặn lại trước khi gửi đến serve
3. mảng rỗng là để chọn nhiều lựa chọn còn chuỗi là để nhập các kỹ tự

