package com.example.paymenthub.service.impl;

import com.example.paymenthub.dto.request.TransactionLogSearchCriteria;
import com.example.paymenthub.entity.TransactionLog;
import com.example.paymenthub.repository.TransactionLogRepository;
import com.example.paymenthub.repository.specification.TransactionLogSpecification;
import com.example.paymenthub.service.TransactionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.example.paymenthub.common.exception.CustomBusinessException;

@Service
@RequiredArgsConstructor
public class TransactionLogServiceImpl implements TransactionLogService {

    private final TransactionLogRepository repository;

    @Override
    public Page<TransactionLog> search(TransactionLogSearchCriteria criteria, Pageable pageable) {
        LocalDateTime fromDate = parseDateTime(criteria.getFromDate(), true);
        LocalDateTime toDate = parseDateTime(criteria.getToDate(), false);

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new CustomBusinessException("INVALID_DATE_RANGE", "Từ ngày giờ không được lớn hơn Đến ngày giờ");
        }

        Specification<TransactionLog> spec = TransactionLogSpecification.filter(
                criteria.getAccountNo(),
                criteria.getTransactionCode(),
                criteria.getStatus(),
                fromDate,
                toDate);

        return repository.findAll(spec, pageable);
    }

    /**
     * Parse chuỗi ngày giờ từ frontend.
     * Chấp nhận 2 định dạng:
     * - "yyyy-MM-ddTHH:mm:ss" (từ TuiInputDatetime — frontend gửi giờ cụ thể)
     * - "yyyy-MM-dd" (fallback — tự gán start-of-day hoặc end-of-day)
     *
     * @param dateStr chuỗi ngày giờ từ query param
     * @param isStart true = start-of-day nếu chỉ có ngày; false = end-of-day
     */
    private LocalDateTime parseDateTime(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isBlank())
            return null;
        try {
            // Thử parse dạng đầy đủ datetime trước
            if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            // Fallback: chỉ có ngày → gán start/end của ngày
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return isStart
                    ? date.atStartOfDay()
                    : date.atTime(23, 59, 59, 999_999_000);
        } catch (Exception e) {
            // Ném lỗi rõ ràng thay vì nuốt exception và bỏ qua filter ngày
            throw new CustomBusinessException("INVALID_DATE_FORMAT",
                    "Định dạng ngày giờ không hợp lệ: '" + dateStr + "'. Hỗ trợ: 'yyyy-MM-dd' hoặc 'yyyy-MM-ddTHH:mm:ss'");
        }
    }
}
