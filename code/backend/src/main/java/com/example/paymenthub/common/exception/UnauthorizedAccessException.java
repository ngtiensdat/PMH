package com.example.paymenthub.common.exception;

import com.example.paymenthub.common.enums.AuthErrorCode;

public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(AuthErrorCode authErrorCode) {
        super(authErrorCode != null ? authErrorCode.getMessage() : "Unauthorized access");
    }
}
