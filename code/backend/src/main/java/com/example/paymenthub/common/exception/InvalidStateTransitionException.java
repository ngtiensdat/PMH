package com.example.paymenthub.common.exception;

import com.example.paymenthub.common.enums.BusinessErrorCode;

public class InvalidStateTransitionException extends BusinessRuleException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(BusinessErrorCode errorCode) {
        super(errorCode);
    }
}
