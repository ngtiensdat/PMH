package com.example.paymenthub.common.util;

import java.time.LocalDateTime;

public class DateUtils {

    /**
     * 1. Ngày hiệu lực (effectiveDate) không được null.
     * 2. Ngày hết hiệu lực (endEffectiveDate) nếu có thì phải lớn hơn hoặc bằng
     * Ngày hiệu lực.
     */
    public static void validateEffectiveDates(LocalDateTime effectiveDate, LocalDateTime endEffectiveDate) {
        if (effectiveDate == null) {
            throw new IllegalArgumentException("Ngày hiệu lực không được để trống!");
        }
        if (endEffectiveDate != null && endEffectiveDate.isBefore(effectiveDate)) {
            throw new IllegalArgumentException(
                    String.format("Ngày hết hiệu lực (%s) không được nhỏ hơn Ngày hiệu lực (%s)!",
                            endEffectiveDate.toString().replace("T", " "),
                            effectiveDate.toString().replace("T", " ")));
        }
    }
}
