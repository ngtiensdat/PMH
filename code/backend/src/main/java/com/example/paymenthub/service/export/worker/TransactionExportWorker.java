package com.example.paymenthub.service.export.worker;

import com.example.paymenthub.common.enums.ExportConstants;
import com.example.paymenthub.common.util.JsonUtils;
import com.example.paymenthub.dto.request.CreateExportJobRequestDTO;
import com.example.paymenthub.entity.ExportJob;
import com.example.paymenthub.repository.ExportJobRepository;
import com.example.paymenthub.service.FileStorageService;
import com.example.paymenthub.service.export.ExcelExportHelper;
import com.example.paymenthub.service.export.ExportLockService;
import com.example.paymenthub.service.export.ExportProgressService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TransactionExportWorker extends BaseExportWorker {

    private static final String[] HEADERS = {
            "ID", "Mã Giao Dịch", "Số Tài Khoản", "Số Tiền", "Trạng Thái",
            "Diễn Giải", "Mã Tham Chiếu", "Ngày Tạo", "Ngày Cập Nhật", "Điều Kiện Lọc"
    };

    public TransactionExportWorker(ExportJobRepository exportJobRepository,
            ExportLockService exportLockService,
            ExportProgressService exportProgressService,
            FileStorageService fileStorageService,
            JdbcTemplate jdbcTemplate) {
        super(exportJobRepository, exportLockService, exportProgressService, fileStorageService, jdbcTemplate);
    }

    @Override
    protected String getWorkerName() {
        return "Transaction";
    }

    @Override
    protected String getTempFilePrefix() {
        return "txn_export_";
    }

    @Override
    protected String getFinalFileNamePrefix() {
        return "Transaction_Log";
    }

    @Override
    protected long writeExportFile(ExportJob job, Path tempFile) throws Exception {
        CreateExportJobRequestDTO filter = JsonUtils.fromJson(
                job.getFilterPayload(), CreateExportJobRequestDTO.class);
        return writeXlsxFile(job, filter, tempFile);
    }

    private long writeXlsxFile(ExportJob job, CreateExportJobRequestDTO filter, Path tempFile) throws Exception {
        long processedRows = 0;
        long maxExportId = job.getMaxExportId();
        long countStart = System.currentTimeMillis();
        long totalRows = countFilteredRows(filter, maxExportId);
        long countEnd = System.currentTimeMillis();
        log.info("[TransactionExportWorker] Milestone 1: Counted totalRows={} in {} ms", totalRows, (countEnd - countStart));

        exportProgressService.updateProgress(String.valueOf(job.getId()), 0, totalRows);
        job.setTotalRows(totalRows);
        exportJobRepository.save(job);

        String filterSummary = ExcelExportHelper.buildTransactionFilterSummary(filter);

        try (SXSSFWorkbook wb = new SXSSFWorkbook(ROW_ACCESS)) {
            wb.setCompressTempFiles(false); // Ghi file tạm thô trực tiếp xuống đĩa SSD (không tốn CPU nén GZIP)

            SXSSFSheet sheet = wb.createSheet("Lịch sử giao dịch");
            sheet.createFreezePane(0, 1);

            CellStyle headerStyle = ExcelExportHelper.createHeaderStyle(wb);
            CellStyle numStyle = ExcelExportHelper.createNumberStyle(wb);
            CellStyle dateStyle = ExcelExportHelper.createDatetimeStyle(wb);
            CellStyle defaultStyle = ExcelExportHelper.createDefaultStyle(wb);

            ExcelExportHelper.createHeaderRow(sheet.createRow(0), HEADERS, headerStyle);

            int rowIndex = 1;
            long offset = 0;
            long lastCursorId = 0;

            while (true) {
                long fetchStart = System.currentTimeMillis();
                List<Map<String, Object>> batch = fetchBatchKeyset(filter, maxExportId, lastCursorId, offset);
                long fetchEnd = System.currentTimeMillis();

                if (batch.isEmpty())
                    break;

                log.info("[TransactionExportWorker] Milestone 2: Batch fetched {} rows (offset={}) in {} ms", 
                        batch.size(), offset, (fetchEnd - fetchStart));

                for (Map<String, Object> data : batch) {
                    Row row = sheet.createRow(rowIndex++);

                    ExcelExportHelper.setCellLong(row, 0, data.get("ID"), numStyle);
                    ExcelExportHelper.setCellString(row, 1, data.get("TRANSACTION_CODE"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 2, data.get("ACCOUNT_NO"), defaultStyle);
                    ExcelExportHelper.setCellDecimal(row, 3, data.get("AMOUNT"), numStyle);
                    ExcelExportHelper.setCellString(row, 4, data.get("STATUS"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 5, data.get("DESCRIPTION"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 6, data.get("REFERENCE_NO"), defaultStyle);
                    ExcelExportHelper.setCellDate(row, 7, data.get("CREATED_AT"), dateStyle);
                    ExcelExportHelper.setCellDate(row, 8, data.get("UPDATED_AT"), dateStyle);

                    if (row.getRowNum() == 1) {
                        ExcelExportHelper.setCellString(row, 9, filterSummary, defaultStyle);
                    }
                    processedRows++;
                    if (data.get("ID") != null) {
                        lastCursorId = toLong(data.get("ID"));
                    }
                    if (processedRows % ExportConstants.PROGRESS_UPDATE_STEP == 0) {
                        exportProgressService.updateProgress(String.valueOf(job.getId()), processedRows, totalRows);
                    }
                }
                offset += batch.size();

                exportProgressService.updateProgress(String.valueOf(job.getId()), processedRows, totalRows);

                if (processedRows % BATCH_SIZE == 0 || batch.size() < BATCH_SIZE) {
                    job.setProcessedRows(processedRows);
                    job.setTotalRows(totalRows);
                    job.setLastHeartbeatAt(LocalDateTime.now());
                    job.setUpdatedAt(LocalDateTime.now());
                    exportJobRepository.save(job);
                }

                if (batch.size() < BATCH_SIZE)
                    break;
            }

            ExcelExportHelper.autoSizeColumns(sheet, HEADERS.length);

            long writeStart = System.currentTimeMillis();
            try (OutputStream os = Files.newOutputStream(tempFile)) {
                wb.write(os);
            }
            long writeEnd = System.currentTimeMillis();
            log.info("[TransactionExportWorker] Milestone 3: SXSSF wb.write(os) written to temp disk in {} ms", (writeEnd - writeStart));
        }
        return processedRows;
    }

    private long countFilteredRows(CreateExportJobRequestDTO filter, long maxExportId) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM TRANSACTION_LOG WHERE 1=1");
        if (maxExportId > 0) {
            sql.append(" AND ID <= ?");
            params.add(maxExportId);
        }
        appendFilterClause(sql, params, filter);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    private List<Map<String, Object>> fetchBatchKeyset(CreateExportJobRequestDTO filter, long maxExportId,
            long lastCursorId, long offset) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT ID, TRANSACTION_CODE, ACCOUNT_NO, AMOUNT, STATUS, DESCRIPTION, REFERENCE_NO, CREATED_AT, UPDATED_AT "
                        + "FROM TRANSACTION_LOG WHERE 1=1");
        if (maxExportId > 0) {
            sql.append(" AND ID <= ?");
            params.add(maxExportId);
        }

        boolean hasCustomSort = filter != null && filter.getSortBy() != null
                && !"createdAt".equalsIgnoreCase(filter.getSortBy()) && !"id".equalsIgnoreCase(filter.getSortBy());

        if (!hasCustomSort) {
            if (lastCursorId > 0) {
                sql.append(" AND ID > ?");
                params.add(lastCursorId);
            }
            appendFilterClause(sql, params, filter);
            sql.append(" ORDER BY ID ASC FETCH FIRST ? ROWS ONLY");
            params.add(BATCH_SIZE);
        } else {
            appendFilterClause(sql, params, filter);
            sql.append(" ").append(resolveSortClause(filter));
            sql.append(" OFFSET ? ROWS FETCH FIRST ? ROWS ONLY");
            params.add(offset);
            params.add(BATCH_SIZE);
        }

        jdbcTemplate.setFetchSize(BATCH_SIZE);
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    private String resolveSortClause(CreateExportJobRequestDTO filter) {
        String sortBy = filter != null && filter.getSortBy() != null ? filter.getSortBy().trim() : "createdAt";
        String dir = filter != null && "asc".equalsIgnoreCase(filter.getSortDirection()) ? "ASC" : "DESC";

        String col;
        switch (sortBy) {
            case "transactionCode" -> col = "TRANSACTION_CODE";
            case "accountNo" -> col = "ACCOUNT_NO";
            case "amount" -> col = "AMOUNT";
            case "status" -> col = "STATUS";
            case "referenceNo" -> col = "REFERENCE_NO";
            case "updatedAt" -> col = "UPDATED_AT";
            case "id" -> col = "ID";
            default -> col = "CREATED_AT";
        }
        if ("ID".equalsIgnoreCase(col)) {
            return "ORDER BY ID " + dir;
        }
        return "ORDER BY " + col + " " + dir + ", ID " + dir;
    }

    private void appendFilterClause(StringBuilder sql, List<Object> params, CreateExportJobRequestDTO filter) {
        if (filter == null)
            return;

        if (filter.getAccountNo() != null && !filter.getAccountNo().isBlank()) {
            sql.append(" AND UPPER(ACCOUNT_NO) LIKE UPPER(?)");
            params.add("%" + filter.getAccountNo().trim() + "%");
        }
        if (filter.getTransactionCode() != null && !filter.getTransactionCode().isBlank()) {
            sql.append(" AND UPPER(TRANSACTION_CODE) LIKE UPPER(?)");
            params.add("%" + filter.getTransactionCode().trim() + "%");
        }
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            sql.append(" AND UPPER(STATUS) = UPPER(?)");
            params.add(filter.getStatus().trim());
        }
        if (filter.getFromDate() != null && !filter.getFromDate().isBlank()) {
            sql.append(" AND CREATED_AT >= TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')");
            params.add(normalizeDateTime(filter.getFromDate(), true));
        }
        if (filter.getToDate() != null && !filter.getToDate().isBlank()) {
            sql.append(" AND CREATED_AT <= TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')");
            params.add(normalizeDateTime(filter.getToDate(), false));
        }
    }

    private String normalizeDateTime(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isBlank())
            return null;
        String s = dateStr.trim();
        if (s.contains("Z"))
            s = s.substring(0, s.indexOf("Z"));
        if (s.contains("+"))
            s = s.substring(0, s.indexOf("+"));
        if (s.contains("."))
            s = s.substring(0, s.indexOf("."));
        if (s.contains("T"))
            s = s.replace("T", " ");
        if (s.length() == 10)
            s = s + (isStart ? " 00:00:00" : " 23:59:59");
        return s;
    }
}
