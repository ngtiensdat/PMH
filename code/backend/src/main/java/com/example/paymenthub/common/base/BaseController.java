package com.example.paymenthub.common.base;

import org.springframework.http.ResponseEntity;

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
}
