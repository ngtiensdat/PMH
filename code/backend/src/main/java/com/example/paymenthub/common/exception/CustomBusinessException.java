package com.example.paymenthub.common.exception;

public class CustomBusinessException extends RuntimeException {

    private final String errorCode;

    public CustomBusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public CustomBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
