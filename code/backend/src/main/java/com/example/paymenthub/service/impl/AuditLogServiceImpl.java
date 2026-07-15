package com.example.paymenthub.service.impl;

import com.example.paymenthub.dto.response.AuditLogDTO;
import com.example.paymenthub.entity.AuditLog;
import com.example.paymenthub.repository.AuditLogRepository;
import com.example.paymenthub.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository repository;
    private final PlatformTransactionManager transactionManager;

    private String getClientIp() {
        try {
            org.springframework.web.context.request.RequestAttributes attributes = 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                jakarta.servlet.http.HttpServletRequest request = 
                    ((org.springframework.web.context.request.ServletRequestAttributes) attributes).getRequest();
                
                String ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("Proxy-Client-IP");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                }
                // Nếu đi qua nhiều proxy thì lấy IP đầu tiên
                if (ipAddress != null && ipAddress.contains(",")) {
                    ipAddress = ipAddress.split(",")[0].trim();
                }
                return ipAddress;
            }
        } catch (Exception e) {
            log.warn("[AuditLog] Failed to get client IP: {}", e.getMessage());
        }
        return "127.0.0.1";
    }

    /**
     * Ghi log trong transaction độc lập sử dụng TransactionTemplate để tránh
     * gây lỗi UnexpectedRollbackException cho nghiệp vụ chính khi ghi log thất bại.
     */
    @Override
    public void log(String module, String recordId, String action, String performedBy,
                    String oldData, String newData, String description,
                    Integer statusBefore, Integer statusAfter) {
        try {
            AuditLog entry = AuditLog.builder()
                    .module(module)
                    .recordId(recordId)
                    .action(action)
                    .performedBy(performedBy)
                    .actionDate(LocalDateTime.now())
                    .oldData(oldData)
                    .newDataLog(newData)
                    .description(description)
                    .statusBefore(statusBefore)
                    .statusAfter(statusAfter)
                    .ipAddress(getClientIp())
                    .build();

            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            transactionTemplate.executeWithoutResult(status -> {
                repository.save(entry);
            });

            log.debug("[AuditLog] Saved. module={}, recordId={}, action={}, user={}, ip={}", module, recordId, action, performedBy, entry.getIpAddress());
        } catch (Exception e) {
            // Không để lỗi audit ảnh hưởng đến nghiệp vụ chính
            log.error("[AuditLog] Failed to save audit log. module={}, recordId={}, action={}. Error: {}",
                    module, recordId, action, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getHistory(String module, String recordId) {
        return repository
                .findByModuleAndRecordIdOrderByActionDateDesc(module, recordId)
                .stream()
                .map(logEntry -> new AuditLogDTO(
                        logEntry.getId(),
                        logEntry.getModule(),
                        logEntry.getRecordId(),
                        logEntry.getAction(),
                        logEntry.getPerformedBy(),
                        logEntry.getActionDate(),
                        logEntry.getDescription(),
                        logEntry.getOldData(),
                        logEntry.getNewDataLog(),
                        logEntry.getStatusBefore(),
                        logEntry.getStatusAfter(),
                        logEntry.getIpAddress()
                ))
                .toList();
    }
}
