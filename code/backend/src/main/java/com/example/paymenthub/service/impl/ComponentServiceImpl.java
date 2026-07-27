package com.example.paymenthub.service.impl;

import com.example.paymenthub.dto.request.ComponentDTO;
import com.example.paymenthub.entity.ProcessingComponent;
import com.example.paymenthub.repository.ComponentRepository;
import com.example.paymenthub.service.ComponentService;
import com.example.paymenthub.repository.specification.ComponentSpecification;
import com.example.paymenthub.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class ComponentServiceImpl implements ComponentService {

    private static final String MODULE = "COMPONENT";

    private final ComponentRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    public ComponentServiceImpl(ComponentRepository repository,
            ObjectMapper objectMapper,
            AuditLogService auditLogService,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.transactionManager = transactionManager;
    }

    // ─── Helper: serialize entity sang JSON ─────────────────────────────────
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[Component] Failed to serialize to JSON: {}", e.getMessage());
            return null;
        }
    }

    // ─── Helper: snapshot entity ─────────────────────────────────────────────
    private Map<String, Object> snapshot(ProcessingComponent e) {
        Map<String, Object> m = new HashMap<>();
        m.put("componentCode", e.getComponentCode());
        m.put("componentName", e.getComponentName());
        m.put("messageType", e.getMessageType());
        m.put("connectionMethod", e.getConnectionMethod());
        m.put("checkToken", e.getCheckToken());
        m.put("description", e.getDescription());
        m.put("status", e.getStatus());
        m.put("isActive", e.getIsActive());
        m.put("isDisplay", e.getIsDisplay());
        m.put("effectiveDate", e.getEffectiveDate() != null ? e.getEffectiveDate().toString() : null);
        m.put("endEffectiveDate", e.getEndEffectiveDate() != null ? e.getEndEffectiveDate().toString() : null);
        m.put("createdBy", e.getCreatedBy());
        m.put("updatedBy", e.getUpdatedBy());
        return m;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcessingComponent> search(
            String componentCode, String componentName, String messageType,
            String connectionMethod, List<Integer> statuses, List<Integer> isActives, Pageable pageable) {
        Specification<ProcessingComponent> spec = ComponentSpecification.filter(
                componentCode, componentName, messageType, connectionMethod, statuses, isActives);
        return repository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessingComponent getByCode(String code) {
        return repository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cấu phần có mã: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessingComponent> getActiveList(Integer status) {
        if (status != null) {
            return repository.findAllByIsActiveAndStatusOrderByComponentNameAsc(1, status);
        }
        return repository.findAllByIsActiveOrderByComponentNameAsc(1);
    }

    @Override
    public ProcessingComponent create(ComponentDTO dto, String username) {
        log.info("[Component] Creating. user={}, componentCode={}", username, dto.getComponentCode());

        if (repository.existsByComponentCode(dto.getComponentCode())) {
            throw new IllegalStateException("Mã cấu phần '" + dto.getComponentCode() + "' đã tồn tại!");
        }

        ProcessingComponent entity = ProcessingComponent.builder()
                .componentCode(dto.getComponentCode().toUpperCase())
                .componentName(dto.getComponentName())
                .messageType(dto.getMessageType())
                .connectionMethod(dto.getConnectionMethod())
                .checkToken(dto.getCheckToken() != null ? dto.getCheckToken() : "N")
                .description(dto.getDescription())
                .status(1)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : 1)
                .isDisplay(1)
                .effectiveDate(dto.getEffectiveDate())
                .endEffectiveDate(dto.getEndEffectiveDate())
                .build();
        entity.setCreatedBy(username);
        entity.setUpdatedBy(username);

        ProcessingComponent saved = repository.save(entity);
        log.info("[Component] Created. code={}", saved.getComponentCode());

        // Ghi audit log
        auditLogService.log(
                MODULE, saved.getComponentCode(),
                "Tạo mới", username,
                null, toJson(snapshot(saved)),
                String.format("Tạo mới cấu phần: %s - %s", saved.getComponentCode(), saved.getComponentName()),
                null, 1);

        return saved;
    }

    @Override
    public ProcessingComponent update(String code, ComponentDTO dto, String username) {
        ProcessingComponent entity = getByCode(code);
        String oldJson = toJson(snapshot(entity));
        int statusBefore = entity.getStatus();

        ProcessingComponent saved;
        String action;
        int statusAfter;

        if (entity.getStatus() == 4) {
            try {
                Map<String, Object> changes = new HashMap<>();
                changes.put("componentName", dto.getComponentName());
                changes.put("messageType", dto.getMessageType());
                changes.put("connectionMethod", dto.getConnectionMethod());
                changes.put("checkToken", dto.getCheckToken());
                changes.put("description", dto.getDescription());
                changes.put("isActive", dto.getIsActive());
                changes.put("effectiveDate", dto.getEffectiveDate() != null ? dto.getEffectiveDate().toString() : null);
                changes.put("endEffectiveDate",
                        dto.getEndEffectiveDate() != null ? dto.getEndEffectiveDate().toString() : null);

                entity.setNewData(objectMapper.writeValueAsString(changes));
                entity.setStatus(3);
                entity.setUpdatedBy(username);
                saved = repository.save(entity);
                action = "Gửi duyệt sửa";
                statusAfter = 3;
            } catch (Exception e) {
                throw new RuntimeException("Lỗi serialize NEW_DATA: " + e.getMessage());
            }
        } else {
            entity.setComponentName(dto.getComponentName());
            entity.setMessageType(dto.getMessageType());
            entity.setConnectionMethod(dto.getConnectionMethod());
            entity.setCheckToken(dto.getCheckToken());
            entity.setDescription(dto.getDescription());
            entity.setIsActive(dto.getIsActive());
            entity.setEffectiveDate(dto.getEffectiveDate());
            entity.setEndEffectiveDate(dto.getEndEffectiveDate());
            entity.setUpdatedBy(username);
            entity.setStatus(1);
            saved = repository.save(entity);
            action = "Cập nhật";
            statusAfter = 1;
        }

        String newJson = toJson(snapshot(saved));

        // Ghi audit log
        auditLogService.log(
                MODULE, code,
                action, username,
                oldJson, newJson,
                String.format("%s cấu phần Code=%s bởi %s", action, code, username),
                statusBefore, statusAfter);

        return saved;
    }

    @Override
    public void delete(String code, String username) {
        log.info("[Component] Deleting. code={}, user={}", code, username);
        
        // Phân quyền: Chỉ Maker mới được phép xóa/hủy yêu cầu
        if (!"USER01".equalsIgnoreCase(username)) {
            throw new IllegalStateException("Chỉ Chuyên viên (Maker) mới có quyền thực hiện chức năng xóa/hủy!");
        }

        ProcessingComponent entity = getByCode(code);

        if (entity.getIsDisplay() == 2) {
            // Bản ghi đã được duyệt trước đó (isDisplay == 2):
            // Nếu bản ghi đang có thay đổi chờ duyệt (status = 3) hoặc bị từ chối (status = 5):
            // Hành động xóa sẽ đóng vai trò hủy bỏ yêu cầu chỉnh sửa này và khôi phục về bản duyệt cũ.
            if (entity.getStatus() == 3 || entity.getStatus() == 5) {
                String oldJson = toJson(snapshot(entity));
                entity.setNewData(null);
                entity.setStatus(4); // Khôi phục trạng thái đã phê duyệt
                entity.setUpdatedBy(username);
                ProcessingComponent saved = repository.save(entity);
                
                // Ghi audit log
                auditLogService.log(
                        MODULE, code,
                        "Hủy yêu cầu sửa", username,
                        oldJson, toJson(snapshot(saved)),
                        String.format("Hủy yêu cầu chỉnh sửa và khôi phục trạng thái đã duyệt cho cấu phần Code=%s", code),
                        3, 4);
                return;
            }
            throw new IllegalStateException("Bản ghi đã phê duyệt và đang vận hành (STATUS = 4), không được phép xóa!");
        }

        // Bản ghi chưa từng được duyệt (isDisplay == 1): Xóa vật lý
        String oldJson = toJson(snapshot(entity));
        repository.delete(entity);
        log.info("[Component] Deleted. code={}", code);

        // Ghi audit log
        auditLogService.log(
                MODULE, code,
                "Xóa", username,
                oldJson, null,
                String.format("Xóa cấu phần chưa duyệt: %s - %s", entity.getComponentCode(), entity.getComponentName()),
                entity.getStatus(), null);
    }

    @Override
    public ProcessingComponent sendForApproval(String code, String username) {
        log.info("[Component] Sending for approval. code={}, user={}", code, username);
        ProcessingComponent entity = getByCode(code);
        int statusBefore = entity.getStatus();

        entity.setStatus(3);
        entity.setUpdatedBy(username);
        ProcessingComponent saved = repository.save(entity);

        // Ghi audit log
        auditLogService.log(
                MODULE, code,
                "Gửi duyệt", username,
                null, null,
                String.format("Gửi duyệt cấu phần Code=%s: %s", code, entity.getComponentName()),
                statusBefore, 3);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRawDataForExport() {
        String sql = "SELECT COMPONENT_CODE, COMPONENT_NAME, MESSAGE_TYPE, CONNECTION_METHOD, " +
                "CHECK_TOKEN, DESCRIPTION, STATUS, IS_ACTIVE, EFFECTIVE_DATE, END_EFFECTIVE_DATE " +
                "FROM PMH_COMPONENTS WHERE IS_ACTIVE = 1 ORDER BY COMPONENT_NAME";

        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("componentCode", row[0]);
            map.put("componentName", row[1]);
            map.put("messageType", row[2]);
            map.put("connectionMethod", row[3]);
            map.put("checkToken", row[4]);
            map.put("description", row[5]);
            map.put("status", row[6] != null ? ((Number) row[6]).intValue() : null);
            map.put("isActive", row[7] != null ? ((Number) row[7]).intValue() : null);
            map.put("effectiveDate", row[8]);
            map.put("endEffectiveDate", row[9]);
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> batchApprove(List<String> codes, String approver) {
        log.info("[Component] Batch approve started. count={}, approver={}", codes.size(), approver);
        List<Map<String, Object>> results = new ArrayList<>();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (String code : codes) {
            results.add(approveSingleComponent(code, approver, transactionTemplate));
        }

        long successCount = results.stream().filter(r -> Boolean.TRUE.equals(r.get("success"))).count();
        log.info("[Component] Batch approve done. success={}/{}", successCount, codes.size());
        return results;
    }

    private Map<String, Object> approveSingleComponent(String code, String approver, TransactionTemplate transactionTemplate) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", code);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                ProcessingComponent entity = getByCode(code);
                
                // Kiểm tra phân quyền: Người duyệt không được trùng với người tạo/cập nhật yêu cầu
                if (approver.equalsIgnoreCase(entity.getCreatedBy()) || approver.equalsIgnoreCase(entity.getUpdatedBy())) {
                    throw new IllegalStateException("Người phê duyệt (" + approver + ") không được trùng với người tạo/cập nhật yêu cầu!");
                }

                int statusBefore = entity.getStatus();

                if (entity.getNewData() != null && !entity.getNewData().isEmpty()) {
                    try {
                        Map<String, Object> changes = objectMapper.readValue(
                                entity.getNewData(),
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                });
                        if (changes.containsKey("componentName"))
                            entity.setComponentName((String) changes.get("componentName"));
                        if (changes.containsKey("messageType"))
                            entity.setMessageType((String) changes.get("messageType"));
                        if (changes.containsKey("connectionMethod"))
                            entity.setConnectionMethod((String) changes.get("connectionMethod"));
                        if (changes.containsKey("checkToken"))
                            entity.setCheckToken((String) changes.get("checkToken"));
                        if (changes.containsKey("description"))
                            entity.setDescription((String) changes.get("description"));
                        if (changes.containsKey("isActive") && changes.get("isActive") != null)
                            entity.setIsActive(((Number) changes.get("isActive")).intValue());
                        if (changes.containsKey("effectiveDate") && changes.get("effectiveDate") != null)
                            entity.setEffectiveDate(LocalDateTime.parse((String) changes.get("effectiveDate")));
                        if (changes.containsKey("endEffectiveDate") && changes.get("endEffectiveDate") != null)
                            entity.setEndEffectiveDate(
                                    LocalDateTime.parse((String) changes.get("endEffectiveDate")));
                        entity.setNewData(null);
                        repository.saveAndFlush(entity);
                    } catch (Exception ex) {
                        throw new RuntimeException("Lỗi giải mã dữ liệu thay đổi: " + ex.getMessage(), ex);
                    }
                }

                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_APPROVE_COMPONENT");
                query.registerStoredProcedureParameter("p_code", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_user", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_status", Integer.class, ParameterMode.OUT);
                query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);
                query.setParameter("p_code", code);
                query.setParameter("p_user", approver);
                query.execute();

                Object spStatusObj = query.getOutputParameterValue("p_status");
                String spMessage = (String) query.getOutputParameterValue("p_message");
                boolean success = false;
                if (spStatusObj instanceof Number) {
                    success = ((Number) spStatusObj).intValue() == 1;
                }

                res.put("success", success);
                res.put("message", spMessage);

                if (success) {
                    // Ghi audit log
                    auditLogService.log(
                            MODULE, code,
                            "Phê duyệt", approver,
                            null, null,
                            String.format("Phê duyệt cấu phần Code=%s. SP: %s", code, spMessage),
                            statusBefore, 4);
                } else {
                    throw new RuntimeException(spMessage);
                }
            });
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage() != null ? e.getMessage() : "Lỗi thực thi");
            log.error("[Component] Batch approve failed for code={}. error={}", code, e.getMessage());
        }
        return res;
    }

    @Override
    public List<Map<String, Object>> batchReject(List<String> codes, String reason, String approver) {
        log.info("[Component] Batch reject started. count={}, approver={}", codes.size(), approver);
        List<Map<String, Object>> results = new ArrayList<>();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (String code : codes) {
            results.add(rejectSingleComponent(code, reason, approver, transactionTemplate));
        }

        long successCount = results.stream().filter(r -> Boolean.TRUE.equals(r.get("success"))).count();
        log.info("[Component] Batch reject done. success={}/{}", successCount, codes.size());
        return results;
    }

    private Map<String, Object> rejectSingleComponent(String code, String reason, String approver, TransactionTemplate transactionTemplate) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", code);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                ProcessingComponent entity = getByCode(code);
                
                // Kiểm tra phân quyền: Người duyệt không được trùng với người tạo/cập nhật yêu cầu
                if (approver.equalsIgnoreCase(entity.getCreatedBy()) || approver.equalsIgnoreCase(entity.getUpdatedBy())) {
                    throw new IllegalStateException("Người phê duyệt (" + approver + ") không được trùng với người tạo/cập nhật yêu cầu!");
                }

                int statusBefore = entity.getStatus();

                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_REJECT_COMPONENT");
                query.registerStoredProcedureParameter("p_code", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_user", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_status", Integer.class, ParameterMode.OUT);
                query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);
                query.setParameter("p_code", code);
                query.setParameter("p_user", approver);
                query.execute();

                Object spStatusObj = query.getOutputParameterValue("p_status");
                String spMessage = (String) query.getOutputParameterValue("p_message");
                boolean success = false;
                if (spStatusObj instanceof Number) {
                    success = ((Number) spStatusObj).intValue() == 1;
                }

                res.put("success", success);
                res.put("message", spMessage);

                if (success) {
                    auditLogService.log(
                            MODULE, code,
                            "Từ chối", approver,
                            null, null,
                            reason != null && !reason.trim().isEmpty()
                                    ? String.format("Từ chối duyệt cấu phần Code=%s. Lý do: %s. SP: %s", code,
                                            reason, spMessage)
                                    : String.format("Từ chối duyệt cấu phần Code=%s. SP: %s", code, spMessage),
                            statusBefore, 5);
                } else {
                    throw new RuntimeException(spMessage);
                }
            });
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage() != null ? e.getMessage() : "Lỗi thực thi");
            log.error("[Component] Batch reject failed for code={}. error={}", code, e.getMessage());
        }
        return res;
    }
}
