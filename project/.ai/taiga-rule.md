# Quy tắc Tích hợp Giao diện Taiga UI (Taiga Rule)
## 1. Quản lý Imports tập trung
- Bắt buộc import các component giao diện thông qua module dùng chung **[SharedTaigaModule](file:///e:/PMH/project/docs/RSD/SharedTaigaModule)** để tránh lặp đi lặp lại hàng chục dòng import thư viện tại mỗi component.
- Không tự ý import các module lõi của Taiga UI vào component nếu nó đã được hỗ trợ trong SharedTaigaModule.

## 2. Định cấu hình Dropdown Select đa ngôn ngữ
- Khi sử dụng thẻ chọn `tuiSelect` kết hợp với `tui-data-list-wrapper`, bắt buộc lưu giá trị thô (code/ID như `''`, `'1'`, `'3'`) trong FormControl thay vì lưu chuỗi nhãn hiển thị tiếng Việt cứng.
- Sử dụng directive `[stringify]` trên `tui-textfield` để gọi hàm map ngôn ngữ động từ `LanguageService`.
  *Ví dụ:*
  ```html
  <tui-textfield tuiChevron [stringify]="stringifyStatus">
     <input tuiSelect formControlName="status" />
     <tui-data-list-wrapper *tuiDropdown [items]="statusCodes" />
  </tui-textfield>
  ```

## 3. Tùy biến Style (Overriding CSS)
- Tuyệt đối không chỉnh sửa đè CSS trực tiếp vào các class gốc của hệ thống Taiga UI (như `.t-input`, `.t-select`).
- Hãy viết CSS tùy chỉnh ở scope Component bằng các CSS variables hệ thống của Taiga (ví dụ: `--tui-primary`, `--tui-text-01`) hoặc bọc class tùy biến bên ngoài để tránh phá vỡ layout toàn cục.
