package com.example.paymenthub.common.base;

import com.example.paymenthub.dto.response.BatchItemResultDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public abstract class BaseController {

    protected <T> ResponseEntity<ApiResponse<T>> ok(T body, String message) {
        return ResponseEntity.ok(ApiResponse.success(body, message));
    }

    protected <T> ResponseEntity<ApiResponse<T>> ok(T body) {
        return ResponseEntity.ok(ApiResponse.success(body, "Thành công"));
    }

    protected ResponseEntity<ApiResponse<Void>> successResponse(String message) {
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    /**
     * Xử lý kết quả batch: nếu mọi item đều FAILED → trả 400 với lỗi đầu tiên.
     * Tái sử dụng cho mọi endpoint batch-approve / batch-reject.
     */
    protected ResponseEntity<ApiResponse<List<BatchItemResultDTO>>> handleBatchResult(
            List<BatchItemResultDTO> result, String successMsg, String defaultErrorMsg) {

        long successCount = result.stream()
                .filter(r -> "SUCCESS".equalsIgnoreCase(r.getStatus()))
                .count();

        if (successCount == 0 && !result.isEmpty()) {
            String firstErr = result.get(0).getErrorMessage() != null
                    ? result.get(0).getErrorMessage()
                    : defaultErrorMsg;
            return ResponseEntity.badRequest().body(ApiResponse.error(firstErr));
        }
        return ok(result, successMsg);
    }
}

