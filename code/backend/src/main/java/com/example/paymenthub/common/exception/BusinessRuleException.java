package com.example.paymenthub.common.exception;

import com.example.paymenthub.common.enums.BusinessErrorCode;

public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(BusinessErrorCode errorCode) {
        super(errorCode != null ? errorCode.getMessage() : "Business rule error");
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessRuleException(BusinessErrorCode errorCode, Throwable cause) {
        super(errorCode != null ? errorCode.getMessage() : "Business rule error", cause);
    }
}
