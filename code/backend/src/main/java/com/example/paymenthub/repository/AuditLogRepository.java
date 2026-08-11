package com.example.paymenthub.repository;

import com.example.paymenthub.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Lấy lịch sử thao tác theo module + recordId, sắp xếp mới nhất lên đầu.
     */
    Page<AuditLog> findByModuleAndRecordIdOrderByActionDateDesc(String module, String recordId, Pageable pageable);
}
