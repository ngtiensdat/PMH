package com.example.paymenthub.repository.specification;

import com.example.paymenthub.entity.TransactionLog;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;

public class TransactionLogSpecification {

    private TransactionLogSpecification() {
    }

    public static Specification<TransactionLog> filter(
            String accountNo,
            String transactionCode,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        return Specification
                .where(likeIgnoreCase("accountNo", accountNo))
                .and(likeIgnoreCase("transactionCode", transactionCode))
                .and(exactMatch("status", status))
                .and(betweenDates("createdAt", fromDate, toDate));
    }

    private static Specification<TransactionLog> likeIgnoreCase(String field, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) return null;
            return cb.like(cb.upper(root.get(field)), "%" + value.toUpperCase() + "%");
        };
    }

    private static Specification<TransactionLog> exactMatch(String field, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) return null;
            return cb.equal(cb.upper(root.get(field)), value.toUpperCase());
        };
    }

    private static Specification<TransactionLog> betweenDates(String field, LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) {
                return cb.between(root.get(field), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get(field), from);
            }
            return cb.lessThanOrEqualTo(root.get(field), to);
        };
    }
}
