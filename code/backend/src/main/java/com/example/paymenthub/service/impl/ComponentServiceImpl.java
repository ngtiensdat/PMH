package com.example.paymenthub.service.impl;

import com.example.paymenthub.common.enums.ActiveStatus;
import com.example.paymenthub.common.enums.AuditAction;
import com.example.paymenthub.common.enums.DisplayStatus;
import com.example.paymenthub.common.enums.ModuleType;
import com.example.paymenthub.common.enums.ParamStatus;
import com.example.paymenthub.dto.request.ComponentDTO;
import com.example.paymenthub.dto.request.ComponentSearchCriteria;
import com.example.paymenthub.entity.ProcessingComponent;
import com.example.paymenthub.repository.ComponentRepository;
import com.example.paymenthub.service.ComponentService;
import com.example.paymenthub.repository.specification.ComponentSpecification;
import com.example.paymenthub.service.AuditLogService;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
@Slf4j
public class ComponentServiceImpl implements ComponentService {

    private static final String MODULE = ModuleType.COMPONENT.getCode();

    private final ComponentRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public ComponentServiceImpl(ComponentRepository repository,
            ObjectMapper objectMapper,
            AuditLogService auditLogService,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
            log.warn("[Component] Failed to serialize to JSON: {}", e.getMessage());
            return null;
        }
    }

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
        List<ProcessingComponent> rawList;
        if (status != null) {
            rawList = repository.findAllByIsActiveAndStatusOrderByComponentNameAsc(ActiveStatus.ACTIVE.getCode(), status);
        } else {
            rawList = repository.findAllByIsActiveOrderByComponentNameAsc(ActiveStatus.ACTIVE.getCode());
        }

        LocalDateTime now = LocalDateTime.now();
        return rawList.stream()
                .filter(c -> c.getEffectiveDate() != null && !c.getEffectiveDate().isAfter(now))
                .filter(c -> c.getEndEffectiveDate() == null || !c.getEndEffectiveDate().isBefore(now))
                .toList();
    }

    @Override
    public ProcessingComponent create(ComponentDTO dto, String username) {
        log.info("[Component] Creating. user={}, componentCode={}", username, dto.getComponentCode());

        DateUtils.validateEffectiveDates(dto.getEffectiveDate(), dto.getEndEffectiveDate());

        if (dto.getComponentCode() != null && repository.existsByComponentCode(dto.getComponentCode().toUpperCase())) {
            throw new BusinessRuleException("Mã cấu phần '" + dto.getComponentCode().toUpperCase() + "' đã tồn tại!");
        }

        ProcessingComponent entity = ProcessingComponent.builder()
                .componentCode(dto.getComponentCode().toUpperCase())
                .componentName(dto.getComponentName())
                .messageType(dto.getMessageType())
                .connectionMethod(dto.getConnectionMethod())
                .checkToken(dto.getCheckToken() != null ? dto.getCheckToken() : "N")
                .description(dto.getDescription())
                .effectiveDate(dto.getEffectiveDate())
                .endEffectiveDate(dto.getEndEffectiveDate())
                .status(ParamStatus.NEW.getCode())
                .isDisplay(DisplayStatus.INITIAL.getCode())
                .isActive(computeActiveStatus(dto.getEffectiveDate(), dto.getEndEffectiveDate()))
                .createdBy(username)
                .updatedBy(username)
                .build();

        ProcessingComponent saved = repository.save(entity);
        log.info("[Component] Created. code={}", saved.getComponentCode());

        // Ghi audit log
        auditLogService.log(
                MODULE, saved.getComponentCode(),
                AuditAction.CREATE.getActionName(), username,
                null, toJson(saved),
                String.format("Tạo mới cấu phần: %s - %s", saved.getComponentCode(), saved.getComponentName()),
                null, ParamStatus.NEW.getCode());

        return saved;
    }

    @Override
    public ProcessingComponent update(String code, ComponentDTO dto, String username) {
        log.info("[Component] Updating. code={}, user={}", code, username);
        if (username == null || username.trim().isEmpty()) {
            username = SecurityUtils.getCurrentUsername();
        }

        ProcessingComponent entity = getByCode(code);
        String oldJson = toJson(entity);
        int statusBefore = entity.getStatus();

        // 1. Kiểm tra dựa trên status: Nếu đang PENDING (3) -> ném lỗi không được sửa
        if (entity.isPending()) {
            throw new InvalidStateTransitionException("Không được phép chỉnh sửa cấu phần đang ở trạng thái Chờ duyệt!");
        }

        // 2. Validate ngày hiệu lực doanh nghiệp
        DateUtils.validateEffectiveDates(dto.getEffectiveDate(), dto.getEndEffectiveDate());

        ProcessingComponent saved;
        String action;
        int statusAfter;

        // 3. Phân nhánh lưu trữ dựa vào status và isDisplay
        if (entity.isApproved() || entity.isOnceApproved()) {
            if (!isDtoDifferentFromEntity(entity, dto)) {
                throw new BusinessRuleException("Dữ liệu cập nhật trùng khớp 100% với dữ liệu đang vận hành, không có thay đổi nào để gửi duyệt!");
            }
            entity.setNewData(toJson(dto));
            entity.setUpdatedBy(username);
            entity.setStatus(ParamStatus.CANCELED.getCode()); // Đặt trạng thái về Hủy duyệt (7) để Maker gửi duyệt lại
            saved = repository.save(entity);
            action = "Lưu sửa nháp (Hủy duyệt)";
            statusAfter = ParamStatus.CANCELED.getCode();
        } else {
            // Bản ghi chưa từng được duyệt (isDisplay == 1): cập nhật trực tiếp vào các cột
            entity.setComponentName(dto.getComponentName());
            entity.setMessageType(dto.getMessageType());
            entity.setConnectionMethod(dto.getConnectionMethod());
            entity.setCheckToken(dto.getCheckToken());
            entity.setDescription(dto.getDescription());
            entity.setIsActive(computeActiveStatus(dto.getEffectiveDate(), dto.getEndEffectiveDate()));
            entity.setEffectiveDate(dto.getEffectiveDate());
            entity.setEndEffectiveDate(dto.getEndEffectiveDate());
            entity.setNewData(null); // Xóa các thay đổi nháp cũ nếu có
            entity.setUpdatedBy(username);
            entity.setStatus(ParamStatus.NEW.getCode());
            saved = repository.save(entity);
            action = AuditAction.UPDATE.getActionName();
            statusAfter = ParamStatus.NEW.getCode();
        }

        String newJson = toJson(saved);

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

        if (username == null || username.trim().isEmpty()) {
            username = SecurityUtils.getCurrentUsername();
        }

        ProcessingComponent entity = getByCode(code);

        if (entity.isOnceApproved()) {
            throw new BusinessRuleException("Bản ghi đã từng được phê duyệt (isDisplay = 2) là bản ghi chuẩn của hệ thống, không được phép xóa!");
        }

        if (entity.isPending()) {
            throw new InvalidStateTransitionException("Bản ghi đang ở trạng thái Chờ duyệt (STATUS = 3), không được phép xóa!");
        }

        String oldJson = toJson(entity);
        repository.delete(entity);
        log.info("[Component] Deleted. code={}", code);

        auditLogService.log(
                MODULE, code,
                AuditAction.DELETE.getActionName(), username,
                oldJson, null,
                String.format("Xóa cấu phần chưa duyệt: %s - %s", entity.getComponentCode(), entity.getComponentName()),
                entity.getStatus(), null);
    }

    @Override
    public ProcessingComponent sendForApproval(String code, String username) {
        log.info("[Component] Sending for approval. code={}, user={}", code, username);
        if (username == null || username.trim().isEmpty()) {
            username = SecurityUtils.getCurrentUsername();
        }

        ProcessingComponent entity = getByCode(code);

        if (!entity.isCanBeSubmitted()) {
            throw new InvalidStateTransitionException("Chỉ được phép gửi duyệt cấu phần ở trạng thái Mới (1), Từ chối (5) hoặc Hủy duyệt (7)!");
        }

        if (entity.isOnceApproved()
                && (entity.getNewData() == null || entity.getNewData().trim().isEmpty())) {
            throw new BusinessRuleException("Cấu phần chưa có bất kỳ thay đổi nào so với dữ liệu đã duyệt, không cần gửi duyệt lại!");
        }

        DateUtils.validateEffectiveDates(entity.getEffectiveDate(), entity.getEndEffectiveDate());

        int statusBefore = entity.getStatus();

        entity.setStatus(ParamStatus.PENDING.getCode());
        entity.setUpdatedBy(username);
        ProcessingComponent saved = repository.save(entity);

        auditLogService.log(
                MODULE, code,
                AuditAction.SEND_APPROVAL.getActionName(), username,
                null, null,
                String.format("Gửi duyệt cấu phần Code=%s: %s", code, entity.getComponentName()),
                statusBefore, ParamStatus.PENDING.getCode());

        return saved;
    }

    @Override
    public ProcessingComponent cancelApproval(String code, String username) {
        log.info("[Component] Canceling approval. code={}, user={}", code, username);

        if (username == null || username.trim().isEmpty()) {
            username = SecurityUtils.getCurrentUsername();
        }

        ProcessingComponent entity = getByCode(code);
        if (!entity.isApproved()) {
            throw new InvalidStateTransitionException(
                    "Chỉ được phép hủy duyệt bản ghi đang ở trạng thái đã phê duyệt (STATUS = 4)!");
        }

        int statusBefore = entity.getStatus();

        entity.setStatus(ParamStatus.CANCELED.getCode());
        entity.setUpdatedBy(username);
        ProcessingComponent saved = repository.save(entity);

        auditLogService.log(
                MODULE, code,
                AuditAction.CANCEL_APPROVAL.getActionName(), username,
                null, null,
                String.format("Hủy duyệt cấu phần Code=%s - %s bởi %s", code, entity.getComponentName(), username),
                statusBefore, ParamStatus.CANCELED.getCode());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRawDataForExport() {
        List<ProcessingComponent> activeList = repository.findAllByIsActiveOrderByComponentNameAsc(ActiveStatus.ACTIVE.getCode());
        return activeList.stream()
                .map(entity -> objectMapper.convertValue(entity, new TypeReference<Map<String, Object>>() {}))
                .toList();
    }

    @Override
    public List<BatchItemResultDTO> batchApprove(List<String> codes, String approver) {
        log.info("[Component] Batch approve started. count={}, approver={}", codes.size(), approver);
        List<BatchItemResultDTO> results = new ArrayList<>();

        for (String code : codes) {
            results.add(approveSingleComponent(code, approver));
        }

        long successCount = results.stream().filter(r -> "SUCCESS".equalsIgnoreCase(r.getStatus())).count();
        log.info("[Component] Batch approve done. success={}/{}", successCount, codes.size());
        return results;
    }

    private BatchItemResultDTO approveSingleComponent(String code, String approver) {
        BatchItemResultDTO result = BatchItemResultDTO.builder().build();
        try {
            this.transactionTemplate.executeWithoutResult(status -> {
                ProcessingComponent entity = getByCode(code);

                if (!entity.isPending()) {
                    throw new InvalidStateTransitionException("Chỉ được phép phê duyệt cấu phần đang ở trạng thái Chờ duyệt (STATUS = 3)!");
                }

                if (approver.equalsIgnoreCase(entity.getCreatedBy())
                        || approver.equalsIgnoreCase(entity.getUpdatedBy())) {
                    throw new MakerCheckerConflictException(
                            "Người phê duyệt (" + approver + ") không được trùng với người tạo/cập nhật yêu cầu!");
                }

                int statusBefore = entity.getStatus();

                if (entity.getNewData() != null && !entity.getNewData().isEmpty()) {
                    try {
                        ComponentDTO changes = objectMapper.readValue(entity.getNewData(), ComponentDTO.class);
                        if (changes.getComponentName() != null) entity.setComponentName(changes.getComponentName());
                        if (changes.getMessageType() != null) entity.setMessageType(changes.getMessageType());
                        if (changes.getConnectionMethod() != null) entity.setConnectionMethod(changes.getConnectionMethod());
                        if (changes.getCheckToken() != null) entity.setCheckToken(changes.getCheckToken());
                        entity.setDescription(changes.getDescription());
                        if (changes.getEffectiveDate() != null) entity.setEffectiveDate(changes.getEffectiveDate());
                        entity.setEndEffectiveDate(changes.getEndEffectiveDate());
                        entity.setIsActive(computeActiveStatus(entity.getEffectiveDate(), entity.getEndEffectiveDate()));
                        entity.setNewData(null);
                        repository.saveAndFlush(entity);
                    } catch (Exception ex) {
                        throw new BusinessRuleException("Lỗi giải mã dữ liệu thay đổi: " + ex.getMessage(), ex);
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
                boolean success = spStatusObj instanceof Number && ((Number) spStatusObj).intValue() == 1;

                if (success) {
                    result.setStatus("SUCCESS");
                    result.setErrorMessage(spMessage);
                    auditLogService.log(
                            MODULE, code,
                            AuditAction.APPROVE.getActionName(), approver,
                            null, null,
                            String.format("Phê duyệt cấu phần Code=%s. SP: %s", code, spMessage),
                            statusBefore, ParamStatus.APPROVED.getCode());
                } else {
                    throw new BusinessRuleException(spMessage);
                }
            });
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Lỗi thực thi");
            log.error("[Component] Batch approve failed for code={}. error={}", code, e.getMessage());
        }
        return result;
    }

    @Override
    public List<BatchItemResultDTO> batchReject(List<String> codes, String reason, String approver) {
        log.info("[Component] Batch reject started. count={}, approver={}", codes.size(), approver);
        List<BatchItemResultDTO> results = new ArrayList<>();

        for (String code : codes) {
            results.add(rejectSingleComponent(code, reason, approver));
        }

        long successCount = results.stream().filter(r -> "SUCCESS".equalsIgnoreCase(r.getStatus())).count();
        log.info("[Component] Batch reject done. success={}/{}", successCount, codes.size());
        return results;
    }

    private BatchItemResultDTO rejectSingleComponent(String code, String reason, String approver) {
        BatchItemResultDTO result = BatchItemResultDTO.builder().build();
        try {
            this.transactionTemplate.executeWithoutResult(status -> {
                ProcessingComponent entity = getByCode(code);

                if (!entity.isPending()) {
                    throw new InvalidStateTransitionException("Chỉ được phép từ chối cấu phần đang ở trạng thái Chờ duyệt (STATUS = 3)!");
                }

                if (approver.equalsIgnoreCase(entity.getCreatedBy())
                        || approver.equalsIgnoreCase(entity.getUpdatedBy())) {
                    throw new MakerCheckerConflictException(
                            "Người phê duyệt (" + approver + ") không được trùng với người tạo/cập nhật yêu cầu!");
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
                boolean success = spStatusObj instanceof Number && ((Number) spStatusObj).intValue() == 1;

                if (success) {
                    result.setStatus("SUCCESS");
                    result.setErrorMessage(spMessage);
                    auditLogService.log(
                            MODULE, code,
                            AuditAction.REJECT.getActionName(), approver,
                            null, null,
                            reason != null && !reason.trim().isEmpty()
                                    ? String.format("Từ chối duyệt cấu phần Code=%s. Lý do: %s. SP: %s", code,
                                            reason, spMessage)
                                    : String.format("Từ chối duyệt cấu phần Code=%s. SP: %s", code, spMessage),
                            statusBefore, ParamStatus.REJECTED.getCode());
                } else {
                    throw new BusinessRuleException(spMessage);
                }
            });
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Lỗi thực thi");
            log.error("[Component] Batch reject failed for code={}. error={}", code, e.getMessage());
        }
        return result;
    }

    private boolean isDtoDifferentFromEntity(ProcessingComponent entity, ComponentDTO dto) {
        return !Objects.equals(entity.getComponentName(), dto.getComponentName())
                || !Objects.equals(entity.getMessageType(), dto.getMessageType())
                || !Objects.equals(entity.getConnectionMethod(), dto.getConnectionMethod())
                || !Objects.equals(entity.getCheckToken(), dto.getCheckToken())
                || !Objects.equals(entity.getDescription(), dto.getDescription())
                || !Objects.equals(entity.getIsActive(), dto.getIsActive())
                || !Objects.equals(entity.getEffectiveDate(), dto.getEffectiveDate())
                || !Objects.equals(entity.getEndEffectiveDate(), dto.getEndEffectiveDate());
    }
}
