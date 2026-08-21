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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
@Slf4j
public class GroupCategoryServiceImpl implements GroupCategoryService {

    private static final String MODULE = ModuleType.GROUP_CATEGORY.getCode();

    private final GroupCategoryRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public GroupCategoryServiceImpl(GroupCategoryRepository repository,
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
        if (effectiveDate == null)
            return ActiveStatus.INACTIVE.getCode();
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(effectiveDate))
            return ActiveStatus.INACTIVE.getCode(); // Chưa đến ngày hiệu lực
        if (endEffectiveDate != null && now.isAfter(endEffectiveDate))
            return ActiveStatus.INACTIVE.getCode(); // Đã quá ngày hết hiệu lực
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

    // ─── Search ──────────────────────────────────────────────────────────────

    @Override
    public Page<GroupCategory> search(GroupCategorySearchCriteria criteria, Pageable pageable) {
        Specification<GroupCategory> spec = GroupCategorySpecification.filter(
                criteria.getParamType(), criteria.getParamValue(), criteria.getParamName(),
                criteria.getStatus(), criteria.getIsActive());
        return repository.findAll(spec, pageable);
    }

    @Override
    public GroupCategory getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục có ID: " + id));
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    @Override
    public GroupCategory create(GroupCategoryDTO dto, String username) {
        log.info("[GroupCategory] Creating. user={}, paramType={}, paramValue={}", username, dto.getParamType(),
                dto.getParamValue());

        DateUtils.validateEffectiveDates(dto.getEffectiveDate(), dto.getEndEffectiveDate());

        if (repository.existsOverlapping(
                dto.getParamName(), dto.getParamType(),
                dto.getEffectiveDate(), dto.getEndEffectiveDate(), null)) {
            throw new BusinessRuleException(
                    "Đã tồn tại cấu hình có cùng Tên và Nhóm bị chồng lấn thời gian hiệu lực!");
        }

        GroupCategory entity = GroupCategory.builder()
                .paramName(dto.getParamName())
                .paramValue(dto.getParamValue())
                .paramType(dto.getParamType())
                .description(dto.getDescription())
                .componentCode(dto.getComponentCode())
                .effectiveDate(dto.getEffectiveDate())
                .endEffectiveDate(dto.getEndEffectiveDate())
                .status(ParamStatus.NEW.getCode())
                .isDisplay(DisplayStatus.INITIAL.getCode())
                .isActive(computeActiveStatus(dto.getEffectiveDate(), dto.getEndEffectiveDate()))
                .createdBy(username)
                .updatedBy(username)
                .build();

        GroupCategory saved = repository.save(entity);
        log.info("[GroupCategory] Created. id={}", saved.getId());

        // Ghi audit log
        auditLogService.log(
                MODULE, String.valueOf(saved.getId()),
                AuditAction.CREATE.getActionName(), username,
                null, toJson(saved),
                String.format("Tạo mới tham số: %s / %s / %s", saved.getParamName(), saved.getParamValue(),
                        saved.getParamType()),
                null, ParamStatus.NEW.getCode());

        return saved;
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    @Override
    public GroupCategory update(Long id, GroupCategoryDTO dto, String username) {
        log.info("[GroupCategory] Updating. id={}, user={}", id, username);
        if (username == null || username.trim().isEmpty()) {
            username = SecurityUtils.getCurrentUsername();
        }

        GroupCategory entity = getById(id);
        String oldJson = toJson(entity);
        int statusBefore = entity.getStatus();

        // 1. Kiểm tra dựa trên status: Nếu đang PENDING (3) -> ném lỗi không được sửa
        if (entity.isPending()) {
            throw new InvalidStateTransitionException("Không được phép chỉnh sửa bản ghi đang ở trạng thái Chờ duyệt!");
        }

        // 2. Validate ngày hiệu lực doanh nghiệp
        DateUtils.validateEffectiveDates(dto.getEffectiveDate(), dto.getEndEffectiveDate());

        if (repository.existsOverlapping(
                dto.getParamName(), dto.getParamType(),
                dto.getEffectiveDate(), dto.getEndEffectiveDate(), id)) {
            throw new BusinessRuleException(
                    "Đã tồn tại cấu hình khác có cùng Tên và Nhóm bị chồng lấn thời gian hiệu lực!");
        }

        GroupCategory saved;
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
            updateEntityFields(entity, dto, username);
            entity.setNewData(null);
            saved = repository.save(entity);
            action = AuditAction.UPDATE.getActionName();
            statusAfter = ParamStatus.NEW.getCode();
        }

        String newJson = toJson(saved);

        // Ghi audit log
        auditLogService.log(
                MODULE, String.valueOf(id),
                action, username,
                oldJson, newJson,
                String.format("%s tham số ID=%d bởi %s", action, id, username),
                statusBefore, statusAfter);

        return saved;
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

        if (username == null || username.trim().isEmpty()) {
            username = SecurityUtils.getCurrentUsername();
        }

        GroupCategory entity = getById(id);

        if (entity.isOnceApproved()) {
            throw new BusinessRuleException("Bản ghi đã từng được phê duyệt (isDisplay = 2) là bản ghi chuẩn của hệ thống, không được phép xóa!");
        }

        if (entity.isPending()) {
            throw new InvalidStateTransitionException("Bản ghi đang ở trạng thái Chờ duyệt (STATUS = 3), không được phép xóa!");
        }

        String oldJson = toJson(entity);
        repository.delete(entity);
        log.info("[GroupCategory] Deleted. id={}", id);

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
        if (username == null || username.trim().isEmpty()) {
            username = SecurityUtils.getCurrentUsername();
        }

        GroupCategory entity = getById(id);

        if (!entity.isCanBeSubmitted()) {
            throw new InvalidStateTransitionException("Chỉ được phép gửi duyệt bản ghi ở trạng thái Mới (1), Từ chối (5) hoặc Hủy duyệt (7)!");
        }

        if (entity.isOnceApproved()
                && (entity.getNewData() == null || entity.getNewData().trim().isEmpty())) {
            throw new BusinessRuleException("Bản ghi chưa có bất kỳ thay đổi nào so với dữ liệu đã duyệt, không cần gửi duyệt lại!");
        }

        DateUtils.validateEffectiveDates(entity.getEffectiveDate(), entity.getEndEffectiveDate());

        if (repository.existsOverlapping(
                entity.getParamName(), entity.getParamType(),
                entity.getEffectiveDate(), entity.getEndEffectiveDate(), entity.getId())) {
            throw new BusinessRuleException(
                    "Đã tồn tại cấu hình khác có cùng Tên và Nhóm bị chồng lấn thời gian hiệu lực!");
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
    public GroupCategory cancelApproval(Long id, String username) {
        log.info("[GroupCategory] Canceling approval. id={}, user={}", id, username);

        if (username == null || username.trim().isEmpty()) {
            username = SecurityUtils.getCurrentUsername();
        }

        GroupCategory entity = getById(id);
        if (!entity.isApproved()) {
            throw new InvalidStateTransitionException(
                    "Chỉ được phép hủy duyệt bản ghi đang ở trạng thái đã phê duyệt (STATUS = 4)!");
        }

        int statusBefore = entity.getStatus();

        entity.setStatus(ParamStatus.CANCELED.getCode());
        entity.setUpdatedBy(username);
        GroupCategory saved = repository.save(entity);

        auditLogService.log(
                MODULE, String.valueOf(id),
                AuditAction.CANCEL_APPROVAL.getActionName(), username,
                null, null,
                String.format("Hủy duyệt tham số ID=%d: %s / %s bởi %s", id, entity.getParamName(),
                        entity.getParamValue(), username),
                statusBefore, ParamStatus.CANCELED.getCode());

        return saved;
    }

    // ─── Joined List (Native Query) ──────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getJoinedList() {
        String sql = "SELECT gc.ID as id, gc.PARAM_NAME as paramName, gc.PARAM_VALUE as paramValue, " +
                "gc.PARAM_TYPE as paramType, gc.DESCRIPTION as description, " +
                "gc.COMPONENT_CODE as componentCode, COALESCE(c.COMPONENT_NAME, 'Chưa xác định') as componentName, " +
                "gc.STATUS as status, gc.IS_ACTIVE as isActive, gc.EFFECTIVE_DATE as effectiveDate " +
                "FROM PMH_GROUP_CATEGORY gc " +
                "LEFT JOIN PMH_COMPONENTS c ON gc.COMPONENT_CODE = c.COMPONENT_CODE " +
                "ORDER BY gc.UPDATED_DATE DESC";

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

    // ─── Batch Approve ───────────────────────────────────────────────────────

    @Override
    public List<BatchItemResultDTO> batchApprove(List<Long> ids, String approver) {
        log.info("[GroupCategory] Batch approve started. count={}, approver={}", ids.size(), approver);
        List<BatchItemResultDTO> results = new ArrayList<>();

        for (Long id : ids) {
            results.add(approveSingleCategory(id, approver));
        }

        long successCount = results.stream().filter(r -> "SUCCESS".equalsIgnoreCase(r.getStatus())).count();
        log.info("[GroupCategory] Batch approve done. success={}/{}", successCount, ids.size());
        return results;
    }

    private BatchItemResultDTO approveSingleCategory(Long id, String approver) {
        BatchItemResultDTO result = BatchItemResultDTO.builder().id(id).build();
        try {
            this.transactionTemplate.executeWithoutResult(status -> {
                GroupCategory entity = getById(id);

                if (!entity.isPending()) {
                    throw new InvalidStateTransitionException("Chỉ được phép phê duyệt bản ghi đang ở trạng thái Chờ duyệt (STATUS = 3)!");
                }

                if (approver.equalsIgnoreCase(entity.getCreatedBy())
                        || approver.equalsIgnoreCase(entity.getUpdatedBy())) {
                    throw new MakerCheckerConflictException(
                            "Người phê duyệt (" + approver + ") không được trùng với người tạo/cập nhật yêu cầu!");
                }

                int statusBefore = entity.getStatus();

                if (entity.getNewData() != null && !entity.getNewData().isEmpty()) {
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
                        throw new BusinessRuleException("Lỗi giải mã dữ liệu thay đổi: " + ex.getMessage(), ex);
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
                boolean success = spStatusObj instanceof Number && ((Number) spStatusObj).intValue() == 1;

                if (success) {
                    result.setStatus("SUCCESS");
                    result.setErrorMessage(spMessage);
                    auditLogService.log(
                            MODULE, String.valueOf(id),
                            AuditAction.APPROVE.getActionName(), approver,
                            null, null,
                            String.format("Phê duyệt tham số ID=%d. SP: %s", id, spMessage),
                            statusBefore, ParamStatus.APPROVED.getCode());
                } else {
                    throw new BusinessRuleException(spMessage);
                }
            });
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Lỗi thực thi");
            log.error("[GroupCategory] Batch approve failed for id={}. error={}", id, e.getMessage());
        }
        return result;
    }

    // ─── Batch Reject ────────────────────────────────────────────────────────

    @Override
    public List<BatchItemResultDTO> batchReject(List<Long> ids, String reason, String approver) {
        log.info("[GroupCategory] Batch reject started. count={}, approver={}", ids.size(), approver);
        List<BatchItemResultDTO> results = new ArrayList<>();

        for (Long id : ids) {
            results.add(rejectSingleCategory(id, reason, approver));
        }

        long successCount = results.stream().filter(r -> "SUCCESS".equalsIgnoreCase(r.getStatus())).count();
        log.info("[GroupCategory] Batch reject done. success={}/{}", successCount, ids.size());
        return results;
    }

    private BatchItemResultDTO rejectSingleCategory(Long id, String reason, String approver) {
        BatchItemResultDTO result = BatchItemResultDTO.builder().id(id).build();
        try {
            this.transactionTemplate.executeWithoutResult(status -> {
                GroupCategory entity = getById(id);

                if (!entity.isPending()) {
                    throw new InvalidStateTransitionException("Chỉ được phép từ chối bản ghi đang ở trạng thái Chờ duyệt (STATUS = 3)!");
                }

                if (approver.equalsIgnoreCase(entity.getCreatedBy())
                        || approver.equalsIgnoreCase(entity.getUpdatedBy())) {
                    throw new MakerCheckerConflictException(
                            "Người phê duyệt (" + approver + ") không được trùng với người tạo/cập nhật yêu cầu!");
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
                boolean success = spStatusObj instanceof Number && ((Number) spStatusObj).intValue() == 1;

                if (success) {
                    result.setStatus("SUCCESS");
                    result.setErrorMessage(spMessage);
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
                    throw new BusinessRuleException(spMessage);
                }
            });
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Lỗi thực thi");
            log.error("[GroupCategory] Batch reject failed for id={}. error={}", id, e.getMessage());
        }
        return result;
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
