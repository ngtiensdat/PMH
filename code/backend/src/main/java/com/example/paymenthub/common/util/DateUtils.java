package com.example.paymenthub.common.util;

import com.example.paymenthub.common.enums.BusinessErrorCode;
import com.example.paymenthub.common.exception.BusinessRuleException;

import java.time.LocalDateTime;

public class DateUtils {

    /**
     * 1. Ngày hiệu lực (effectiveDate) không được null.
     * 2. Ngày hết hiệu lực (endEffectiveDate) nếu có thì phải lớn hơn hoặc bằng Ngày hiệu lực.
     */
    public static void validateEffectiveDates(LocalDateTime effectiveDate, LocalDateTime endEffectiveDate) {
        if (effectiveDate == null) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_EFFECTIVE_DATE);
        }
        if (endEffectiveDate != null && endEffectiveDate.isBefore(effectiveDate)) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_DATE_RANGE);
        }
    }
}
