package com.example.paymenthub.service;

import com.example.paymenthub.dto.request.TransactionLogSearchCriteria;
import com.example.paymenthub.entity.TransactionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionLogService {
    Page<TransactionLog> search(TransactionLogSearchCriteria criteria, Pageable pageable);
}
