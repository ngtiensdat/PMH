package com.example.paymenthub.common.exception;

import com.example.paymenthub.common.enums.BusinessErrorCode;

public class MakerCheckerConflictException extends BusinessRuleException {

    public MakerCheckerConflictException(String message) {
        super(message);
    }

    public MakerCheckerConflictException(BusinessErrorCode errorCode) {
        super(errorCode);
    }
}
