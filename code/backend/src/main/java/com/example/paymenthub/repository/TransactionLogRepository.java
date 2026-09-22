package com.example.paymenthub.repository;

import com.example.paymenthub.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionLogRepository
        extends JpaRepository<TransactionLog, Long>, JpaSpecificationExecutor<TransactionLog> {

    /**
     * Lấy MAX(ID) của bảng TRANSACTION_LOG — dùng làm Snapshot Boundary khi tạo Export Job.
     * Trả về Optional.empty() nếu bảng chưa có bản ghi nào.
     */
    @Query(value = "SELECT MAX(t.id) FROM TransactionLog t")
    Optional<Long> findMaxId();
}
