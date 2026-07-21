package com.example.paymenthub.service.impl;

import com.example.paymenthub.dto.request.GroupCategoryDTO;
import com.example.paymenthub.entity.GroupCategory;
import com.example.paymenthub.repository.GroupCategoryRepository;
import com.example.paymenthub.service.GroupCategoryService;
import com.example.paymenthub.repository.specification.GroupCategorySpecification;
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
public class GroupCategoryServiceImpl implements GroupCategoryService {

    private static final String MODULE = "GROUP_CATEGORY";

    private final GroupCategoryRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    public GroupCategoryServiceImpl(GroupCategoryRepository repository,
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
            log.warn("[GroupCategory] Failed to serialize to JSON: {}", e.getMessage());
            return null;
        }
    }

    // ─── Helper: snapshot entity ─────────────────────────────────────────────
    private Map<String, Object> snapshot(GroupCategory e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("paramName", e.getParamName());
        m.put("paramValue", e.getParamValue());
        m.put("paramType", e.getParamType());
        m.put("description", e.getDescription());
        m.put("componentCode", e.getComponentCode());
        m.put("status", e.getStatus());
        m.put("isActive", e.getIsActive());
        m.put("isDisplay", e.getIsDisplay());
        m.put("effectiveDate", e.getEffectiveDate() != null ? e.getEffectiveDate().toString() : null);
        m.put("endEffectiveDate", e.getEndEffectiveDate() != null ? e.getEndEffectiveDate().toString() : null);
        m.put("createdBy", e.getCreatedBy());
        m.put("updatedBy", e.getUpdatedBy());
        return m;
    }

    // ─── Search ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<GroupCategory> search(
            String paramType, String paramValue, String paramName,
            List<Integer> statuses, List<Integer> isActives, Pageable pageable) {
        Specification<GroupCategory> spec = GroupCategorySpecification.filter(
                paramType, paramValue, paramName, statuses, isActives);
        return repository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupCategory getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục có ID: " + id));
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    @Override
    public GroupCategory create(GroupCategoryDTO dto, String username) {
        log.info("[GroupCategory] Creating. user={}, paramType={}, paramValue={}", username, dto.getParamType(),
                dto.getParamValue());

        if (repository.existsByParamNameAndParamValueAndParamType(
                dto.getParamName(), dto.getParamValue(), dto.getParamType())) {
            throw new IllegalStateException(
                    "Đã tồn tại bản ghi với bộ 3: Tên thành phần, Giá trị thành phần và Nhóm này!");
        }

        GroupCategory entity = GroupCategory.builder()
                .paramName(dto.getParamName())
                .paramValue(dto.getParamValue())
                .paramType(dto.getParamType())
                .description(dto.getDescription())
                .componentCode(dto.getComponentCode())
                .status(1)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : 1)
                .isDisplay(1)
                .effectiveDate(dto.getEffectiveDate())
                .endEffectiveDate(dto.getEndEffectiveDate())
                .build();
        entity.setCreatedBy(username);
        entity.setUpdatedBy(username);

        GroupCategory saved = repository.save(entity);
        log.info("[GroupCategory] Created. id={}", saved.getId());

        // Ghi audit log
        auditLogService.log(
                MODULE, String.valueOf(saved.getId()),
                "Tạo mới", username,
                null, toJson(snapshot(saved)),
                String.format("Tạo mới tham số: %s / %s / %s", saved.getParamName(), saved.getParamValue(),
                        saved.getParamType()),
                null, 1);

        return saved;
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    @Override
    public GroupCategory update(Long id, GroupCategoryDTO dto, String username) {
        GroupCategory entity = getById(id);
        String oldJson = toJson(snapshot(entity));
        int statusBefore = entity.getStatus();

        if (repository.existsByParamNameAndParamValueAndParamTypeAndIdNot(
                dto.getParamName(), dto.getParamValue(), dto.getParamType(), id)) {
            throw new IllegalStateException("Đã tồn tại bản ghi khác có cùng bộ 3: Tên, Giá trị và Nhóm!");
        }

        GroupCategory saved;
        String action;
        int statusAfter;

        if (entity.getStatus() == 4) {
            entity.setNewData(buildNewDataJson(dto));
            entity.setStatus(3);
            entity.setUpdatedBy(username);
            saved = repository.save(entity);
            action = "Gửi duyệt sửa";
            statusAfter = 3;
        } else {
            updateEntityFields(entity, dto, username);
            saved = repository.save(entity);
            action = "Cập nhật";
            statusAfter = 1;
        }

        String newJson = toJson(snapshot(saved));

        // Ghi audit log
        auditLogService.log(
                MODULE, String.valueOf(id),
                action, username,
                oldJson, newJson,
                String.format("%s tham số ID=%d bởi %s", action, id, username),
                statusBefore, statusAfter);

        return saved;
    }

    private String buildNewDataJson(GroupCategoryDTO dto) {
        try {
            Map<String, Object> changes = new HashMap<>();
            changes.put("paramName", dto.getParamName());
            changes.put("paramValue", dto.getParamValue());
            changes.put("paramType", dto.getParamType());
            changes.put("description", dto.getDescription());
            changes.put("componentCode", dto.getComponentCode());
            changes.put("effectiveDate", dto.getEffectiveDate().toString());
            changes.put("endEffectiveDate",
                    dto.getEndEffectiveDate() != null ? dto.getEndEffectiveDate().toString() : null);
            changes.put("isActive", dto.getIsActive());
            return objectMapper.writeValueAsString(changes);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi chuyển đổi dữ liệu thay đổi sang JSON: " + e.getMessage(), e);
        }
    }

    private void updateEntityFields(GroupCategory entity, GroupCategoryDTO dto, String username) {
        entity.setParamName(dto.getParamName());
        entity.setParamValue(dto.getParamValue());
        entity.setParamType(dto.getParamType());
        entity.setDescription(dto.getDescription());
        entity.setComponentCode(dto.getComponentCode());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setEndEffectiveDate(dto.getEndEffectiveDate());
        entity.setIsActive(dto.getIsActive());
        entity.setUpdatedBy(username);
        entity.setStatus(1);
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id, String username) {
        log.info("[GroupCategory] Deleting. id={}, user={}", id, username);
        
        // Phân quyền: Chỉ Maker mới được phép xóa/hủy yêu cầu
        if (!"USER01".equalsIgnoreCase(username)) {
            throw new IllegalStateException("Chỉ Chuyên viên (Maker) mới có quyền thực hiện chức năng xóa/hủy!");
        }

        GroupCategory entity = getById(id);

        if (entity.getIsDisplay() == 2) {
            // Bản ghi đã được duyệt trước đó (isDisplay == 2):
            // Nếu bản ghi đang có thay đổi chờ duyệt (status = 3) hoặc bị từ chối (status = 5):
            // Hành động xóa sẽ đóng vai trò hủy bỏ yêu cầu chỉnh sửa này và khôi phục về bản duyệt cũ.
            if (entity.getStatus() == 3 || entity.getStatus() == 5) {
                String oldJson = toJson(snapshot(entity));
                entity.setNewData(null);
                entity.setStatus(4); // Khôi phục trạng thái đã phê duyệt
                entity.setUpdatedBy(username);
                GroupCategory saved = repository.save(entity);
                
                // Ghi audit log
                auditLogService.log(
                        MODULE, String.valueOf(id),
                        "Hủy yêu cầu sửa", username,
                        oldJson, toJson(snapshot(saved)),
                        String.format("Hủy yêu cầu chỉnh sửa và khôi phục trạng thái đã duyệt cho ID=%d", id),
                        3, 4);
                return;
            }
            throw new IllegalStateException("Bản ghi đã phê duyệt và đang vận hành (STATUS = 4), không được phép xóa!");
        }

        // Bản ghi chưa từng được duyệt (isDisplay == 1): Xóa vật lý
        String oldJson = toJson(snapshot(entity));
        repository.delete(entity);
        log.info("[GroupCategory] Deleted. id={}", id);

        // Ghi audit log (sau khi delete để lưu oldData đầy đủ)
        auditLogService.log(
                MODULE, String.valueOf(id),
                "Xóa", username,
                oldJson, null,
                String.format("Xóa tham số chưa duyệt: %s / %s / %s", entity.getParamName(), entity.getParamValue(),
                        entity.getParamType()),
                entity.getStatus(), null);
    }

    // ─── Send For Approval ───────────────────────────────────────────────────

    @Override
    public GroupCategory sendForApproval(Long id, String username) {
        log.info("[GroupCategory] Sending for approval. id={}, user={}", id, username);
        GroupCategory entity = getById(id);
        int statusBefore = entity.getStatus();

        entity.setStatus(3);
        entity.setUpdatedBy(username);
        GroupCategory saved = repository.save(entity);

        // Ghi audit log
        auditLogService.log(
                MODULE, String.valueOf(id),
                "Gửi duyệt", username,
                null, null,
                String.format("Gửi duyệt tham số ID=%d: %s / %s", id, entity.getParamName(), entity.getParamValue()),
                statusBefore, 3);

        return saved;
    }

    // ─── Joined List (Native Query) ──────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getJoinedList() {
        String sql = "SELECT gc.ID, gc.PARAM_NAME, gc.PARAM_VALUE, gc.PARAM_TYPE, gc.DESCRIPTION, " +
                "gc.COMPONENT_CODE, c.COMPONENT_NAME, gc.STATUS, gc.IS_ACTIVE, gc.EFFECTIVE_DATE " +
                "FROM PMH_GROUP_CATEGORY gc " +
                "LEFT JOIN PMH_COMPONENTS c ON gc.COMPONENT_CODE = c.COMPONENT_CODE " +
                "ORDER BY gc.UPDATED_DATE DESC";

        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", row[0] != null ? ((Number) row[0]).longValue() : null);
            map.put("paramName", row[1]);
            map.put("paramValue", row[2]);
            map.put("paramType", row[3]);
            map.put("description", row[4]);
            map.put("componentCode", row[5]);
            map.put("componentName", row[6] != null ? row[6] : "Chưa xác định");
            map.put("status", row[7] != null ? ((Number) row[7]).intValue() : null);
            map.put("isActive", row[8] != null ? ((Number) row[8]).intValue() : null);
            map.put("effectiveDate", row[9]);
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRawDataForExport() {
        String sql = "SELECT ID, PARAM_NAME, PARAM_VALUE, PARAM_TYPE, DESCRIPTION, STATUS, IS_ACTIVE, EFFECTIVE_DATE, END_EFFECTIVE_DATE "
                +
                "FROM PMH_GROUP_CATEGORY " +
                "WHERE IS_ACTIVE = 1 " +
                "ORDER BY PARAM_TYPE, PARAM_NAME";

        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", row[0]);
            map.put("paramName", row[1]);
            map.put("paramValue", row[2]);
            map.put("paramType", row[3]);
            map.put("description", row[4]);
            map.put("status", row[5]);
            map.put("isActive", row[6]);
            map.put("effectiveDate", row[7]);
            map.put("endEffectiveDate", row[8]);
            result.add(map);
        }
        return result;
    }

    // ─── Batch Approve ───────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> batchApprove(List<Long> ids, String approver) {
        log.info("[GroupCategory] Batch approve started. count={}, approver={}", ids.size(), approver);
        List<Map<String, Object>> results = new ArrayList<>();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (Long id : ids) {
            results.add(approveSingleCategory(id, approver, transactionTemplate));
        }

        long successCount = results.stream().filter(r -> Boolean.TRUE.equals(r.get("success"))).count();
        log.info("[GroupCategory] Batch approve done. success={}/{}", successCount, ids.size());
        return results;
    }

    private Map<String, Object> approveSingleCategory(Long id, String approver, TransactionTemplate transactionTemplate) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                GroupCategory entity = getById(id);
                
                // Kiểm tra phân quyền: Người duyệt không được trùng với người tạo/cập nhật yêu cầu
                if (approver.equalsIgnoreCase(entity.getCreatedBy()) || approver.equalsIgnoreCase(entity.getUpdatedBy())) {
                    throw new IllegalStateException("Người phê duyệt (" + approver + ") không được trùng với người tạo/cập nhật yêu cầu!");
                }

                int statusBefore = entity.getStatus();

                // Apply NEW_DATA nếu có (cập nhật đang chờ duyệt)
                if (entity.getNewData() != null && !entity.getNewData().isEmpty()) {
                    try {
                        Map<String, Object> changes = objectMapper.readValue(
                                entity.getNewData(),
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                });
                        if (changes.containsKey("paramName"))
                            entity.setParamName((String) changes.get("paramName"));
                        if (changes.containsKey("paramValue"))
                            entity.setParamValue((String) changes.get("paramValue"));
                        if (changes.containsKey("paramType"))
                            entity.setParamType((String) changes.get("paramType"));
                        if (changes.containsKey("description"))
                            entity.setDescription((String) changes.get("description"));
                        if (changes.containsKey("componentCode"))
                            entity.setComponentCode((String) changes.get("componentCode"));
                        if (changes.containsKey("isActive"))
                            entity.setIsActive((Integer) changes.get("isActive"));
                        if (changes.containsKey("effectiveDate"))
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

                StoredProcedureQuery query = entityManager
                        .createStoredProcedureQuery("PROC_APPROVE_GROUP_CATEGORY");
                query.registerStoredProcedureParameter("p_id", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_user", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_status", Integer.class, ParameterMode.OUT);
                query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);
                query.setParameter("p_id", id);
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
                    // Ghi audit log phê duyệt
                    auditLogService.log(
                            MODULE, String.valueOf(id),
                            "Phê duyệt", approver,
                            null, null,
                            String.format("Phê duyệt tham số ID=%d. SP: %s", id, spMessage),
                            statusBefore, 4);
                } else {
                    throw new RuntimeException(spMessage);
                }
            });
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage() != null ? e.getMessage() : "Lỗi thực thi");
            log.error("[GroupCategory] Batch approve failed for id={}. error={}", id, e.getMessage());
        }
        return res;
    }

    // ─── Batch Reject ────────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> batchReject(List<Long> ids, String reason, String approver) {
        log.info("[GroupCategory] Batch reject started. count={}, approver={}", ids.size(), approver);
        List<Map<String, Object>> results = new ArrayList<>();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (Long id : ids) {
            results.add(rejectSingleCategory(id, reason, approver, transactionTemplate));
        }

        long successCount = results.stream().filter(r -> Boolean.TRUE.equals(r.get("success"))).count();
        log.info("[GroupCategory] Batch reject done. success={}/{}", successCount, ids.size());
        return results;
    }

    private Map<String, Object> rejectSingleCategory(Long id, String reason, String approver, TransactionTemplate transactionTemplate) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                GroupCategory entity = getById(id);
                
                // Kiểm tra phân quyền: Người duyệt không được trùng với người tạo/cập nhật yêu cầu
                if (approver.equalsIgnoreCase(entity.getCreatedBy()) || approver.equalsIgnoreCase(entity.getUpdatedBy())) {
                    throw new IllegalStateException("Người phê duyệt (" + approver + ") không được trùng với người tạo/cập nhật yêu cầu!");
                }

                int statusBefore = entity.getStatus();

                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_REJECT_GROUP_CATEGORY");
                query.registerStoredProcedureParameter("p_id", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_user", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_status", Integer.class, ParameterMode.OUT);
                query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);
                query.setParameter("p_id", id);
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
                            MODULE, String.valueOf(id),
                            "Từ chối", approver,
                            null, null,
                            reason != null && !reason.trim().isEmpty()
                                    ? String.format("Từ chối duyệt tham số ID=%d. Lý do: %s. SP: %s", id, reason,
                                            spMessage)
                                    : String.format("Từ chối duyệt tham số ID=%d. SP: %s", id, spMessage),
                            statusBefore, 5);
                } else {
                    throw new RuntimeException(spMessage);
                }
            });
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage() != null ? e.getMessage() : "Lỗi thực thi");
            log.error("[GroupCategory] Batch reject failed for id={}. error={}", id, e.getMessage());
        }
        return res;
    }
}
