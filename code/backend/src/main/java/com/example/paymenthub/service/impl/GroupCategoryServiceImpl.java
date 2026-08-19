package com.example.paymenthub.service.impl;

import com.example.paymenthub.common.enums.ActiveStatus;
import com.example.paymenthub.common.enums.AuditAction;
import com.example.paymenthub.common.enums.DisplayStatus;
import com.example.paymenthub.common.enums.ModuleType;
import com.example.paymenthub.common.enums.ParamStatus;
import com.example.paymenthub.dto.request.GroupCategoryDTO;
import com.example.paymenthub.dto.request.GroupCategorySearchCriteria;
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
import com.example.paymenthub.common.exception.ResourceNotFoundException;
import com.example.paymenthub.common.exception.ForbiddenAccessException;
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

    private static final String MODULE = ModuleType.GROUP_CATEGORY.getCode();

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

    // ─── Helper: compute active status from effective dates ─────────────────
    public static int computeActiveStatus(LocalDateTime effectiveDate, LocalDateTime endEffectiveDate) {
        if (effectiveDate == null) return ActiveStatus.INACTIVE.getCode();
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(effectiveDate)) return ActiveStatus.INACTIVE.getCode(); // Chưa đến ngày hiệu lực
        if (endEffectiveDate != null && now.isAfter(endEffectiveDate)) return ActiveStatus.INACTIVE.getCode(); // Đã quá ngày hết hiệu lực
        return ActiveStatus.ACTIVE.getCode(); // Đang trong khoảng hiệu lực
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
    public Page<GroupCategory> search(GroupCategorySearchCriteria criteria, Pageable pageable) {
        Specification<GroupCategory> spec = GroupCategorySpecification.filter(
                criteria.getParamType(), criteria.getParamValue(), criteria.getParamName(),
                criteria.getStatus(), criteria.getIsActive());
        return repository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupCategory getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục có ID: " + id));
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    @Override
    public GroupCategory create(GroupCategoryDTO dto, String username) {
        log.info("[GroupCategory] Creating. user={}, paramType={}, paramValue={}", username, dto.getParamType(),
                dto.getParamValue());

        if (repository.existsOverlapping(
                dto.getParamName(), dto.getParamType(),
                dto.getEffectiveDate(), dto.getEndEffectiveDate(), null)) {
            throw new IllegalStateException(
                    "Đã tồn tại cấu hình có cùng Tên và Nhóm bị chồng lấn thời gian hiệu lực!");
        }

        GroupCategory entity = GroupCategory.builder()
                .paramName(dto.getParamName())
                .paramValue(dto.getParamValue())
                .paramType(dto.getParamType())
                .description(dto.getDescription())
                .componentCode(dto.getComponentCode())
                .status(ParamStatus.NEW.getCode())
                .isActive(computeActiveStatus(dto.getEffectiveDate(), dto.getEndEffectiveDate()))
                .isDisplay(DisplayStatus.INITIAL.getCode())
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
                AuditAction.CREATE.getActionName(), username,
                null, toJson(snapshot(saved)),
                String.format("Tạo mới tham số: %s / %s / %s", saved.getParamName(), saved.getParamValue(),
                        saved.getParamType()),
                null, ParamStatus.NEW.getCode());

        return saved;
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    @Override
    public GroupCategory update(Long id, GroupCategoryDTO dto, String username) {
        GroupCategory entity = getById(id);
        String oldJson = toJson(snapshot(entity));
        int statusBefore = entity.getStatus();

        // 1. Kiểm tra trạng thái được phép sửa: Tạo mới (1), Từ chối (5), Hủy duyệt (7)
        if (entity.getStatus() != ParamStatus.NEW.getCode() 
                && entity.getStatus() != ParamStatus.REJECTED.getCode() 
                && entity.getStatus() != ParamStatus.CANCELED.getCode()) {
            throw new IllegalStateException("Không được phép chỉnh sửa bản ghi đang ở trạng thái: " 
                + (entity.getStatus() == ParamStatus.PENDING.getCode() ? "Chờ duyệt" : "Đã duyệt"));
        }

        if (repository.existsOverlapping(
                dto.getParamName(), dto.getParamType(),
                dto.getEffectiveDate(), dto.getEndEffectiveDate(), id)) {
            throw new IllegalStateException(
                    "Đã tồn tại cấu hình khác có cùng Tên và Nhóm bị chồng lấn thời gian hiệu lực!");
        }

        GroupCategory saved;
        String action;
        int statusAfter;

        if (entity.getIsDisplay() == DisplayStatus.ONCE_APPROVED.getCode()) {
            // Bản ghi đã từng được duyệt (isDisplay == 2): không sửa trực tiếp vào các cột, chỉ lưu vào NEW_DATA
            entity.setNewData(buildNewDataJson(dto));
            entity.setUpdatedBy(username);
            entity.setStatus(ParamStatus.CANCELED.getCode()); // Khi sửa một thay đổi của bản ghi đã duyệt, đặt trạng thái về Hủy duyệt (7) để Maker gửi duyệt lại
            saved = repository.save(entity);
            action = "Lưu sửa nháp (Hủy duyệt)";
            statusAfter = ParamStatus.CANCELED.getCode();
        } else {
            // Bản ghi chưa từng được duyệt (isDisplay == 1): cập nhật trực tiếp vào các cột
            updateEntityFields(entity, dto, username);
            entity.setNewData(null); // Xóa các thay đổi nháp cũ nếu có
            saved = repository.save(entity);
            action = AuditAction.UPDATE.getActionName();
            statusAfter = ParamStatus.NEW.getCode();
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
            changes.put("effectiveDate", dto.getEffectiveDate() != null ? dto.getEffectiveDate().toString() : null);
            changes.put("endEffectiveDate",
                    dto.getEndEffectiveDate() != null ? dto.getEndEffectiveDate().toString() : null);
            changes.put("isActive", computeActiveStatus(dto.getEffectiveDate(), dto.getEndEffectiveDate()));
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
        entity.setIsActive(computeActiveStatus(dto.getEffectiveDate(), dto.getEndEffectiveDate()));
        entity.setUpdatedBy(username);
        entity.setStatus(ParamStatus.NEW.getCode());
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id, String username) {
        log.info("[GroupCategory] Deleting. id={}, user={}", id, username);
        
        // Phân quyền: Yêu cầu phải có định danh người dùng (username)
        if (username == null || username.trim().isEmpty()) {
            throw new ForbiddenAccessException("Yêu cầu cần có định danh người dùng (username)!");
        }

        GroupCategory entity = getById(id);

        if (entity.getIsDisplay() == DisplayStatus.ONCE_APPROVED.getCode()) {
            // Bản ghi đã được duyệt trước đó (isDisplay == 2):
            // Nếu bản ghi đang có thay đổi chờ duyệt (status = 3) hoặc bị từ chối (status = 5):
            // Hành động xóa sẽ đóng vai trò hủy bỏ yêu cầu chỉnh sửa này và khôi phục về bản duyệt cũ.
            if (entity.getStatus() == ParamStatus.PENDING.getCode() || entity.getStatus() == ParamStatus.REJECTED.getCode()) {
                String oldJson = toJson(snapshot(entity));
                entity.setNewData(null);
                entity.setStatus(ParamStatus.APPROVED.getCode()); // Khôi phục trạng thái đã phê duyệt
                entity.setUpdatedBy(username);
                GroupCategory saved = repository.save(entity);
                
                // Ghi audit log
                auditLogService.log(
                        MODULE, String.valueOf(id),
                        "Hủy yêu cầu sửa", username,
                        oldJson, toJson(snapshot(saved)),
                        String.format("Hủy yêu cầu chỉnh sửa và khôi phục trạng thái đã duyệt cho ID=%d", id),
                        ParamStatus.PENDING.getCode(), ParamStatus.APPROVED.getCode());
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
                AuditAction.DELETE.getActionName(), username,
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
        
        if (entity.getStatus() == ParamStatus.PENDING.getCode()) {
            throw new IllegalStateException("Bản ghi đã ở trạng thái Chờ duyệt!");
        }
        
        int statusBefore = entity.getStatus();

        entity.setStatus(ParamStatus.PENDING.getCode());
        entity.setUpdatedBy(username);
        GroupCategory saved = repository.save(entity);

        // Ghi audit log
        auditLogService.log(
                MODULE, String.valueOf(id),
                AuditAction.SEND_APPROVAL.getActionName(), username,
                null, null,
                String.format("Gửi duyệt tham số ID=%d: %s / %s", id, entity.getParamName(), entity.getParamValue()),
                statusBefore, ParamStatus.PENDING.getCode());

        return saved;
    }

    // ─── Cancel Approval ─────────────────────────────────────────────────────

    @Override
    public GroupCategory cancelApproval(Long id, String username) {
        log.info("[GroupCategory] Canceling approval. id={}, user={}", id, username);
        
        // 1. Phân quyền: Yêu cầu phải có định danh người dùng (username)
        if (username == null || username.trim().isEmpty()) {
            throw new ForbiddenAccessException("Yêu cầu cần có định danh người dùng (username)!");
        }

        GroupCategory entity = getById(id);
        if (entity.getStatus() != ParamStatus.APPROVED.getCode()) {
            throw new IllegalStateException("Chỉ được phép hủy duyệt bản ghi đang ở trạng thái đã phê duyệt (STATUS = 4)!");
        }

        int statusBefore = entity.getStatus();

        entity.setStatus(ParamStatus.CANCELED.getCode()); // Hủy duyệt
        entity.setUpdatedBy(username); // Lưu định danh người sửa vào DB
        GroupCategory saved = repository.save(entity);

        // Ghi audit log kiểm toán với đích danh cá nhân (username) thực hiện
        auditLogService.log(
                MODULE, String.valueOf(id),
                AuditAction.CANCEL_APPROVAL.getActionName(), username,
                null, null,
                String.format("Hủy duyệt tham số ID=%d: %s / %s bởi %s", id, entity.getParamName(), entity.getParamValue(), username),
                statusBefore, ParamStatus.CANCELED.getCode());

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
                    throw new ForbiddenAccessException("Người phê duyệt (" + approver + ") không được trùng với người tạo/cập nhật yêu cầu!");
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
                        if (changes.containsKey("effectiveDate") && changes.get("effectiveDate") != null
                                && !((String) changes.get("effectiveDate")).trim().isEmpty())
                            entity.setEffectiveDate(LocalDateTime.parse((String) changes.get("effectiveDate")));
                        if (changes.containsKey("endEffectiveDate") && changes.get("endEffectiveDate") != null
                                && !((String) changes.get("endEffectiveDate")).trim().isEmpty())
                            entity.setEndEffectiveDate(
                                    LocalDateTime.parse((String) changes.get("endEffectiveDate")));
                        entity.setIsActive(computeActiveStatus(entity.getEffectiveDate(), entity.getEndEffectiveDate()));
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
                            AuditAction.APPROVE.getActionName(), approver,
                            null, null,
                            String.format("Phê duyệt tham số ID=%d. SP: %s", id, spMessage),
                            statusBefore, ParamStatus.APPROVED.getCode());
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
                    throw new ForbiddenAccessException("Người phê duyệt (" + approver + ") không được trùng với người tạo/cập nhật yêu cầu!");
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
                            AuditAction.REJECT.getActionName(), approver,
                            null, null,
                            reason != null && !reason.trim().isEmpty()
                                     ? String.format("Từ chối duyệt tham số ID=%d. Lý do: %s. SP: %s", id, reason,
                                            spMessage)
                                    : String.format("Từ chối duyệt tham số ID=%d. SP: %s", id, spMessage),
                            statusBefore, ParamStatus.REJECTED.getCode());
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
