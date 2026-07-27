# FILE HỌC CODE THỰC CHIẾN - PHẦN 4 (LEARN04)

Chào mừng bạn đến với bài học số 4! Dưới đây là phần code của lớp **Specification**, nơi Spring Boot xử lý việc tìm kiếm và lọc dữ liệu động từ giao diện gửi lên.

---

## 💻 Đoạn code: Lọc dữ liệu động với JPA Specification

Đoạn code này nằm trong file [GroupCategorySpecification.java](file:///e:/PMH/code/backend/src/main/java/com/example/paymenthub/repository/specification/GroupCategorySpecification.java).

```java
public class GroupCategorySpecification {

    private GroupCategorySpecification() {}

    public static Specification<GroupCategory> filter(
            String paramType,
            String paramValue,
            String paramName,
            List<Integer> statuses,
            List<Integer> isActives
    ) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction(); // Tạo điều kiện mặc định ban đầu là rỗng (1=1)

            if (paramType != null && !paramType.trim().isEmpty()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("paramType")), "%" + paramType.trim().toLowerCase() + "%"));
            }

            if (paramValue != null && !paramValue.trim().isEmpty()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("paramValue")), "%" + paramValue.trim().toLowerCase() + "%"));
            }

            if (statuses != null && !statuses.isEmpty()) {
                predicate = cb.and(predicate, root.get("status").in(statuses));
            }

            return predicate;
        };
    }
}
```

---

## ❓ Câu hỏi dành cho bạn:

### 1. Vai trò của JPA Specification:
*   Tại sao chúng ta phải viết lớp `Specification` này thay vì chỉ viết các hàm tìm kiếm đơn giản trong JPA Repository? Lợi ích lớn nhất của nó khi người dùng tìm kiếm bằng nhiều ô lọc khác nhau trên giao diện (cái nhập, cái bỏ trống) là gì?

### 2. Ý nghĩa các tham số của Lambda Expression:
*   Trong biểu thức lambda `(root, query, cb) -> { ... }`:
    *   **`root`** đại diện cho cái gì? (Gợi ý: Nó đại diện cho thực thể/bảng dữ liệu nào ta đang tìm kiếm?)
    *   **`cb`** (CriteriaBuilder) được dùng để làm gì?
    *   Hàm `cb.conjunction()` trả về điều kiện gì làm nền tảng ban đầu?

### 3. Giải mã câu lệnh so sánh:
*   Dòng code `cb.like(cb.lower(root.get("paramType")), "%" + paramType.trim().toLowerCase() + "%")` tương đương với cú pháp so sánh nào trong SQL mà bạn hay gõ? Tại sao lại cần phải sử dụng hàm `cb.lower(...)` kết hợp với `.toLowerCase()`?

---

## ✍️ Phần trả lời của bạn:
1. viết lớp Specification này có nhiệm vụ tìm kiếm, nó tối ưu hơn vì có thể cộng nhiều điều kiện từ các trường giá trị khác nhau, điều mà JPA repository không làm được
2. trong biểu thức lambda, root là bảng, query là dữ liệu trong bảng và cb là để hiển thị ra dữ liệu. Hàm `cb.conjunction()` trả về điều kiện là 1=1 nghĩa là luôn đúng và trả về dữ liệu ban đầu
3. dùng cb.lower kết hợp với toLowerCase là để xử lý tìm kiếm không phân biệt hoa thường. Cú pháp trong SQL là WHERE 