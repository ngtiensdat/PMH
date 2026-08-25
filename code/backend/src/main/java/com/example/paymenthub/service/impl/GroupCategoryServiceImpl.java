package com.example.paymenthub.service.impl;

import com.example.paymenthub.common.enums.ActiveStatus;
import com.example.paymenthub.common.enums.AuditAction;
import com.example.paymenthub.common.enums.BusinessErrorCode;
import com.example.paymenthub.common.enums.ModuleType;
import com.example.paymenthub.common.enums.ParamStatus;
import com.example.paymenthub.dto.request.GroupCategoryDTO;
import com.example.paymenthub.dto.request.GroupCategorySearchCriteria;
import com.example.paymenthub.entity.GroupCategory;
import com.example.paymenthub.mapper.GroupCategoryMapper;
import com.example.paymenthub.repository.GroupCategoryRepository;
import com.example.paymenthub.service.GroupCategoryService;
import com.example.paymenthub.repository.specification.GroupCategorySpecification;
import com.example.paymenthub.service.AuditLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.paymenthub.common.exception.ResourceNotFoundException;
import com.example.paymenthub.common.exception.BusinessRuleException;
import com.example.paymenthub.common.exception.MakerCheckerConflictException;
import com.example.paymenthub.common.exception.InvalidStateTransitionException;
import com.example.paymenthub.dto.response.BatchItemResultDTO;
import com.example.paymenthub.common.util.DateUtils;
import com.example.paymenthub.security.SecurityUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class GroupCategoryServiceImpl implements GroupCategoryService {

    private static final String MODULE = ModuleType.GROUP_CATEGORY.getCode();
    private static final String BATCH_SUCCESS = "SUCCESS";
    private static final String BATCH_FAILED = "FAILED";

    private final GroupCategoryRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final GroupCategoryMapper groupCategoryMapper;

    public GroupCategoryServiceImpl(GroupCategoryRepository repository,
            ObjectMapper objectMapper,
            AuditLogService auditLogService,
            PlatformTransactionManager transactionManager,
            EntityManager entityManager,
            GroupCategoryMapper groupCategoryMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.entityManager = entityManager;
        this.groupCategoryMapper = groupCategoryMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ─── Helper: compute active status from effective dates ─────────────────
    public static int computeActiveStatus(LocalDateTime effectiveDate, LocalDateTime endEffectiveDate) {
        if (effectiveDate == null) return ActiveStatus.INACTIVE.getCode();
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(effectiveDate)) return ActiveStatus.INACTIVE.getCode();
        if (endEffectiveDate != null && now.isAfter(endEffectiveDate)) return ActiveStatus.INACTIVE.getCode();
        return ActiveStatus.ACTIVE.getCode();
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
    @Transactional
    public GroupCategory create(GroupCategoryDTO dto, String username) {
        log.info("[GroupCategory] Creating. user={}, paramType={}, paramValue={}", username, dto.getParamType(), dto.getParamValue());

        DateUtils.validateEffectiveDates(dto.getEffectiveDate(), dto.getEndEffectiveDate());

        if (repository.existsOverlapping(
                dto.getParamName(), dto.getParamType(),
                dto.getEffectiveDate(), dto.getEndEffectiveDate(), null)) {
            throw new BusinessRuleException("Đã tồn tại cấu hình có cùng Tên và Nhóm bị chồng lấn thời gian hiệu lực!");
        }

        GroupCategory entity = groupCategoryMapper.toEntity(dto, username);
        GroupCategory saved = repository.save(entity);
        log.info("[GroupCategory] Created. id={}", saved.getId());

        auditLogService.log(
                MODULE, String.valueOf(saved.getId()),
                AuditAction.CREATE.getActionName(), username,
                null, toJson(saved),
                String.format("Tạo mới tham số: %s / %s / %s", saved.getParamName(), saved.getParamValue(), saved.getParamType()),
                null, ParamStatus.NEW.getCode());

        return saved;
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public GroupCategory update(Long id, GroupCategoryDTO dto, String username) {
        log.info("[GroupCategory] Updating. id={}, user={}", id, username);
        username = resolveUsername(username);

        GroupCategory entity = getById(id);
        String oldJson = toJson(entity);
        int statusBefore = entity.getStatus();

        if (entity.isPending()) {
            throw new InvalidStateTransitionException(BusinessErrorCode.PENDING_EDIT_NOT_ALLOWED);
        }

        DateUtils.validateEffectiveDates(dto.getEffectiveDate(), dto.getEndEffectiveDate());

        if (repository.existsOverlapping(
                dto.getParamName(), dto.getParamType(),
                dto.getEffectiveDate(), dto.getEndEffectiveDate(), id)) {
            throw new BusinessRuleException("Đã tồn tại cấu hình khác có cùng Tên và Nhóm bị chồng lấn thời gian hiệu lực!");
        }

        GroupCategory saved;
        String action;
        int statusAfter;

        if (entity.isApproved() || entity.isOnceApproved()) {
            if (!isDtoDifferentFromEntity(entity, dto)) {
                throw new BusinessRuleException(BusinessErrorCode.DATA_UNCHANGED_UPDATE);
            }
            entity.setNewData(toJson(dto));
            entity.setUpdatedBy(username);
            entity.setStatus(ParamStatus.CANCELED.getCode());
            saved = repository.save(entity);
            action = AuditAction.UPDATE.getActionName();
            statusAfter = ParamStatus.CANCELED.getCode();
        } else {
            updateEntityFields(entity, dto, username);
            entity.setNewData(null);
            saved = repository.save(entity);
            action = AuditAction.UPDATE.getActionName();
            statusAfter = ParamStatus.NEW.getCode();
        }

        String newJson = toJson(saved);

        auditLogService.log(
                MODULE, String.valueOf(id),
                action, username,
                oldJson, newJson,
                String.format("Cập nhật tham số ID=%d: %s / %s bởi %s", id, saved.getParamName(), saved.getParamValue(), username),
                statusBefore, statusAfter);

        return saved;
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void delete(Long id, String username) {
        log.info("[GroupCategory] Deleting. id={}, user={}", id, username);
        username = resolveUsername(username);

        GroupCategory entity = getById(id);

        if (entity.isOnceApproved()) {
            throw new BusinessRuleException(BusinessErrorCode.APPROVED_RECORD_DELETE_NOT_ALLOWED);
        }

        if (entity.isPending()) {
            throw new InvalidStateTransitionException(BusinessErrorCode.PENDING_RECORD_DELETE_NOT_ALLOWED);
        }

        String oldJson = toJson(entity);
        repository.delete(entity);
        log.info("[GroupCategory] Deleted. id={}", id);

        auditLogService.log(
                MODULE, String.valueOf(id),
                AuditAction.DELETE.getActionName(), username,
                oldJson, null,
                String.format("Xóa tham số ID=%d: %s / %s bởi %s", id, entity.getParamName(), entity.getParamValue(), username),
                entity.getStatus(), null);
    }

    // ─── Send For Approval ───────────────────────────────────────────────────

    @Override
    @Transactional
    public GroupCategory sendForApproval(Long id, String username) {
        log.info("[GroupCategory] Sending for approval. id={}, user={}", id, username);
        username = resolveUsername(username);

        GroupCategory entity = getById(id);

        if (!entity.isCanBeSubmitted()) {
            throw new InvalidStateTransitionException(BusinessErrorCode.INVALID_SUBMIT_STATUS);
        }

        if (entity.isOnceApproved()
                && (entity.getNewData() == null || entity.getNewData().trim().isEmpty())) {
            throw new BusinessRuleException(BusinessErrorCode.DATA_UNCHANGED_SUBMIT);
        }

        DateUtils.validateEffectiveDates(entity.getEffectiveDate(), entity.getEndEffectiveDate());

        if (repository.existsOverlapping(
                entity.getParamName(), entity.getParamType(),
                entity.getEffectiveDate(), entity.getEndEffectiveDate(), entity.getId())) {
            throw new BusinessRuleException("Đã tồn tại cấu hình khác có cùng Tên và Nhóm bị chồng lấn thời gian hiệu lực!");
        }

        int statusBefore = entity.getStatus();

        entity.setStatus(ParamStatus.PENDING.getCode());
        entity.setUpdatedBy(username);
        GroupCategory saved = repository.save(entity);

        auditLogService.log(
                MODULE, String.valueOf(id),
                AuditAction.SEND_APPROVAL.getActionName(), username,
                null, null,
                String.format("Gửi duyệt tham số ID=%d bởi %s: %s / %s", id, username, entity.getParamName(), entity.getParamValue()),
                statusBefore, ParamStatus.PENDING.getCode());

        return saved;
    }

    // ─── Cancel Approval ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public GroupCategory cancelApproval(Long id, String username) {
        log.info("[GroupCategory] Canceling approval. id={}, user={}", id, username);
        username = resolveUsername(username);

        GroupCategory entity = getById(id);
        if (!entity.isApproved()) {
            throw new InvalidStateTransitionException(BusinessErrorCode.INVALID_SUBMIT_STATUS);
        }

        int statusBefore = entity.getStatus();

        entity.setStatus(ParamStatus.CANCELED.getCode());
        entity.setUpdatedBy(username);
        GroupCategory saved = repository.save(entity);

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
        String sql = """
                SELECT gc.ID as id,
                       gc.PARAM_NAME as paramName,
                       gc.PARAM_VALUE as paramValue,
                       gc.PARAM_TYPE as paramType,
                       gc.DESCRIPTION as description,
                       gc.COMPONENT_CODE as componentCode,
                       COALESCE(c.COMPONENT_NAME, 'Chưa xác định') as componentName,
                       gc.STATUS as status,
                       gc.IS_ACTIVE as isActive,
                       gc.EFFECTIVE_DATE as effectiveDate
                FROM PMH_GROUP_CATEGORY gc
                LEFT JOIN PMH_COMPONENTS c ON gc.COMPONENT_CODE = c.COMPONENT_CODE
                ORDER BY gc.UPDATED_DATE DESC
                """;

        List<Tuple> tuples = entityManager.createNativeQuery(sql, Tuple.class)
                .setMaxResults(1000)
                .getResultList();

        return tuples.stream().map(tuple -> {
            Map<String, Object> map = new HashMap<>();
            tuple.getElements().forEach(elem -> map.put(elem.getAlias(), tuple.get(elem)));
            return map;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRawDataForExport() {
        List<GroupCategory> activeList = repository.findByIsActiveOrderByParamTypeAscParamNameAsc(ActiveStatus.ACTIVE.getCode());
        return activeList.stream()
                .map(entity -> objectMapper.convertValue(entity, new TypeReference<Map<String, Object>>() {}))
                .toList();
    }

    // ─── Batch Operations: Public APIs ───────────────────────────────────────

    /**
     * Phê duyệt hàng loạt các danh mục được chọn.
     */
    @Override
    public List<BatchItemResultDTO> batchApprove(List<Long> ids, String approver) {
        log.info("[GroupCategory] Batch approve started. count={}, approver={}", ids.size(), approver);
        List<BatchItemResultDTO> results = ids.stream()
                .map(id -> executeBatchItem(id, (entity, result) -> approveCategoryAction(entity, approver, result)))
                .toList();

        long successCount = results.stream().filter(r -> BATCH_SUCCESS.equalsIgnoreCase(r.getStatus())).count();
        log.info("[GroupCategory] Batch approve done. success={}/{}", successCount, ids.size());
        return results;
    }

    /**
     * Từ chối hàng loạt các danh mục được chọn kèm lý do.
     */
    @Override
    public List<BatchItemResultDTO> batchReject(List<Long> ids, String reason, String approver) {
        log.info("[GroupCategory] Batch reject started. count={}, approver={}", ids.size(), approver);
        List<BatchItemResultDTO> results = ids.stream()
                .map(id -> executeBatchItem(id, (entity, result) -> rejectCategoryAction(entity, reason, approver, result)))
                .toList();

        long successCount = results.stream().filter(r -> BATCH_SUCCESS.equalsIgnoreCase(r.getStatus())).count();
        log.info("[GroupCategory] Batch reject done. success={}/{}", successCount, ids.size());
        return results;
    }

    // ─── Batch Operations: Shared Transaction Runner ─────────────────────────

    @FunctionalInterface
    private interface BatchActionConsumer {
        void accept(GroupCategory entity, BatchItemResultDTO result);
    }

    /**
     * Khung runner dùng chung để bọc Transaction độc lập và try-catch xử lý riêng cho từng item trong Batch.
     */
    private BatchItemResultDTO executeBatchItem(Long id, BatchActionConsumer action) {
        BatchItemResultDTO result = BatchItemResultDTO.builder().id(id).build();
        try {
            this.transactionTemplate.executeWithoutResult(status -> action.accept(getById(id), result));
        } catch (Exception e) {
            result.setStatus(BATCH_FAILED);
            result.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Lỗi thực thi");
            log.error("[GroupCategory] Batch item failed for id={}. error={}", id, e.getMessage());
        }
        return result;
    }

    // ─── Batch Operations: Specific Logic Handlers ───────────────────────────

    /**
     * Logic nghiệp vụ chi tiết cho hành động PHÊ DUYỆT 1 danh mục
     */
    private void approveCategoryAction(GroupCategory entity, String approver, BatchItemResultDTO result) {
        validateMakerChecker(entity, approver, BusinessErrorCode.INVALID_APPROVE_STATUS);

        int statusBefore = entity.getStatus();
        if (entity.getNewData() != null && !entity.getNewData().isEmpty()) {
            applyNewDataChanges(entity);
        }

        StoredProcedureResult spResult = executeCategoryStoredProcedure("PROC_APPROVE_GROUP_CATEGORY", entity.getId(), approver);
        if (!spResult.isSuccess()) {
            throw new BusinessRuleException(spResult.getMessage());
        }

        result.setStatus(BATCH_SUCCESS);
        result.setErrorMessage(spResult.getMessage());
        auditLogService.log(
                MODULE, String.valueOf(entity.getId()),
                AuditAction.APPROVE.getActionName(), approver,
                null, null,
                String.format("Phê duyệt tham số ID=%d: %s / %s bởi %s", entity.getId(), entity.getParamName(), entity.getParamValue(), approver),
                statusBefore, ParamStatus.APPROVED.getCode());
    }

    /**
     * Logic nghiệp vụ chi tiết cho hành động TỪ CHỐI 1 danh mục
     */
    private void rejectCategoryAction(GroupCategory entity, String reason, String approver, BatchItemResultDTO result) {
        validateMakerChecker(entity, approver, BusinessErrorCode.INVALID_REJECT_STATUS);

        int statusBefore = entity.getStatus();
        StoredProcedureResult spResult = executeCategoryStoredProcedure("PROC_REJECT_GROUP_CATEGORY", entity.getId(), approver);
        if (!spResult.isSuccess()) {
            throw new BusinessRuleException(spResult.getMessage());
        }

        result.setStatus(BATCH_SUCCESS);
        result.setErrorMessage(spResult.getMessage());
        auditLogService.log(
                MODULE, String.valueOf(entity.getId()),
                AuditAction.REJECT.getActionName(), approver,
                null, null,
                reason != null && !reason.trim().isEmpty()
                        ? String.format("Từ chối duyệt tham số ID=%d: %s / %s bởi %s. Lý do: %s", entity.getId(), entity.getParamName(), entity.getParamValue(), approver, reason.trim())
                        : String.format("Từ chối duyệt tham số ID=%d: %s / %s bởi %s", entity.getId(), entity.getParamName(), entity.getParamValue(), approver),
                statusBefore, ParamStatus.REJECTED.getCode());
    }

    // ─── Refactored Helper Methods ──────────────────────────────────────────

    private String resolveUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return SecurityUtils.getCurrentUsername();
        }
        return username;
    }

    private void validateMakerChecker(GroupCategory entity, String approver, BusinessErrorCode invalidStatusError) {
        if (!entity.isPending()) {
            throw new InvalidStateTransitionException(invalidStatusError);
        }
        if (approver.equalsIgnoreCase(entity.getCreatedBy())
                || approver.equalsIgnoreCase(entity.getUpdatedBy())) {
            throw new MakerCheckerConflictException(BusinessErrorCode.MAKER_CHECKER_SAME_USER);
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

    private void applyNewDataChanges(GroupCategory entity) {
        try {
            GroupCategoryDTO changes = objectMapper.readValue(entity.getNewData(), GroupCategoryDTO.class);
            if (changes.getParamName() != null) entity.setParamName(changes.getParamName());
            if (changes.getParamValue() != null) entity.setParamValue(changes.getParamValue());
            if (changes.getParamType() != null) entity.setParamType(changes.getParamType());
            entity.setDescription(changes.getDescription());
            if (changes.getComponentCode() != null) entity.setComponentCode(changes.getComponentCode());
            if (changes.getEffectiveDate() != null) entity.setEffectiveDate(changes.getEffectiveDate());
            entity.setEndEffectiveDate(changes.getEndEffectiveDate());
            entity.setIsActive(computeActiveStatus(entity.getEffectiveDate(), entity.getEndEffectiveDate()));
            entity.setNewData(null);
            repository.saveAndFlush(entity);
        } catch (Exception ex) {
            throw new BusinessRuleException(BusinessErrorCode.DATA_DECODE_ERROR, ex);
        }
    }

    private StoredProcedureResult executeCategoryStoredProcedure(String procedureName, Long id, String user) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(procedureName);
        query.registerStoredProcedureParameter("p_id", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_user", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_status", Integer.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);
        query.setParameter("p_id", id);
        query.setParameter("p_user", user);
        query.execute();

        Object spStatusObj = query.getOutputParameterValue("p_status");
        String spMessage = (String) query.getOutputParameterValue("p_message");
        boolean success = spStatusObj instanceof Number && ((Number) spStatusObj).intValue() == 1;

        return new StoredProcedureResult(success, spMessage);
    }

    private static class StoredProcedureResult {
        private final boolean success;
        private final String message;

        public StoredProcedureResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    private boolean isDtoDifferentFromEntity(GroupCategory entity, GroupCategoryDTO dto) {
        return !Objects.equals(entity.getParamName(), dto.getParamName())
                || !Objects.equals(entity.getParamType(), dto.getParamType())
                || !Objects.equals(entity.getParamValue(), dto.getParamValue())
                || !Objects.equals(entity.getDescription(), dto.getDescription())
                || !Objects.equals(entity.getComponentCode(), dto.getComponentCode())
                || !Objects.equals(entity.getIsActive(), dto.getIsActive())
                || !Objects.equals(entity.getEffectiveDate(), dto.getEffectiveDate())
                || !Objects.equals(entity.getEndEffectiveDate(), dto.getEndEffectiveDate());
    }
}
