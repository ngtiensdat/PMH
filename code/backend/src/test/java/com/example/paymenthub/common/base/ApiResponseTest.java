package com.example.paymenthub.common.base;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class ApiResponseTest {

    @Test
    public void testSuccessResponse() {
        String testData = "Hello World";
        String message = "Thao tác thành công";

        // Gọi phương thức tĩnh tạo response thành công
        ApiResponse<String> response = ApiResponse.success(testData, message);

        // Kiểm tra xem dữ liệu sinh ra có đúng kỳ vọng hay không
        assertTrue(response.isSuccess(), "Trường success phải là true");
        assertEquals(message, response.getMessage(), "Message phải khớp với tham số truyền vào");
        assertEquals(testData, response.getData(), "Data phải khớp với dữ liệu truyền vào");
        assertNotNull(response.getTimestamp(), "Timestamp không được phép là null");
    }

    @Test
    public void testErrorResponse() {
        String message = "Đã xảy ra lỗi hệ thống";

        // Gọi phương thức tĩnh tạo response lỗi
        ApiResponse<Object> response = ApiResponse.error(message);

        // Kiểm tra các trường
        assertFalse(response.isSuccess(), "Trường success phải là false");
        assertEquals(message, response.getMessage(), "Message lỗi phải khớp");
        assertNull(response.getData(), "Data lỗi phải là null");
        assertNotNull(response.getTimestamp(), "Timestamp không được null");
    }

    @Test
    public void testBuilderPattern() {
        LocalDateTime customTime = LocalDateTime.of(2026, 8, 17, 12, 0);

        // Khởi tạo đối tượng bằng Builder Pattern sinh ra bởi Lombok
        ApiResponse<Integer> response = ApiResponse.<Integer>builder()
                .success(true)
                .message("Thông điệp tự định nghĩa")
                .data(999)
                .timestamp(customTime)
                .build();

        // Kiểm tra
        assertTrue(response.isSuccess());
        assertEquals("Thông điệp tự định nghĩa", response.getMessage());
        assertEquals(999, response.getData());
        assertEquals(customTime, response.getTimestamp());
    }
}
