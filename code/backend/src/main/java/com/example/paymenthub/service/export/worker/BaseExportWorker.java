package com.example.paymenthub.service.export.worker;

import com.example.paymenthub.common.enums.ExportConstants;
import com.example.paymenthub.common.enums.ExportJobStatus;
import com.example.paymenthub.entity.ExportJob;
import com.example.paymenthub.repository.ExportJobRepository;
import com.example.paymenthub.service.FileStorageService;
import com.example.paymenthub.service.export.ExportLockService;
import com.example.paymenthub.service.export.ExportProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseExportWorker {

    protected final ExportJobRepository exportJobRepository;
    protected final ExportLockService exportLockService;
    protected final ExportProgressService exportProgressService;
    protected final FileStorageService fileStorageService;
    protected final JdbcTemplate jdbcTemplate;

    protected static final int BATCH_SIZE = ExportConstants.DEFAULT_BATCH_SIZE;
    protected static final int ROW_ACCESS = ExportConstants.EXCEL_ROW_ACCESS_WINDOW;

    private static final Path EXPORT_TEMP_DIR = Path.of("E:/temp_exports");

    static {
        try {
            Files.createDirectories(EXPORT_TEMP_DIR);
            System.setProperty("java.io.tmpdir", EXPORT_TEMP_DIR.toAbsolutePath().toString());
            org.apache.poi.util.TempFile.setTempFileCreationStrategy(
                    new org.apache.poi.util.DefaultTempFileCreationStrategy(EXPORT_TEMP_DIR.toFile()));
        } catch (Exception e) {
            log.error("[BaseExportWorker] Failed to setup temp export dir on Drive E: {}", e.getMessage());
        }
    }

    protected abstract String getWorkerName();

    protected abstract String getTempFilePrefix();

    protected abstract String getFinalFileNamePrefix();

    protected abstract long writeExportFile(ExportJob job, Path tempFile) throws Exception;

    @Async("exportExecutor")
    public void runExport(Long jobId) {
        ExportJob job = exportJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("[ExportWorker-{}] Job not found: id={}", getWorkerName(), jobId);
            return;
        }

        String lockKey = String.valueOf(job.getUserId());
        Path tempFile = null;

        long startMs = System.currentTimeMillis();
        try {
            job.setStatus(ExportJobStatus.PROCESSING.name());
            job.setLastHeartbeatAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            exportJobRepository.save(job);

            if (!Files.exists(EXPORT_TEMP_DIR)) {
                Files.createDirectories(EXPORT_TEMP_DIR);
            }
            tempFile = Files.createTempFile(EXPORT_TEMP_DIR, getTempFilePrefix(), ".xlsx");
            long processedRows = writeExportFile(job, tempFile);
            long writeMs = System.currentTimeMillis();
            log.info("[ExportWorker-{}] Step 1 Finished writing temp file: rows={}, took {} ms", getWorkerName(),
                    processedRows, (writeMs - startMs));

            String finalFileName = getFinalFileNamePrefix() + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    + ".xlsx";

            String storedPath;
            try (InputStream in = Files.newInputStream(tempFile)) {
                storedPath = fileStorageService.upload(finalFileName, in, Files.size(tempFile));
            }
            long uploadMs = System.currentTimeMillis();
            log.info("[ExportWorker-{}] Step 2 Upload to MinIO done: path={}, took {} ms, TOTAL = {} ms",
                    getWorkerName(), storedPath, (uploadMs - writeMs), (uploadMs - startMs));

            LocalDateTime completedAt = LocalDateTime.now();
            job.setStatus(ExportJobStatus.DONE.name());
            job.setExportUrl(storedPath);
            job.setFileName(finalFileName);
            job.setProcessedRows(processedRows);
            job.setTotalRows(processedRows);
            job.setUpdatedAt(completedAt);
            job.setExpiresAt(completedAt.plusHours(12));
            job.setLastHeartbeatAt(completedAt);
            exportJobRepository.save(job);

            exportProgressService.updateProgress(String.valueOf(jobId), processedRows, processedRows);
            log.info("[ExportWorker-{}] Job DONE: id={}, rows={}, file={}", getWorkerName(), jobId, processedRows,
                    storedPath);

        } catch (Throwable t) {
            handleJobFailure(jobId, job, t, lockKey, tempFile);
        } finally {
            cleanUp(lockKey, tempFile);
        }
    }

    protected void handleJobFailure(Long jobId, ExportJob job, Throwable t, String lockKey, Path tempFile) {
        log.error("[ExportWorker-{}] Job FAILED: id={}, error={}", getWorkerName(), jobId, t.getMessage(), t);
        String errMsg = t.getMessage();
        job.setStatus(ExportJobStatus.FAILED.name());
        job.setErrorMessage(
                errMsg != null ? errMsg.substring(0, Math.min(errMsg.length(), 999)) : "Lỗi không xác định");
        job.setUpdatedAt(LocalDateTime.now());
        job.setLastHeartbeatAt(LocalDateTime.now());
        exportJobRepository.save(job);
        exportProgressService.clearProgress(String.valueOf(jobId));
    }

    protected void cleanUp(String lockKey, Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
            }
        }
        if (lockKey != null) {
            exportLockService.unlock(lockKey);
        }
    }

    protected long toLong(Object value) {
        if (value instanceof Number n)
            return n.longValue();
        return Long.parseLong(value.toString());
    }

    protected String formatStatus(Object value) {
        if (value == null)
            return "";
        int code;
        try {
            code = ((Number) value).intValue();
        } catch (Exception e) {
            return value.toString();
        }

        return switch (code) {
            case 1 -> "Tạo mới";
            case 3 -> "Chờ duyệt";
            case 4 -> "Đã duyệt";
            case 5 -> "Từ chối";
            case 7 -> "Hủy duyệt";
            default -> String.valueOf(code);
        };
    }

    protected String formatIsActive(Object value) {
        if (value == null)
            return "";
        int code;
        try {
            code = ((Number) value).intValue();
        } catch (Exception e) {
            return value.toString();
        }
        return code == 1 ? "Hoạt động" : "Ngừng hoạt động";
    }

    protected void appendInClause(StringBuilder sql, java.util.List<Object> params, String columnName,
            java.util.List<?> values) {
        if (values != null && !values.isEmpty()) {
            String placeholders = String.join(",", values.stream().map(v -> "?").toList());
            sql.append(" AND ").append(columnName).append(" IN (").append(placeholders).append(")");
            params.addAll(values);
        }
    }
}
