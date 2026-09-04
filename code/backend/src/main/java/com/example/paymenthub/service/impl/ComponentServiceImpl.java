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
import com.example.paymenthub.dto.request.ComponentDTO;
import com.example.paymenthub.dto.request.ComponentSearchCriteria;
import com.example.paymenthub.dto.response.BatchItemResultDTO;
import com.example.paymenthub.entity.ProcessingComponent;
import com.example.paymenthub.mapper.ComponentMapper;
import com.example.paymenthub.repository.ComponentRepository;
import com.example.paymenthub.repository.specification.ComponentSpecification;
import com.example.paymenthub.service.AuditLogService;
import com.example.paymenthub.service.ComponentService;
import com.example.paymenthub.common.base.AbstractMakerCheckerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class ComponentServiceImpl extends AbstractMakerCheckerService implements ComponentService {

    private static final String MODULE = ModuleType.COMPONENT.getCode();

    private final ComponentRepository repository;
    private final ObjectMapper        objectMapper;
    private final AuditLogService     auditLogService;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager       entityManager;
    private final ComponentMapper     componentMapper;

    public ComponentServiceImpl(ComponentRepository repository,
            ObjectMapper objectMapper,
            AuditLogService auditLogService,
            PlatformTransactionManager transactionManager,
            EntityManager entityManager,
            ComponentMapper componentMapper) {
        this.repository          = repository;
        this.objectMapper        = objectMapper;
        this.auditLogService     = auditLogService;
        this.entityManager       = entityManager;
        this.componentMapper     = componentMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ── AbstractMakerCheckerService: provide dependencies ─────────────────────
    @Override protected ObjectMapper        getObjectMapper()        { return objectMapper; }
    @Override protected AuditLogService     getAuditLogService()     { return auditLogService; }
    @Override protected TransactionTemplate getTransactionTemplate() { return transactionTemplate; }

    // ─── Search ───────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Page<ProcessingComponent> search(ComponentSearchCriteria criteria, Pageable pageable) {
        Specification<ProcessingComponent> spec = ComponentSpecification.filter(
                criteria.getComponentCode(), criteria.getComponentName(), criteria.getMessageType(),
                criteria.getConnectionMethod(), criteria.getStatus(), criteria.getIsActive());
        return repository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessingComponent getByCode(String code) {
        return repository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu phần có mã: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessingComponent> getActiveList(Integer status) {
        List<ProcessingComponent> rawList = status != null
                ? repository.findAllByIsActiveAndStatusOrderByComponentNameAsc(ActiveStatus.ACTIVE.getCode(), status)
                : repository.findAllByIsActiveOrderByComponentNameAsc(ActiveStatus.ACTIVE.getCode());

        LocalDateTime now = LocalDateTime.now();
        return rawList.stream()
                .filter(c -> c.getEffectiveDate() != null && !c.getEffectiveDate().isAfter(now))
                .filter(c -> c.getEndEffectiveDate() == null || !c.getEndEffectiveDate().isBefore(now))
                .toList();
    }

    // ─── Create ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ProcessingComponent create(ComponentDTO dto, String username) {
        log.info("[Component] Creating. user={}, componentCode={}", username, dto.getComponentCode());

        DateUtils.validateEffectiveDates(dto.getEffectiveDate(), dto.getEndEffectiveDate());

        if (dto.getComponentCode() != null && repository.existsByComponentCode(dto.getComponentCode().toUpperCase())) {
            throw new BusinessRuleException(BusinessErrorCode.COMPONENT_CODE_EXISTS);
        }

        ProcessingComponent entity = componentMapper.toEntity(dto, username);
        ProcessingComponent saved  = repository.save(entity);
        log.info("[Component] Created. code={}", saved.getComponentCode());

        auditLogService.log(MODULE, saved.getComponentCode(),
                AuditAction.CREATE.getActionName(), username,
                null, toJson(saved),
                String.format("Tạo mới cấu phần: %s - %s", saved.getComponentCode(), saved.getComponentName()),
                null, ParamStatus.NEW.getCode());
        return saved;
    }

    // ─── Update ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ProcessingComponent update(String code, ComponentDTO dto, String username) {
        log.info("[Component] Updating. code={}, user={}", code, username);
        username = resolveUsername(username);

        ProcessingComponent entity     = getByCode(code);
        String              oldJson    = toJson(entity);
        int                 statusBefore = entity.getStatus();

        if (entity.isPending()) throw new InvalidStateTransitionException(BusinessErrorCode.PENDING_EDIT_NOT_ALLOWED);

        DateUtils.validateEffectiveDates(dto.getEffectiveDate(), dto.getEndEffectiveDate());

        ProcessingComponent saved;
        String action;
        int    statusAfter;

        if (entity.isApproved() || entity.isOnceApproved()) {
            if (!isDtoDifferentFromEntity(entity, dto)) throw new BusinessRuleException(BusinessErrorCode.DATA_UNCHANGED_UPDATE);
            entity.setNewData(toJson(dto));
            entity.setUpdatedBy(username);
            entity.setStatus(ParamStatus.CANCELED.getCode());
            saved       = repository.save(entity);
            action      = AuditAction.UPDATE.getActionName();
            statusAfter = ParamStatus.CANCELED.getCode();
        } else {
            updateEntityFromDto(entity, dto, username);
            entity.setNewData(null);
            saved       = repository.save(entity);
            action      = AuditAction.UPDATE.getActionName();
            statusAfter = ParamStatus.NEW.getCode();
        }

        auditLogService.log(MODULE, code, action, username,
                oldJson, toJson(saved),
                String.format("Cập nhật cấu phần Code=%s - %s bởi %s", code, saved.getComponentName(), username),
                statusBefore, statusAfter);
        return saved;
    }

    // ─── Delete ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void delete(String code, String username) {
        log.info("[Component] Deleting. code={}, user={}", code, username);
        username = resolveUsername(username);

        ProcessingComponent entity = getByCode(code);

        if (entity.isOnceApproved()) throw new BusinessRuleException(BusinessErrorCode.APPROVED_RECORD_DELETE_NOT_ALLOWED);
        if (entity.isPending())      throw new InvalidStateTransitionException(BusinessErrorCode.PENDING_RECORD_DELETE_NOT_ALLOWED);

        String oldJson = toJson(entity);
        repository.delete(entity);
        log.info("[Component] Deleted. code={}", code);

        auditLogService.log(MODULE, code,
                AuditAction.DELETE.getActionName(), username,
                oldJson, null,
                String.format("Xóa cấu phần chưa duyệt: %s - %s", entity.getComponentCode(), entity.getComponentName()),
                entity.getStatus(), null);
    }

    // ─── Send For Approval ────────────────────────────────────────────────────
    @Override
    @Transactional
    public ProcessingComponent sendForApproval(String code, String username) {
        log.info("[Component] Sending for approval. code={}, user={}", code, username);
        username = resolveUsername(username);

        ProcessingComponent entity = getByCode(code);

        if (!entity.isCanBeSubmitted()) throw new InvalidStateTransitionException(BusinessErrorCode.INVALID_SUBMIT_STATUS);
        if (entity.isOnceApproved() && !StringUtils.hasText(entity.getNewData()))
            throw new BusinessRuleException(BusinessErrorCode.DATA_UNCHANGED_SUBMIT);

        DateUtils.validateEffectiveDates(entity.getEffectiveDate(), entity.getEndEffectiveDate());

        int statusBefore = entity.getStatus();
        entity.setStatus(ParamStatus.PENDING.getCode());
        entity.setUpdatedBy(username);
        ProcessingComponent saved = repository.save(entity);

        auditLogService.log(MODULE, code,
                AuditAction.SEND_APPROVAL.getActionName(), username, null, null,
                String.format("Gửi duyệt cấu phần Code=%s: %s", code, entity.getComponentName()),
                statusBefore, ParamStatus.PENDING.getCode());
        return saved;
    }

    // ─── Cancel Approval ──────────────────────────────────────────────────────
    @Override
    @Transactional
    public ProcessingComponent cancelApproval(String code, String username) {
        log.info("[Component] Canceling approval. code={}, user={}", code, username);
        username = resolveUsername(username);

        ProcessingComponent entity = getByCode(code);
        if (!entity.isApproved()) throw new InvalidStateTransitionException(BusinessErrorCode.INVALID_SUBMIT_STATUS);

        int statusBefore = entity.getStatus();
        entity.setStatus(ParamStatus.CANCELED.getCode());
        entity.setUpdatedBy(username);
        ProcessingComponent saved = repository.save(entity);

        auditLogService.log(MODULE, code,
                AuditAction.CANCEL_APPROVAL.getActionName(), username, null, null,
                String.format("Hủy duyệt cấu phần Code=%s - %s bởi %s", code, entity.getComponentName(), username),
                statusBefore, ParamStatus.CANCELED.getCode());
        return saved;
    }

    // ─── Export ───────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRawDataForExport() {
        List<ProcessingComponent> activeList = repository
                .findAllByIsActiveOrderByComponentNameAsc(ActiveStatus.ACTIVE.getCode());
        return activeList.stream()
                .map(entity -> objectMapper.convertValue(entity, new TypeReference<Map<String, Object>>() {}))
                .toList();
    }

    // ─── Batch Operations ─────────────────────────────────────────────────────
    @Override
    public List<BatchItemResultDTO> batchApprove(List<String> codes, String approver) {
        log.info("[Component] Batch approve started. count={}, approver={}", codes.size(), approver);
        List<BatchItemResultDTO> results = codes.stream()
                .map(code -> runBatchItem(code, (entity, result) -> approveComponentAction(entity, approver, result)))
                .toList();
        log.info("[Component] Batch approve done. success={}/{}",
                results.stream().filter(r -> BATCH_SUCCESS.equalsIgnoreCase(r.getStatus())).count(), codes.size());
        return results;
    }

    @Override
    public List<BatchItemResultDTO> batchReject(List<String> codes, String reason, String approver) {
        log.info("[Component] Batch reject started. count={}, approver={}", codes.size(), approver);
        List<BatchItemResultDTO> results = codes.stream()
                .map(code -> runBatchItem(code, (entity, result) -> rejectComponentAction(entity, reason, approver, result)))
                .toList();
        log.info("[Component] Batch reject done. success={}/{}",
                results.stream().filter(r -> BATCH_SUCCESS.equalsIgnoreCase(r.getStatus())).count(), codes.size());
        return results;
    }

    /** Delegate sang base executeBatchItem với code và entity supplier cụ thể */
    private BatchItemResultDTO runBatchItem(String code, BatchActionConsumer<ProcessingComponent> action) {
        return executeBatchItem(
                BatchItemResultDTO.builder().code(code).build(),
                () -> getByCode(code),
                action,
                code);
    }

    private void approveComponentAction(ProcessingComponent entity, String approver, BatchItemResultDTO result) {
        validateMakerChecker(entity, approver, BusinessErrorCode.INVALID_APPROVE_STATUS);

        int statusBefore = entity.getStatus();
        if (StringUtils.hasText(entity.getNewData())) applyNewDataChanges(entity);

        StoredProcedureResult spResult = executeComponentStoredProcedure("PROC_APPROVE_COMPONENT", entity.getComponentCode(), approver);
        if (!spResult.isSuccess()) throw new BusinessRuleException(spResult.getMessage());

        entityManager.refresh(entity);
        result.setStatus(BATCH_SUCCESS);
        result.setErrorMessage(spResult.getMessage());
        auditLogService.log(MODULE, entity.getComponentCode(),
                AuditAction.APPROVE.getActionName(), approver, null, null,
                String.format("Phê duyệt cấu phần Code=%s - %s bởi %s",
                        entity.getComponentCode(), entity.getComponentName(), approver),
                statusBefore, ParamStatus.APPROVED.getCode());
    }

    private void rejectComponentAction(ProcessingComponent entity, String reason, String approver, BatchItemResultDTO result) {
        validateMakerChecker(entity, approver, BusinessErrorCode.INVALID_REJECT_STATUS);

        int statusBefore = entity.getStatus();
        StoredProcedureResult spResult = executeComponentStoredProcedure("PROC_REJECT_COMPONENT", entity.getComponentCode(), approver);
        if (!spResult.isSuccess()) throw new BusinessRuleException(spResult.getMessage());

        entityManager.refresh(entity);
        result.setStatus(BATCH_SUCCESS);
        result.setErrorMessage(spResult.getMessage());
        auditLogService.log(MODULE, entity.getComponentCode(),
                AuditAction.REJECT.getActionName(), approver, null, null,
                StringUtils.hasText(reason)
                        ? String.format("Từ chối duyệt cấu phần Code=%s - %s bởi %s. Lý do: %s",
                                entity.getComponentCode(), entity.getComponentName(), approver, reason.trim())
                        : String.format("Từ chối duyệt cấu phần Code=%s - %s bởi %s",
                                entity.getComponentCode(), entity.getComponentName(), approver),
                statusBefore, ParamStatus.REJECTED.getCode());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private void updateEntityFromDto(ProcessingComponent entity, ComponentDTO dto, String username) {
        entity.setComponentName(dto.getComponentName());
        entity.setMessageType(dto.getMessageType());
        entity.setConnectionMethod(dto.getConnectionMethod());
        entity.setCheckToken(dto.getCheckToken());
        entity.setDescription(dto.getDescription());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setEndEffectiveDate(dto.getEndEffectiveDate());
        entity.updateActiveStatus();   // BaseEntity — không cần computeActiveStatus() nữa
        entity.setUpdatedBy(username);
        entity.setStatus(ParamStatus.NEW.getCode());
    }

    private void applyNewDataChanges(ProcessingComponent entity) {
        try {
            ComponentDTO changes = getObjectMapper().readValue(entity.getNewData(), ComponentDTO.class);
            if (changes.getComponentName()    != null) entity.setComponentName(changes.getComponentName());
            if (changes.getMessageType()      != null) entity.setMessageType(changes.getMessageType());
            if (changes.getConnectionMethod() != null) entity.setConnectionMethod(changes.getConnectionMethod());
            if (changes.getCheckToken()       != null) entity.setCheckToken(changes.getCheckToken());
            entity.setDescription(changes.getDescription());
            if (changes.getEffectiveDate()    != null) entity.setEffectiveDate(changes.getEffectiveDate());
            entity.setEndEffectiveDate(changes.getEndEffectiveDate());
            entity.updateActiveStatus();               // BaseEntity — không cần computeActiveStatus() nữa
            entity.setNewData(null);
            repository.saveAndFlush(entity);
        } catch (Exception ex) {
            throw new BusinessRuleException(BusinessErrorCode.DATA_DECODE_ERROR, ex);
        }
    }

    private StoredProcedureResult executeComponentStoredProcedure(String procedureName, String code, String user) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(procedureName);
        query.registerStoredProcedureParameter("p_code",    String.class,  ParameterMode.IN);
        query.registerStoredProcedureParameter("p_user",    String.class,  ParameterMode.IN);
        query.registerStoredProcedureParameter("p_status",  Integer.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_message", String.class,  ParameterMode.OUT);
        query.setParameter("p_code", code);
        query.setParameter("p_user", user);
        query.execute();

        Object spStatusObj = query.getOutputParameterValue("p_status");
        String spMessage   = (String) query.getOutputParameterValue("p_message");
        return new StoredProcedureResult(
                spStatusObj instanceof Number && ((Number) spStatusObj).intValue() == 1,
                spMessage);
    }

    private boolean isDtoDifferentFromEntity(ProcessingComponent entity, ComponentDTO dto) {
        return !Objects.equals(entity.getComponentName(),    dto.getComponentName())
            || !Objects.equals(entity.getMessageType(),      dto.getMessageType())
            || !Objects.equals(entity.getConnectionMethod(), dto.getConnectionMethod())
            || !Objects.equals(entity.getCheckToken(),       dto.getCheckToken())
            || !Objects.equals(entity.getDescription(),      dto.getDescription())
            || !Objects.equals(entity.getEffectiveDate(),    dto.getEffectiveDate())
            || !Objects.equals(entity.getEndEffectiveDate(), dto.getEndEffectiveDate());
    }
}
