package com.example.paymenthub.common.base;

import com.example.paymenthub.common.enums.BusinessErrorCode;
import com.example.paymenthub.common.exception.InvalidStateTransitionException;
import com.example.paymenthub.common.exception.MakerCheckerConflictException;
import com.example.paymenthub.dto.response.BatchItemResultDTO;
import com.example.paymenthub.security.SecurityUtils;
import com.example.paymenthub.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

@Slf4j
public abstract class AbstractMakerCheckerService {

    protected static final String BATCH_SUCCESS = "SUCCESS";
    protected static final String BATCH_FAILED = "FAILED";

    // ── Dependencies: subclass cung cấp qua getter (không cần super constructor)
    // ──
    protected abstract ObjectMapper getObjectMapper();

    protected abstract AuditLogService getAuditLogService();

    protected abstract TransactionTemplate getTransactionTemplate();

    // ── Serialize object to JSON ───────────────────────────────────────────────
    protected String toJson(Object obj) {
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[{}] Failed to serialize to JSON: {}", getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    // ── Resolve username: fallback sang SecurityContext nếu rỗng ──────────────
    protected String resolveUsername(String username) {
        if (!StringUtils.hasText(username))
            return SecurityUtils.getCurrentUsername();
        return username;
    }

    // ── Validate Maker-Checker: entity phải extend BaseEntity ─────────────────
    protected <E extends BaseEntity> void validateMakerChecker(
            E entity, String approver, BusinessErrorCode invalidStatusError) {

        if (!entity.isPending()) {
            throw new InvalidStateTransitionException(invalidStatusError);
        }
        String maker = StringUtils.hasText(entity.getUpdatedBy())
                ? entity.getUpdatedBy()
                : entity.getCreatedBy();
        if (StringUtils.hasText(maker) && approver != null && approver.equalsIgnoreCase(maker)) {
            throw new MakerCheckerConflictException(BusinessErrorCode.MAKER_CHECKER_SAME_USER);
        }
    }

    // ── BatchActionConsumer functional interface ───────────────────────────────
    @FunctionalInterface
    protected interface BatchActionConsumer<E> {
        void accept(E entity, BatchItemResultDTO result);
    }

    /**
     * Generic batch item runner.
     * <p>
     * Bọc mỗi item trong Transaction độc lập (REQUIRES_NEW).
     * Caller cung cấp pre-built result (đã set id/code), entity supplier, và
     * action.
     *
     * @param resultTemplate BatchItemResultDTO đã được khởi tạo với id hoặc code
     * @param entitySupplier Hàm lấy entity (getById / getByCode)
     * @param action         Logic nghiệp vụ (approve / reject)
     * @param logKey         Giá trị dùng để log (id hoặc code)
     */
    protected <E, ID> BatchItemResultDTO executeBatchItem(
            BatchItemResultDTO resultTemplate,
            Supplier<E> entitySupplier,
            BatchActionConsumer<E> action,
            ID logKey) {
        try {
            getTransactionTemplate().executeWithoutResult(
                    status -> action.accept(entitySupplier.get(), resultTemplate));
        } catch (Exception e) {
            resultTemplate.setStatus(BATCH_FAILED);
            resultTemplate.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Lỗi thực thi");
            log.error("[{}] Batch item failed for key={}. error={}",
                    getClass().getSimpleName(), logKey, e.getMessage());
        }
        return resultTemplate;
    }

    // ── StoredProcedureResult (shared across modules) ─────────────────────────
    protected static class StoredProcedureResult {
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
}
