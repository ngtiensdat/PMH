package com.example.paymenthub.service.impl;

import com.example.paymenthub.common.enums.ActiveStatus;
import com.example.paymenthub.common.enums.AuditAction;
import com.example.paymenthub.common.enums.BusinessErrorCode;
import com.example.paymenthub.common.enums.ModuleType;
import com.example.paymenthub.common.enums.ParamStatus;
import com.example.paymenthub.common.exception.BusinessRuleException;
import com.example.paymenthub.common.exception.InvalidStateTransitionException;
import com.example.paymenthub.common.exception.ResourceNotFoundException;
import com.example.paymenthub.common.util.DateUtils;
import com.example.paymenthub.dto.request.GroupCategoryDTO;
import com.example.paymenthub.dto.request.GroupCategorySearchCriteria;
import com.example.paymenthub.dto.response.BatchItemResultDTO;
import com.example.paymenthub.entity.GroupCategory;
import com.example.paymenthub.entity.ProcessingComponent;
import com.example.paymenthub.mapper.GroupCategoryMapper;
import com.example.paymenthub.repository.ComponentRepository;
import com.example.paymenthub.repository.GroupCategoryRepository;
import com.example.paymenthub.repository.specification.GroupCategorySpecification;
import com.example.paymenthub.service.AuditLogService;
import com.example.paymenthub.service.GroupCategoryService;
import com.example.paymenthub.common.base.AbstractMakerCheckerService;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class GroupCategoryServiceImpl extends AbstractMakerCheckerService implements GroupCategoryService {

    private static final String MODULE = ModuleType.GROUP_CATEGORY.getCode();

    private final GroupCategoryRepository repository;
    private final ComponentRepository componentRepository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final GroupCategoryMapper groupCategoryMapper;

    public GroupCategoryServiceImpl(GroupCategoryRepository repository,
            ComponentRepository componentRepository,
            ObjectMapper objectMapper,
            AuditLogService auditLogService,
            PlatformTransactionManager transactionManager,
            EntityManager entityManager,
            GroupCategoryMapper groupCategoryMapper) {
        this.repository = repository;
        this.componentRepository = componentRepository;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.entityManager = entityManager;
        this.groupCategoryMapper = groupCategoryMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ── AbstractMakerCheckerService: provide dependencies ─────────────────────
    @Override
    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    protected AuditLogService getAuditLogService() {
        return auditLogService;
    }

    @Override
    protected TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    // ─── Validate componentCode ───────────────────────────────────────────────
    private void validateComponentCode(String componentCode) {
        if (!StringUtils.hasText(componentCode))
            return;
        for (String rawCode : componentCode.split(",")) {
            String code = rawCode.trim();
            if (!StringUtils.hasText(code))
                continue;
            ProcessingComponent component = componentRepository.findById(code)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Mã Cấu phần xử lý '" + code + "' không tồn tại trong hệ thống!"));
            if (!component.isApproved()) {
                throw new BusinessRuleException("Cấu phần xử lý '" + code
                        + "' đang ở trạng thái chưa được Phê duyệt (STATUS != 4)! Không thể dùng để tạo/sửa Tham số danh mục.");
            }
        }
    }

    // ─── Search ───────────────────────────────────────────────────────────────
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

    // ─── Create ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public GroupCategory create(GroupCategoryDTO dto, String username) {
        log.info("[GroupCategory] Creating. user={}, paramType={}, paramValue={}", username, dto.getParamType(),
                dto.getParamValue());

        validateComponentCode(dto.getComponentCode());
        DateUtils.validateEffectiveDates(dto.getEffectiveDate(), dto.getEndEffectiveDate());

        if (repository.existsOverlapping(dto.getParamName(), dto.getParamType(),
                dto.getEffectiveDate(), dto.getEndEffectiveDate(), null)) {
            throw new BusinessRuleException("Đã tồn tại cấu hình có cùng Tên và Nhóm bị chồng lấn thời gian hiệu lực!");
        }

        GroupCategory entity = groupCategoryMapper.toEntity(dto, username);
        GroupCategory saved = repository.save(entity);
        log.info("[GroupCategory] Created. id={}", saved.getId());

        auditLogService.log(MODULE, String.valueOf(saved.getId()),
                AuditAction.CREATE.getActionName(), username,
                null, toJson(saved),
                String.format("Tạo mới tham số: %s / %s / %s",
                        saved.getParamName(), saved.getParamValue(), saved.getParamType()),
                null, ParamStatus.NEW.getCode());
        return saved;
    }

    // ─── Update ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public GroupCategory update(Long id, GroupCategoryDTO dto, String username) {
        log.info("[GroupCategory] Updating. id={}, user={}", id, username);
        username = resolveUsername(username);

        GroupCategory entity = getById(id);
        String oldJson = toJson(entity);
        int statusBefore = entity.getStatus();

        if (entity.isPending())
            throw new InvalidStateTransitionException(BusinessErrorCode.PENDING_EDIT_NOT_ALLOWED);

        validateComponentCode(dto.getComponentCode());
        DateUtils.validateEffectiveDates(dto.getEffectiveDate(), dto.getEndEffectiveDate());

        if (repository.existsOverlapping(dto.getParamName(), dto.getParamType(),
                dto.getEffectiveDate(), dto.getEndEffectiveDate(), id)) {
            throw new BusinessRuleException(
                    "Đã tồn tại cấu hình khác có cùng Tên và Nhóm bị chồng lấn thời gian hiệu lực!");
        }

        GroupCategory saved;
        String action;
        int statusAfter;

        if (entity.isApproved() || entity.isOnceApproved()) {
            if (!isDtoDifferentFromEntity(entity, dto))
                throw new BusinessRuleException(BusinessErrorCode.DATA_UNCHANGED_UPDATE);
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

        auditLogService.log(MODULE, String.valueOf(id), action, username,
                oldJson, toJson(saved),
                String.format("Cập nhật tham số ID=%d: %s / %s bởi %s",
                        id, saved.getParamName(), saved.getParamValue(), username),
                statusBefore, statusAfter);
        return saved;
    }

    // ─── Delete ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void delete(Long id, String username) {
        log.info("[GroupCategory] Deleting. id={}, user={}", id, username);
        username = resolveUsername(username);

        GroupCategory entity = getById(id);

        if (entity.isOnceApproved())
            throw new BusinessRuleException(BusinessErrorCode.APPROVED_RECORD_DELETE_NOT_ALLOWED);
        if (entity.isPending())
            throw new InvalidStateTransitionException(BusinessErrorCode.PENDING_RECORD_DELETE_NOT_ALLOWED);

        String oldJson = toJson(entity);
        repository.delete(entity);
        log.info("[GroupCategory] Deleted. id={}", id);

        auditLogService.log(MODULE, String.valueOf(id),
                AuditAction.DELETE.getActionName(), username,
                oldJson, null,
                String.format("Xóa tham số ID=%d: %s / %s bởi %s",
                        id, entity.getParamName(), entity.getParamValue(), username),
                entity.getStatus(), null);
    }

    // ─── Send For Approval ────────────────────────────────────────────────────
    @Override
    @Transactional
    public GroupCategory sendForApproval(Long id, String username) {
        log.info("[GroupCategory] Sending for approval. id={}, user={}", id, username);
        username = resolveUsername(username);

        GroupCategory entity = getById(id);

        if (!entity.isCanBeSubmitted())
            throw new InvalidStateTransitionException(BusinessErrorCode.INVALID_SUBMIT_STATUS);
        if (entity.isOnceApproved() && !StringUtils.hasText(entity.getNewData()))
            throw new BusinessRuleException(BusinessErrorCode.DATA_UNCHANGED_SUBMIT);

        DateUtils.validateEffectiveDates(entity.getEffectiveDate(), entity.getEndEffectiveDate());

        if (repository.existsOverlapping(entity.getParamName(), entity.getParamType(),
                entity.getEffectiveDate(), entity.getEndEffectiveDate(), entity.getId())) {
            throw new BusinessRuleException(
                    "Đã tồn tại cấu hình khác có cùng Tên và Nhóm bị chồng lấn thời gian hiệu lực!");
        }

        int statusBefore = entity.getStatus();
        entity.setStatus(ParamStatus.PENDING.getCode());
        entity.setUpdatedBy(username);
        GroupCategory saved = repository.save(entity);

        auditLogService.log(MODULE, String.valueOf(id),
                AuditAction.SEND_APPROVAL.getActionName(), username, null, null,
                String.format("Gửi duyệt tham số ID=%d bởi %s: %s / %s",
                        id, username, entity.getParamName(), entity.getParamValue()),
                statusBefore, ParamStatus.PENDING.getCode());
        return saved;
    }

    // ─── Cancel Approval ──────────────────────────────────────────────────────
    @Override
    @Transactional
    public GroupCategory cancelApproval(Long id, String username) {
        log.info("[GroupCategory] Canceling approval. id={}, user={}", id, username);
        username = resolveUsername(username);

        GroupCategory entity = getById(id);
        if (!entity.isApproved())
            throw new InvalidStateTransitionException(BusinessErrorCode.INVALID_SUBMIT_STATUS);

        int statusBefore = entity.getStatus();
        entity.setStatus(ParamStatus.CANCELED.getCode());
        entity.setUpdatedBy(username);
        GroupCategory saved = repository.save(entity);

        auditLogService.log(MODULE, String.valueOf(id),
                AuditAction.CANCEL_APPROVAL.getActionName(), username, null, null,
                String.format("Hủy duyệt tham số ID=%d: %s / %s bởi %s",
                        id, entity.getParamName(), entity.getParamValue(), username),
                statusBefore, ParamStatus.CANCELED.getCode());
        return saved;
    }

    // ─── Joined List (Native Query + JOIN) ────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getJoinedList() {
        String sql = """
                SELECT gc.ID as "id",
                       gc.PARAM_NAME as "paramName",
                       gc.PARAM_VALUE as "paramValue",
                       gc.PARAM_TYPE as "paramType",
                       gc.DESCRIPTION as "description",
                       gc.COMPONENT_CODE as "componentCode",
                       COALESCE(c.COMPONENT_NAME, 'Chưa xác định') as "componentName",
                       gc.STATUS as "status",
                       gc.IS_ACTIVE as "isActive",
                       gc.EFFECTIVE_DATE as "effectiveDate",
                       gc.END_EFFECTIVE_DATE as "endEffectiveDate",
                       gc.CREATED_BY as "createdBy",
                       gc.CREATED_DATE as "createdDate",
                       gc.UPDATED_BY as "updatedBy",
                       gc.UPDATED_DATE as "updatedDate"
                FROM PMH_GROUP_CATEGORY gc
                LEFT JOIN PMH_COMPONENTS c ON gc.COMPONENT_CODE = c.COMPONENT_CODE
                ORDER BY COALESCE(gc.UPDATED_DATE, gc.CREATED_DATE) DESC, gc.ID DESC
                """;

        List<Tuple> tuples = entityManager.createNativeQuery(sql, Tuple.class)
                .setMaxResults(1000)
                .getResultList();

        Map<String, String> componentNameCache = componentRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProcessingComponent::getComponentCode,
                        ProcessingComponent::getComponentName,
                        (existing, replacement) -> existing));

        return tuples.stream().map(tuple -> {
            Map<String, Object> map = new HashMap<>();
            tuple.getElements().forEach(elem -> {
                String alias = elem.getAlias();
                if (alias != null) {
                    map.put(alias, tuple.get(elem));
                }
            });

            Object compCodeObj = map.get("componentCode") != null ? map.get("componentCode") : map.get("COMPONENTCODE");
            String compCode = compCodeObj != null ? compCodeObj.toString() : null;
            if (StringUtils.hasText(compCode)) {
                List<String> names = new ArrayList<>();
                for (String c : compCode.split(",")) {
                    String trimmed = c.trim();
                    if (!trimmed.isEmpty()) {
                        String foundName = componentNameCache.get(trimmed);
                        names.add(foundName != null ? foundName : trimmed);
                    }
                }
                if (!names.isEmpty())
                    map.put("componentName", String.join(", ", names));
            }
            return map;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRawDataForExport() {
        List<GroupCategory> activeList = repository
                .findByIsActiveOrderByParamTypeAscParamNameAsc(ActiveStatus.ACTIVE.getCode());
        return activeList.stream()
                .map(entity -> objectMapper.convertValue(entity, new TypeReference<Map<String, Object>>() {
                }))
                .toList();
    }

    // ─── Batch Operations ─────────────────────────────────────────────────────
    @Override
    public List<BatchItemResultDTO> batchApprove(List<Long> ids, String approver) {
        log.info("[GroupCategory] Batch approve started. count={}, approver={}", ids.size(), approver);
        List<BatchItemResultDTO> results = ids.stream()
                .map(id -> runBatchItem(id, (entity, result) -> approveCategoryAction(entity, approver, result)))
                .toList();
        log.info("[GroupCategory] Batch approve done. success={}/{}",
                results.stream().filter(r -> BATCH_SUCCESS.equalsIgnoreCase(r.getStatus())).count(), ids.size());
        return results;
    }

    @Override
    public List<BatchItemResultDTO> batchReject(List<Long> ids, String reason, String approver) {
        log.info("[GroupCategory] Batch reject started. count={}, approver={}", ids.size(), approver);
        List<BatchItemResultDTO> results = ids.stream()
                .map(id -> runBatchItem(id, (entity, result) -> rejectCategoryAction(entity, reason, approver, result)))
                .toList();
        log.info("[GroupCategory] Batch reject done. success={}/{}",
                results.stream().filter(r -> BATCH_SUCCESS.equalsIgnoreCase(r.getStatus())).count(), ids.size());
        return results;
    }

    /** Delegate sang base executeBatchItem với id và entity supplier cụ thể */
    private BatchItemResultDTO runBatchItem(Long id, BatchActionConsumer<GroupCategory> action) {
        return executeBatchItem(
                BatchItemResultDTO.builder().id(id).build(),
                () -> getById(id),
                action,
                id);
    }

    private void approveCategoryAction(GroupCategory entity, String approver, BatchItemResultDTO result) {
        validateMakerChecker(entity, approver, BusinessErrorCode.INVALID_APPROVE_STATUS);

        int statusBefore = entity.getStatus();
        if (StringUtils.hasText(entity.getNewData()))
            applyNewDataChanges(entity);

        StoredProcedureResult spResult = executeCategoryStoredProcedure("PROC_APPROVE_GROUP_CATEGORY", entity.getId(),
                approver);
        if (!spResult.isSuccess())
            throw new BusinessRuleException(spResult.getMessage());

        entityManager.refresh(entity);
        result.setStatus(BATCH_SUCCESS);
        result.setErrorMessage(spResult.getMessage());
        auditLogService.log(MODULE, String.valueOf(entity.getId()),
                AuditAction.APPROVE.getActionName(), approver, null, null,
                String.format("Phê duyệt tham số ID=%d: %s / %s bởi %s",
                        entity.getId(), entity.getParamName(), entity.getParamValue(), approver),
                statusBefore, ParamStatus.APPROVED.getCode());
    }

    private void rejectCategoryAction(GroupCategory entity, String reason, String approver, BatchItemResultDTO result) {
        validateMakerChecker(entity, approver, BusinessErrorCode.INVALID_REJECT_STATUS);

        int statusBefore = entity.getStatus();
        StoredProcedureResult spResult = executeCategoryStoredProcedure("PROC_REJECT_GROUP_CATEGORY", entity.getId(),
                approver);
        if (!spResult.isSuccess())
            throw new BusinessRuleException(spResult.getMessage());

        entityManager.refresh(entity);
        result.setStatus(BATCH_SUCCESS);
        result.setErrorMessage(spResult.getMessage());
        auditLogService.log(MODULE, String.valueOf(entity.getId()),
                AuditAction.REJECT.getActionName(), approver, null, null,
                StringUtils.hasText(reason)
                        ? String.format("Từ chối duyệt tham số ID=%d: %s / %s bởi %s. Lý do: %s",
                                entity.getId(), entity.getParamName(), entity.getParamValue(), approver, reason.trim())
                        : String.format("Từ chối duyệt tham số ID=%d: %s / %s bởi %s",
                                entity.getId(), entity.getParamName(), entity.getParamValue(), approver),
                statusBefore, ParamStatus.REJECTED.getCode());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private void updateEntityFields(GroupCategory entity, GroupCategoryDTO dto, String username) {
        entity.setParamName(dto.getParamName());
        entity.setParamValue(dto.getParamValue());
        entity.setParamType(dto.getParamType());
        entity.setDescription(dto.getDescription());
        entity.setComponentCode(dto.getComponentCode());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setEndEffectiveDate(dto.getEndEffectiveDate());
        entity.updateActiveStatus();
        entity.setUpdatedBy(username);
        entity.setStatus(ParamStatus.NEW.getCode());
    }

    private void applyNewDataChanges(GroupCategory entity) {
        try {
            GroupCategoryDTO changes = getObjectMapper().readValue(entity.getNewData(), GroupCategoryDTO.class);
            if (changes.getParamName() != null)
                entity.setParamName(changes.getParamName());
            if (changes.getParamValue() != null)
                entity.setParamValue(changes.getParamValue());
            if (changes.getParamType() != null)
                entity.setParamType(changes.getParamType());
            entity.setDescription(changes.getDescription());
            if (changes.getComponentCode() != null)
                entity.setComponentCode(changes.getComponentCode());
            if (changes.getEffectiveDate() != null)
                entity.setEffectiveDate(changes.getEffectiveDate());
            entity.setEndEffectiveDate(changes.getEndEffectiveDate());
            entity.updateActiveStatus();
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
        return new StoredProcedureResult(
                spStatusObj instanceof Number && ((Number) spStatusObj).intValue() == 1,
                spMessage);
    }

    private boolean isDtoDifferentFromEntity(GroupCategory entity, GroupCategoryDTO dto) {
        return !Objects.equals(entity.getParamName(), dto.getParamName())
                || !Objects.equals(entity.getParamType(), dto.getParamType())
                || !Objects.equals(entity.getParamValue(), dto.getParamValue())
                || !Objects.equals(entity.getDescription(), dto.getDescription())
                || !Objects.equals(entity.getComponentCode(), dto.getComponentCode())
                || !Objects.equals(entity.getEffectiveDate(), dto.getEffectiveDate())
                || !Objects.equals(entity.getEndEffectiveDate(), dto.getEndEffectiveDate());
    }
}
