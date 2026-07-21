import os

def create_files():
    base_dir = r"e:\PMH\project\Angular"
    os.makedirs(base_dir, exist_ok=True)
    
    files_content = {
        "p2.md": """# PHẦN 2. Angular Core

## 1. Angular là gì?
Angular là một **Platform** và **Framework** mã nguồn mở được phát triển bởi Google nhằm xây dựng các ứng dụng web đơn trang (Single Page Application - SPA) phía client.
*   **SPA:** Chỉ tải một trang HTML duy nhất. Khi người dùng chuyển hướng, Angular tự động vẽ lại các phần của giao diện mà không cần tải lại toàn bộ trang từ server.
*   **Component-based:** Giao diện được chia nhỏ thành các Component (Thành phần) độc lập, tái sử dụng được.

---

## 2. Cấu trúc Project Angular tiêu chuẩn
Một dự án Angular (như phần frontend của dự án hiện tại) có các thư mục/tệp quan trọng:
*   `angular.json`: Tệp cấu hình của Angular CLI (cấu hình assets, styles, scripts...).
*   `package.json`: Khai báo các thư viện phụ thuộc (dependencies) và các lệnh script chạy (`npm start`, `npm run build`).
*   `src/index.html`: Tệp HTML gốc duy nhất chứa thẻ `<app-root></app-root>`.
*   `src/main.ts`: Điểm khởi chạy (Bootstrap) chính của toàn bộ ứng dụng.
*   `src/app/`: Thư mục chứa toàn bộ code nghiệp vụ.
    *   `app.config.ts`: Cấu hình toàn hệ thống (Routing, Providers...).
    *   `app.ts` & `app.html` & `app.css`: Component gốc của ứng dụng.

---

## 3. Angular CLI (Command Line Interface)
Công cụ dòng lệnh giúp tạo, chạy và build ứng dụng Angular nhanh chóng:
*   `ng serve`: Chạy server phát triển cục bộ (local dev server). Mặc định chạy tại port `4200`.
*   `ng generate component <name>` (hoặc `ng g c <name>`): Tạo một component mới tự động kèm đầy đủ các tệp `.ts`, `.html`, `.css`.
*   `ng generate service <name>` (hoặc `ng g s <name>`): Tạo một service mới để xử lý logic/gọi API.
*   `ng build`: Biên dịch mã nguồn thành HTML/JS/CSS tĩnh sẵn sàng đưa lên môi trường Production.

---

## 4. Cấu trúc của một Component
Một Component bao gồm 3 tệp cơ bản:
1.  **Logic (TypeScript - `.ts`):** Xử lý luồng dữ liệu.
2.  **Giao diện (HTML - `.html`):** Định nghĩa cấu trúc giao diện hiển thị.
3.  **Định dạng (CSS - `.css` / `.less`):** Định dạng riêng cho component đó.

### Ví dụ về Standalone Component hiện đại:
```typescript
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-hello',        // Tên thẻ HTML đại diện cho component này
  standalone: true,             // Component độc lập
  imports: [CommonModule],      // Nhập các module phụ trợ
  templateUrl: './hello.html',  // Đường dẫn tới file giao diện
  styleUrl: './hello.css'       // Đường dẫn tới file CSS
})
export class HelloComponent implements OnInit {
  title: string = 'Chào mừng tới Payment Hub';

  ngOnInit() {
    console.log('Component đã được khởi tạo thành công!');
  }
}
```

---

## 5. Luồng Bootstrap (Khởi chạy) ứng dụng
Khi chạy ứng dụng:
1. Trình duyệt tải tệp `index.html`.
2. Trình duyệt chạy mã JavaScript được biên dịch từ `main.ts`.
3. `main.ts` kích hoạt cấu hình ứng dụng trong `app.config.ts` và khởi tạo component gốc `AppComponent`.
4. Thẻ `<app-root></app-root>` trong `index.html` được thay thế bằng nội dung giao diện của `AppComponent`.
""",
        
        "p3.md": """# PHẦN 3. Template & Data Binding

Template trong Angular thực chất là mã HTML thông thường được bổ sung thêm cú pháp đặc biệt của Angular để liên kết dữ liệu (Data Binding) và điều khiển giao diện.

---

## 1. Các kiểu Data Binding
Angular cung cấp 4 cách liên kết dữ liệu giữa file logic `.ts` và file giao diện `.html`:

### A. Interpolation (Nội suy dữ liệu - Cú pháp `{{ }}`)
Đưa dữ liệu một chiều từ biến trong `.ts` ra hiển thị ở `.html`.
*   *File `.ts`:* `username = 'Nguyễn Văn A';`
*   *File `.html`:* `<p>Xin chào, {{ username }}</p>`

### B. Property Binding (Liên kết thuộc tính - Cú pháp `[property]="value"`)
Truyền giá trị từ `.ts` vào các thuộc tính của thẻ HTML hoặc các component con.
*   *File `.ts`:* `isDisabled = true;`
*   *File `.html`:* `<button [disabled]="isDisabled">Gửi duyệt</button>`

### C. Event Binding (Liên kết sự kiện - Cú pháp `(event)="handler()"`)
Lắng nghe hành động của người dùng trên giao diện để kích hoạt hàm trong `.ts`.
*   *File `.ts`:* `onClick() { console.log('Đã click!'); }`
*   *File `.html`:* `<button (click)="onClick()">Lưu thông tin</button>`

### D. Two-way Data Binding (Liên kết dữ liệu hai chiều - Cú pháp `[(ngModel)]`)
Đồng bộ hóa dữ liệu lập tức giữa giao diện và biến logic (thường dùng trong form).
*   *File `.html`:* `<input [(ngModel)]="username" />` (Khi nhập ô input, biến `username` trong file `.ts` tự thay đổi theo và ngược lại).

---

## 2. Directives (Chỉ thị cấu trúc)
Chỉ thị giúp bạn thay đổi cấu trúc DOM (thêm/bớt thẻ) hoặc thay đổi thuộc tính hiển thị.

### Cú pháp điều khiển luồng hiện đại (Angular 17+)
Angular giới thiệu cú pháp `@` thay thế cho các directive cũ (`*ngIf`, `*ngFor`):

*   **@if (Điều kiện hiển thị):**
    ```html
    @if (isLoading) {
      <div>Đang tải dữ liệu...</div>
    } @else {
      <div>Tải thành công!</div>
    }
    ```
*   **@for (Duyệt danh sách - Yêu cầu từ khóa `track` để tối ưu hiệu năng):**
    ```html
    <ul>
      @for (item of categories; track item.id) {
        <li>{{ item.paramName }}</li>
      } @empty {
        <li>Không có dữ liệu hiển thị</li>
      }
    </ul>
    ```

---

## 3. Pipes (Ống dẫn định dạng)
Pipes dùng để biến đổi định dạng hiển thị dữ liệu trực tiếp trên giao diện mà không làm thay đổi giá trị gốc của biến.
*   `uppercase`: Viết hoa chữ: `{{ name | uppercase }}`
*   `date`: Định dạng ngày: `{{ effectiveDate | date: 'dd/MM/yyyy' }}`
*   `currency`: Định dạng tiền tệ: `{{ price | currency: 'VND' }}`
*   `json`: Chuyển Object thành chuỗi JSON (rất tiện khi debug).
""",
        
        "p4.md": """# PHẦN 4. Component Communication (Truyền thông tin giữa các Component)

Trong Angular, một trang giao diện lớn được chia nhỏ thành nhiều Component cha-con (ví dụ: `CategoryListComponent` chứa các nút bấm, bảng dữ liệu, và các pop-up `CategoryDialogComponent`). Việc truyền dữ liệu qua lại giữa chúng là bắt buộc.

---

## 1. Truyền dữ liệu từ Cha xuống Con (`@Input` hoặc `input()`)
Component cha truyền giá trị vào thuộc tính của Component con.

### Cách truyền thống:
*   **Component con (`child.ts`):**
    ```typescript
    import { Component, Input } from '@angular/core';

    @Component({
      selector: 'app-child',
      standalone: true,
      template: `<p>Dữ liệu nhận từ cha: {{ data }}</p>`
    })
    export class ChildComponent {
      @Input() data: string = '';
    }
    ```
*   **Component cha (`parent.html`):**
    ```html
    <app-child [data]="'Thông tin mật'"></app-child>
    ```

---

## 2. Truyền tín hiệu từ Con lên Cha (`@Output` + `EventEmitter`)
Component con phát một sự kiện (event) để Component cha lắng nghe và xử lý.

*   **Component con (`child.ts`):**
    ```typescript
    import { Component, Output, EventEmitter } from '@angular/core';

    @Component({
      selector: 'app-child',
      standalone: true,
      template: `<button (click)="notifyParent()">Gửi tin lên Cha</button>`
    })
    export class ChildComponent {
      @Output() onNotify = new EventEmitter<string>();

      notifyParent() {
        this.onNotify.emit('Con chào Cha!');
      }
    }
    ```
*   **Component cha (`parent.ts` và `parent.html`):**
    *   *HTML:* Lắng nghe sự kiện bằng cú pháp `(tên_sự_kiện)="hàm($event)"`
        ```html
        <app-child (onNotify)="handleChildNotification($event)"></app-child>
        ```
    *   *TS:*
        ```typescript
        handleChildNotification(msg: string) {
          console.log('Cha nhận được thông điệp:', msg); // "Cha nhận được thông điệp: Con chào Cha!"
        }
        ```

---

## 3. ViewChild (Truy cập trực tiếp Component con)
Cho phép Component cha gọi trực tiếp các hàm hoặc lấy biến công khai của Component con nằm trong template của nó.

*   **Component cha (`parent.ts`):**
    ```typescript
    import { Component, ViewChild } from '@angular/core';
    import { ChildComponent } from './child';

    @Component({ ... })
    export class ParentComponent {
      @ViewChild(ChildComponent) childComp!: ChildComponent;

      triggerChildAction() {
        this.childComp.someChildFunction(); // Gọi trực tiếp hàm của con
      }
    }
    ```

---

## 4. Content Projection (Chèn nội dung động `<ng-content>`)
Giúp thiết kế các Component có tính chất khung (Layout, Card, Modal, Dialog...) bằng cách cho phép chèn mã HTML bất kỳ từ bên ngoài vào trong Component.

*   **Component con (`dialog.html`):**
    ```html
    <div class="dialog-box">
      <h3>Tiêu đề Dialog</h3>
      <div class="content">
        <!-- Nội dung truyền từ cha sẽ được cắm vào đây -->
        <ng-content></ng-content>
      </div>
    </div>
    ```
*   **Component cha sử dụng:**
    ```html
    <app-dialog>
      <p>Đây là nội dung tùy biến của form đăng ký.</p>
    </app-dialog>
    ```
""",
        
        "p5.md": """# PHẦN 5. Dependency Injection (DI) & Services

Dependency Injection (DI) là một mẫu thiết kế (design pattern) được tích hợp sẵn vào hạt nhân của Angular. Nó giúp tách biệt phần **lưu trữ logic/dữ liệu (Service)** ra khỏi phần **hiển thị (Component)**.

---

## 1. Tạo Service
Service là một Class bình thường được trang trí bằng `@Injectable`. Thường dùng để:
*   Gọi API từ backend qua HTTP Client.
*   Chia sẻ dữ liệu chung giữa các Component không có quan hệ cha-con trực tiếp.

```typescript
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root' // Khai báo service này tồn tại ở mức toàn ứng dụng (Singleton)
})
export class LoggerService {
  log(msg: string) {
    console.log(`[INFO] ${new Date().toLocaleString()}: ${msg}`);
  }
}
```

---

## 2. Cách Inject Service vào Component
Có hai cách để nhúng Service vào và sử dụng:

### Cách 1: Sử dụng hàm `inject()` (Khuyên dùng trong Angular hiện đại)
*   Cú pháp sạch sẽ, gọn gàng và độc lập với hàm khởi tạo `constructor`.
```typescript
import { Component, OnInit, inject } from '@angular/core';
import { LoggerService } from './logger.service';

@Component({ ... })
export class MyComponent implements OnInit {
  private logger = inject(LoggerService); // Nhúng service trực tiếp

  ngOnInit() {
    this.logger.log('MyComponent đã tải xong!');
  }
}
```

### Cách 2: Sử dụng Constructor Injection (Truyền thống)
```typescript
export class MyComponent implements OnInit {
  constructor(private logger: LoggerService) {}

  ngOnInit() {
    this.logger.log('MyComponent khởi tạo qua Constructor!');
  }
}
```

---

## 3. Quản lý vòng đời (Scope) của Service
*   `providedIn: 'root'`: Tạo ra một bản sao duy nhất (Singleton Pattern) của Service chạy xuyên suốt ứng dụng. Mọi component dùng chung đều đọc/ghi trên cùng một vùng nhớ dữ liệu.
*   **Khai báo tại Component (`providers: [LocalService]`):** Tạo ra một phiên bản Service mới chỉ áp dụng cho component này và các component con của nó. Khi component bị hủy, instance của service cũng bị giải phóng khỏi bộ nhớ.
""",
        
        "p6.md": """# PHẦN 6. Routing (Định tuyến liên kết trang)

Routing giúp người dùng chuyển đổi qua lại giữa các màn hình trong ứng dụng web đơn trang (SPA) mà không cần tải lại trang vật lý từ trình duyệt.

---

## 1. Cấu hình Routes
Bạn định nghĩa danh sách các đường dẫn và Component tương ứng trong tệp `app.routes.ts`.

```typescript
import { Routes } from '@angular/router';
import { CategoryListComponent } from './features/category/components/category-list/category-list';
import { CategoryDetailComponent } from './features/category/components/category-detail/category-detail';

export const routes: Routes = [
  { path: 'categories', component: CategoryListComponent },
  { path: 'categories/detail/:id', component: CategoryDetailComponent }, // Route động nhận tham số id
  { path: '', redirectTo: 'categories', pathMatch: 'full' },            // Trang chủ chuyển hướng mặc định
  { path: '**', redirectTo: 'categories' }                                // Xử lý các đường dẫn không tồn tại
];
```

---

## 2. Hiển thị Route trên giao diện
*   **Thẻ chứa giao diện trang:** `<router-outlet></router-outlet>` (Đặt ở file component gốc [app.html](file:///e:/PMH/code/frontend/src/app/app.html)). Angular sẽ tự động chèn giao diện của Component tương ứng với đường dẫn URL vào thẻ này.
*   **Điều hướng bằng mã HTML:** Sử dụng thuộc tính `routerLink` thay cho thuộc tính `href` thông thường để tránh tải lại trang.
    ```html
    <a routerLink="/categories">Quay về danh sách</a>
    ```

---

## 3. Điều hướng bằng mã TypeScript (Programmatic Routing)
Sử dụng đối tượng `Router` được nhúng vào file logic.

```typescript
import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

@Component({ ... })
export class SampleComponent {
  private router = inject(Router);

  goToDetail(categoryId: number) {
    this.router.navigate(['/categories/detail', categoryId]);
  }
}
```

---

## 4. Lazy Loading (Tải chậm tối ưu hiệu năng)
Thay vì tải toàn bộ code của tất cả các màn hình ngay lần đầu tiên mở web, ta có thể tách mã nguồn và chỉ tải trang nào khi người dùng thực sự kích hoạt truy cập đường dẫn của trang đó.

```typescript
export const routes: Routes = [
  {
    path: 'categories',
    loadComponent: () => import('./features/category/components/category-list/category-list')
      .then(m => m.CategoryListComponent)
  }
];
```

---

## 5. Route Guards (Chốt chặn bảo mật)
Dùng để kiểm tra quyền truy cập của người dùng trước khi vào một trang.
*   `CanActivate`: Kiểm tra xem người dùng đã đăng nhập (AuthGuard) hay chưa, hoặc có quyền Maker/Checker không trước khi mở trang.
*   `CanDeactivate`: Kiểm tra xem người dùng có thay đổi dữ liệu chưa lưu trên form không. Nếu có, hiển thị pop-up cảnh báo xác nhận muốn thoát trang không.
""",
        
        "p7.md": """# PHẦN 7. Forms & Validation

Quản lý dữ liệu biểu mẫu (Form) là thành phần then chốt trong các dự án nghiệp vụ quản lý tham số như Payment Hub. Angular hỗ trợ hai cách làm form: Template-driven Forms và **Reactive Forms**. Chúng ta sẽ tập trung vào Reactive Forms vì nó kiểm soát logic tốt hơn, dễ viết Unit Test và được dự án của bạn sử dụng.

---

## 1. Thiết lập một Reactive Form
Để sử dụng, bạn import `ReactiveFormsModule` vào component.

*   *File `.ts`:*
    ```typescript
    import { Component, inject } from '@angular/core';
    import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';

    @Component({
      standalone: true,
      imports: [ReactiveFormsModule],
      ...
    })
    export class CategoryFormComponent {
      private fb = inject(FormBuilder);

      // Tạo cấu trúc form và các ràng buộc dữ liệu đầu vào
      categoryForm: FormGroup = this.fb.group({
        paramName: ['', [Validators.required, Validators.minLength(3)]],
        paramValue: ['', Validators.required],
        status: [3] // Giá trị mặc định là 3 (Chờ duyệt)
      });

      onSubmit() {
        if (this.categoryForm.valid) {
          console.log('Dữ liệu hợp lệ để gửi API:', this.categoryForm.value);
        } else {
          console.log('Form không hợp lệ!');
        }
      }
    }
    ```

*   *File `.html`:* Liên kết form bằng `[formGroup]` và `formControlName`.
    ```html
    <form [formGroup]="categoryForm" (ngSubmit)="onSubmit()">
      <div>
        <label>Tên tham số:</label>
        <input formControlName="paramName" />
        
        <!-- Hiển thị lỗi nếu người dùng nhập sai quy tắc -->
        @if (categoryForm.get('paramName')?.touched && categoryForm.get('paramName')?.errors?.['required']) {
          <span style="color: red;">Vui lòng nhập tên tham số!</span>
        }
      </div>
      
      <button type="submit" [disabled]="categoryForm.invalid">Lưu thông tin</button>
    </form>
    ```

---

## 2. Dynamic Form (Biểu mẫu động bằng FormArray)
Đối với các biểu mẫu cho phép người dùng ấn nút "Thêm dòng" để nhập nhiều bản ghi cùng lúc, ta sử dụng `FormArray`:
```typescript
this.batchForm = this.fb.group({
  items: this.fb.array([]) // Tạo một mảng Form động
});

// Hàm để lấy mảng ra
get items() {
  return this.batchForm.get('items') as FormArray;
}

// Thêm một dòng nhập liệu mới
addItemRow() {
  this.items.push(this.fb.group({
    paramName: ['', Validators.required],
    paramValue: ['', Validators.required]
  }));
}
```
""",
        
        "p8.md": """# PHẦN 8. HTTP & API Integration

Angular cung cấp dịch vụ `HttpClient` giúp ứng dụng giao tiếp bất đồng bộ với Backend API thông qua các giao thức HTTP (GET, POST, PUT, DELETE).

---

## 1. Thiết lập HttpClient
Khai báo cung cấp HttpClient trong tệp cấu hình ứng dụng `app.config.ts` bằng `provideHttpClient()`.

Sau đó, sử dụng Service để bọc logic gọi API:
```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GroupCategoryResponse } from '../models/group-category.model';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/categories';

  // Lấy danh sách danh mục (GET)
  getCategories(): Observable<GroupCategoryResponse[]> {
    return this.http.get<GroupCategoryResponse[]>(this.apiUrl);
  }

  // Thêm mới danh mục (POST)
  createCategory(data: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, data);
  }

  // Cập nhật danh mục (PUT)
  updateCategory(id: number, data: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, data);
  }
}
```

---

## 2. HTTP Interceptors (Bộ lọc trung gian)
Interceptor giống như một cửa ải lọc toàn bộ các Request đi ra và Response đi về. Rất hữu ích cho:
*   Tự động đính kèm mã bảo mật token vào header `Authorization: Bearer <token>`.
*   Bắt lỗi hệ thống tập trung (như lỗi mất mạng, lỗi `401 Unauthorized` -> chuyển hướng về trang Login).

```typescript
import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('jwt_token');

  // Nếu có token, tạo một request nhân bản được đính kèm token
  const authReq = token ? req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  }) : req;

  return next(authReq);
};
```
""",
        
        "p9.md": """# PHẦN 9. RxJS & Lập trình phản ứng (Reactive Programming)

RxJS là thư viện cực kỳ mạnh mẽ đi kèm với Angular để xử lý luồng dữ liệu bất đồng bộ dựa trên khái niệm **Observables**.

---

## 1. Các khái niệm nền tảng
*   **Observable (Luồng dữ liệu phát đi):** Giống như một dòng sông chảy phát ra dữ liệu theo dòng thời gian.
*   **Observer (Đối tượng lắng nghe):** Người nhận dữ liệu.
*   **Subscription (Sự đăng ký):** Hành động kích hoạt kết nối giữa Observer và Observable bằng hàm `.subscribe()`.
*   **Subject & BehaviorSubject:** Các lớp đặc biệt vừa có thể phát dữ liệu vừa có thể lắng nghe. `BehaviorSubject` luôn giữ một giá trị khởi tạo và phát lại giá trị mới nhất cho bất kỳ ai vừa đăng ký lắng nghe nó.

---

## 2. Các toán tử (Operators) phổ biến
Ta sử dụng phương thức `.pipe()` để biến đổi, sàng lọc luồng dữ liệu trước khi nhận.

```typescript
import { map, filter } from 'rxjs/operators';
import { of } from 'rxjs';

const numberStream$ = of(1, 2, 3, 4, 5); // Tạo luồng phát số từ 1-5

numberStream$.pipe(
  filter(val => val % 2 !== 0), // Lọc số lẻ: 1, 3, 5
  map(val => val * 10)          // Nhân 10: 10, 30, 50
).subscribe(result => {
  console.log(result); // Kết quả lần lượt in ra: 10, 30, 50
});
```

---

## 3. Các toán tử nâng cao hay dùng trong Dự án
*   **switchMap:** Hủy yêu cầu cũ nếu có yêu cầu mới nhất xuất hiện (rất hữu ích cho ô tìm kiếm tự động - Auto Search khi người dùng gõ phím liên tục).
*   **forkJoin:** Chờ cho toàn bộ các API con chạy xong rồi mới gộp kết quả trả về một thể (tương tự `Promise.all()`).
*   **combineLatest:** Phát ra kết quả mới nhất bất cứ khi nào có một trong các luồng con phát sinh dữ liệu mới.
""",
        
        "p10.md": """# PHẦN 10. Quản lý trạng thái & Angular Signals

Trạng thái (State) là nơi lưu trữ toàn bộ dữ liệu ứng dụng tại một thời điểm (ví dụ: thông tin user đăng nhập, danh sách hàng đang chọn, danh mục đang hiển thị).

---

## 1. Quản lý trạng thái cổ điển bằng Service
Chia sẻ thông tin giữa các trang bằng một Service tập trung dùng chung:
```typescript
@Injectable({ providedIn: 'root' })
export class AppStateService {
  // BehaviorSubject giữ thông tin đăng nhập
  private currentUserSubject = new BehaviorSubject<string>('Khách');
  currentUser$ = this.currentUserSubject.asObservable();

  setCurrentUser(username: string) {
    this.currentUserSubject.next(username);
  }
}
```

---

## 2. Angular Signals (Quản lý trạng thái hiện đại - Bản v16+)
Signals là giải pháp mới của Angular giúp thay thế hoặc đơn giản hóa RxJS cho việc quản lý trạng thái giao diện nội bộ với cú pháp đơn giản hơn nhiều.

### A. Khởi tạo Signal
```typescript
import { signal, computed, effect } from '@angular/core';

// Tạo tín hiệu chứa số đếm ban đầu = 0
count = signal(0);
```

### B. Sử dụng và Cập nhật
```typescript
// Lấy giá trị hiện tại của count
console.log(this.count()); // 0

// Tăng giá trị lên bằng hàm set hoặc update
this.count.set(5);          // count = 5
this.count.update(n => n + 1); // count = 6 (Lấy giá trị cũ + 1)
```

### C. Tính toán tự động bằng `computed`
Tự động tính ra kết quả mới bất cứ khi nào `count` thay đổi:
```typescript
doubleCount = computed(() => this.count() * 2);
```

### D. Tác vụ phụ bằng `effect`
Tự động chạy lại hàm này khi có bất kỳ Signal nào bên trong nó bị thay đổi giá trị (thường dùng để đồng bộ dữ liệu vào `localStorage` hoặc debug):
```typescript
constructor() {
  effect(() => {
    console.log(`Giá trị count hiện tại đã đổi thành: ${this.count()}`);
  });
}
```
""",
        
        "p11.md": """# PHẦN 11. Cơ chế Change Detection (Nhận diện thay đổi)

Change Detection là cơ chế Angular quét qua các Component để kiểm tra xem biến dữ liệu trong code có thay đổi không, từ đó cập nhật lại giao diện tương ứng.

---

## 1. Zone.js là gì?
Mặc định, Angular sử dụng thư viện **Zone.js** để tự động lắng nghe toàn bộ các sự kiện bất đồng bộ xảy ra trong ứng dụng (click chuột, phản hồi từ API, hàm `setTimeout`...). Mỗi khi một sự kiện kết thúc, Zone.js báo hiệu cho Angular thực hiện quét Change Detection từ trên xuống dưới toàn bộ cây component để vẽ lại giao diện.

---

## 2. Các chiến lược (Change Detection Strategies)
Quét toàn bộ ứng dụng mỗi khi có sự kiện nhỏ xảy ra sẽ rất tốn hiệu năng đối với dự án lớn. Do đó Angular cung cấp chiến lược tối ưu:

### A. Chiến lược Default
*   Quét mọi component trên cây giao diện bất cứ khi nào có thay đổi.

### B. Chiến lược OnPush (`ChangeDetectionStrategy.OnPush`)
*   Angular sẽ **bỏ qua** không quét quét component này trừ khi:
    1.  Biến đầu vào của component nhận tham chiếu mới (Đối tượng `@Input` được thay đổi hoàn toàn địa chỉ vùng nhớ chứ không chỉ sửa trường bên trong).
    2.  Có sự kiện trực tiếp kích hoạt bên trong component này (ví dụ người dùng click vào nút của component).
    3.  Lập trình viên gọi hàm ép quét bằng tay: `this.cdr.markForCheck()`.
    4.  Sử dụng Observable với async pipe ở file html.
*   **Ví dụ khai báo:**
    ```typescript
    import { Component, ChangeDetectionStrategy } from '@angular/core';

    @Component({
      changeDetection: ChangeDetectionStrategy.OnPush,
      ...
    })
    export class OptimizedComponent {}
    ```

---

## 3. Tối ưu Change Detection bằng Signals
Với Angular Signals (từ v16+), Angular nhận diện thay đổi chính xác đến từng phần tử HTML sử dụng Signal mà không cần Zone.js quét cả cây Component. Đây là tương lai hướng tới hiệu năng cực hạn của Angular.
""",
        
        "p12.md": """# PHẦN 12. Tối ưu hiệu năng ứng dụng (Performance)

Để đảm ứng dụng tải nhanh và hoạt động mượt mà, bạn cần thực hiện các kỹ thuật tối ưu sau:

---

## 1. Lazy Loading (Như đã đề cập ở Phần 6)
Giúp giảm dung lượng tệp JavaScript tải về ban đầu (Main Bundle Size) bằng cách chia nhỏ ứng dụng ra thành các gói mã nguồn riêng lẻ chỉ tải về khi truy cập đường dẫn.

---

## 2. Sử dụng `track` trong vòng lặp `@for`
Khi danh sách dữ liệu cập nhật từ API, nếu không khai báo `track`, Angular sẽ xóa sạch toàn bộ các thẻ HTML cũ trong danh sách và vẽ lại từ đầu -> gây giật lag.
Khai báo khóa duy nhất (như ID bản ghi) giúp Angular chỉ cập nhật dòng nào có sự thay đổi.

```html
@for (item of products; track item.id) {
  <li>{{ item.name }}</li>
}
```

---

## 3. Tối ưu hóa tính toán trong Template bằng Pure Pipes
Tuyệt đối không gọi trực tiếp hàm xử lý tính toán trong tệp HTML như thế này:
```html
<!-- KHÔNG NÊN: Hàm calculatePrice() sẽ chạy liên tục mỗi khi màn hình quét detect changes -->
<p>Giá: {{ calculatePrice(product) }}</p>
```
**Giải pháp:** Hãy viết một Custom Pipe. Pipe có tính chất "Pure" - nó chỉ chạy tính toán lại một lần duy nhất khi tham số đầu vào thay đổi, giúp tiết kiệm CPU cực lớn.
""",
        
        "p13.md": """# PHẦN 13. Kiểm thử ứng dụng (Testing)

Kiểm thử tự động giúp đảm bảo code không bị lỗi khi nâng cấp hệ thống hoặc viết thêm chức năng mới.

---

## 1. Unit Test (Kiểm thử đơn vị)
Kiểm thử riêng lẻ từng hàm, service hoặc component bằng thư viện Jasmine (hoặc Jest) kết hợp công cụ chạy kiểm thử Karma (hoặc Vitest).
Angular cung cấp công cụ `TestBed` để giả lập môi trường chạy ứng dụng.

### Ví dụ Test Service:
```typescript
import { TestBed } from '@angular/core/testing';
import { LoggerService } from './logger.service';

describe('LoggerService', () => {
  let service: LoggerService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LoggerService); // Lấy instance của service
  });

  it('hàm log() hoạt động bình thường', () => {
    spyOn(console, 'log');
    service.log('Test message');
    expect(console.log).toHaveBeenCalledWith(jasmine.stringContaining('Test message'));
  });
});
```

---

## 2. End-to-End Test (E2E Test)
Kiểm thử toàn bộ hệ thống từ góc nhìn người dùng thực tế (nhấp chuột, điền form, chờ trang chuyển tiếp...). Các công cụ phổ biến: Cypress, Playwright.
""",
        
        "p14.md": """# PHẦN 14. Server-Side Rendering (SSR) & Hydration

Mặc định Angular hoạt động theo cơ chế Client-Side Rendering (CSR): Trình duyệt tải về một file HTML rỗng cùng các tệp JS, sau đó trình duyệt tự chạy JS để vẽ ra giao diện. Nhược điểm: Tải trang đầu tiên hơi chậm và SEO Google khó đọc nội dung.

---

## 1. SSR hoạt động như thế nào?
*   Khi người dùng gửi yêu cầu truy cập trang web, một server Node.js chạy phía sau sẽ dựng trước giao diện ứng dụng thành tệp HTML hoàn chỉnh có đầy đủ nội dung chữ/ảnh.
*   Server gửi tệp HTML đã có nội dung này về cho trình duyệt -> Người dùng thấy giao diện hiện ra ngay lập tức.

---

## 2. Hydration (Tích hợp lại)
Sau khi trình duyệt nhận HTML từ server SSR, Angular chạy ngầm phía client để tải tệp JavaScript xuống và "cắm" các sự kiện click chuột, các trạng thái xử lý logic vào tệp HTML tĩnh đó mà không làm vẽ lại màn hình. Quá trình này gọi là **Hydration**.
""",
        
        "p15.md": """# PHẦN 15. Kiến trúc ứng dụng & Best Practices

Để dự án không trở thành "mớ bòng bong" khi quy mô phình to, ta cần tuân thủ các quy tắc tổ chức kiến trúc sau:

---

## 1. Tổ chức thư mục theo chức năng (Folder Structure by Feature)
Phân chia thư mục gọn gàng như dự án hiện tại của bạn:
*   `core/`: Chứa các service dùng chung toàn hệ thống tồn tại duy nhất (như Authentication, HTTP Interceptors, LanguageService).
*   `shared/`: Chứa các component dùng chung (như Nút bấm tùy chỉnh, Pop-up xác nhận, Pipes định dạng tiền) được sử dụng ở nhiều màn hình khác nhau.
*   `features/`: Mỗi chức năng nghiệp vụ độc lập là một thư mục con riêng biệt (ví dụ: `category/` chứa routes, components, services riêng của nó).

---

## 2. Best Practices khi viết code Angular
1.  **Hủy đăng ký lắng nghe (Unsubscribe) để tránh rò rỉ bộ nhớ (Memory Leak):**
    Khi subscribe các luồng dữ liệu RxJS, bạn phải hủy lắng nghe khi component bị phá hủy. Trong Angular hiện đại, hãy sử dụng toán tử `takeUntilDestroyed` hoặc dùng cú pháp Async Pipe ở tệp HTML.
2.  **Giữ Component "gầy" (Lean Components):**
    Component chỉ nên chứa logic hiển thị giao diện và bắt sự kiện của người dùng. Mọi tác vụ tính toán phức tạp, gọi API phải đẩy hết vào trong Service.
3.  **Tách biệt Smart Components và Dumb Components:**
    *   *Smart Component (Component quản lý):* Giao tiếp trực tiếp với các Service, hứng dữ liệu API, điều phối dữ liệu (ví dụ: `CategoryListComponent`).
    *   *Dumb Component (Component trình diễn):* Chỉ nhận dữ liệu từ `@Input` và phát ra sự kiện `@Output` lên cha, không tự gọi service lấy dữ liệu (ví dụ: `CategoryDialogComponent`). Giúp tái sử dụng tối đa.
"""
    }
    
    for filename, content in files_content.items():
        filepath = os.path.join(base_dir, filename)
        print(f"Writing {filepath}...")
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(content.strip())
            
    print("All files generated successfully!")

if __name__ == "__main__":
    create_files()
