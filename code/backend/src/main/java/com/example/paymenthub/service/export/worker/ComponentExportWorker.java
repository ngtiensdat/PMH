package com.example.paymenthub.service.export.worker;

import com.example.paymenthub.common.util.JsonUtils;
import com.example.paymenthub.dto.request.CreateExportComponentJobRequestDTO;
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
public class ComponentExportWorker extends BaseExportWorker {

    private static final String[] HEADERS = {
            "Mã cấu phần", "Tên cấu phần", "Chuẩn tin điện",
            "Phương thức kết nối", "Kiểm tra Token", "Trạng thái", "Hoạt động",
            "Ngày hiệu lực", "Ngày hết hiệu lực", "Điều Kiện Lọc"
    };

    public ComponentExportWorker(ExportJobRepository exportJobRepository,
            ExportLockService exportLockService,
            ExportProgressService exportProgressService,
            FileStorageService fileStorageService,
            JdbcTemplate jdbcTemplate) {
        super(exportJobRepository, exportLockService, exportProgressService, fileStorageService, jdbcTemplate);
    }

    @Override
    protected String getWorkerName() {
        return "Component";
    }

    @Override
    protected String getTempFilePrefix() {
        return "comp_export_";
    }

    @Override
    protected String getFinalFileNamePrefix() {
        return "Component";
    }

    @Override
    protected long writeExportFile(ExportJob job, Path tempFile) throws Exception {
        CreateExportComponentJobRequestDTO filter = JsonUtils.fromJson(
                job.getFilterPayload(), CreateExportComponentJobRequestDTO.class);
        return writeComponentXlsxFile(job, filter, tempFile);
    }

    private long writeComponentXlsxFile(ExportJob job, CreateExportComponentJobRequestDTO filter, Path tempFile)
            throws Exception {
        long processedRows = 0;
        long totalRows = countComponentFilteredRows(filter);

        exportProgressService.updateProgress(String.valueOf(job.getId()), 0, totalRows);
        job.setTotalRows(totalRows);
        exportJobRepository.save(job);

        String filterSummary = ExcelExportHelper.buildComponentFilterSummary(filter);

        try (SXSSFWorkbook wb = new SXSSFWorkbook(ROW_ACCESS)) {
            wb.setCompressTempFiles(false);
            SXSSFSheet sheet = wb.createSheet("Cấu phần xử lý");
            sheet.createFreezePane(0, 1);

            CellStyle headerStyle = ExcelExportHelper.createHeaderStyle(wb);
            CellStyle dateStyle = ExcelExportHelper.createDatetimeStyle(wb);
            CellStyle defaultStyle = ExcelExportHelper.createDefaultStyle(wb);

            ExcelExportHelper.createHeaderRow(sheet.createRow(0), HEADERS, headerStyle);

            int rowIndex = 1;
            long offset = 0;
            while (true) {
                List<Map<String, Object>> batch = fetchComponentBatch(filter, offset);
                if (batch.isEmpty())
                    break;

                for (Map<String, Object> data : batch) {
                    Row row = sheet.createRow(rowIndex++);
                    ExcelExportHelper.setCellString(row, 0, data.get("COMPONENT_CODE"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 1, data.get("COMPONENT_NAME"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 2, data.get("MESSAGE_TYPE"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 3, data.get("CONNECTION_METHOD"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 4, data.get("CHECK_TOKEN"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 5, formatStatus(data.get("STATUS")), defaultStyle);
                    ExcelExportHelper.setCellString(row, 6, formatIsActive(data.get("IS_ACTIVE")), defaultStyle);
                    ExcelExportHelper.setCellDate(row, 7, data.get("EFFECTIVE_DATE"), dateStyle);
                    ExcelExportHelper.setCellDate(row, 8, data.get("END_EFFECTIVE_DATE"), dateStyle);
                    if (row.getRowNum() == 1) {
                        ExcelExportHelper.setCellString(row, 9, filterSummary, defaultStyle);
                    }
                    processedRows++;
                    if (processedRows % 5_000 == 0) {
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
            try (OutputStream os = Files.newOutputStream(tempFile)) {
                wb.write(os);
            }
        }
        return processedRows;
    }

    private long countComponentFilteredRows(CreateExportComponentJobRequestDTO filter) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM PMH_COMPONENTS WHERE 1=1");
        appendComponentFilterClause(sql, params, filter);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    private List<Map<String, Object>> fetchComponentBatch(CreateExportComponentJobRequestDTO filter, long offset) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT COMPONENT_CODE, COMPONENT_NAME, MESSAGE_TYPE, CONNECTION_METHOD, " +
                        "CHECK_TOKEN, STATUS, IS_ACTIVE, EFFECTIVE_DATE, END_EFFECTIVE_DATE " +
                        "FROM PMH_COMPONENTS WHERE 1=1");
        appendComponentFilterClause(sql, params, filter);

        sql.append(" ").append(resolveSortClause(filter));
        sql.append(" OFFSET ? ROWS FETCH FIRST ? ROWS ONLY");
        params.add(offset);
        params.add(BATCH_SIZE);

        jdbcTemplate.setFetchSize(BATCH_SIZE);
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    private String resolveSortClause(CreateExportComponentJobRequestDTO filter) {
        String sortBy = filter != null && filter.getSortBy() != null ? filter.getSortBy().trim() : "componentCode";
        String dir = filter != null && "desc".equalsIgnoreCase(filter.getSortDirection()) ? "DESC" : "ASC";

        String col;
        switch (sortBy) {
            case "componentName" -> col = "COMPONENT_NAME";
            case "messageType" -> col = "MESSAGE_TYPE";
            case "connectionMethod" -> col = "CONNECTION_METHOD";
            case "checkToken" -> col = "CHECK_TOKEN";
            case "status" -> col = "STATUS";
            case "isActive" -> col = "IS_ACTIVE";
            case "effectiveDate" -> col = "EFFECTIVE_DATE";
            case "endEffectiveDate" -> col = "END_EFFECTIVE_DATE";
            default -> col = "COMPONENT_CODE";
        }
        if ("COMPONENT_CODE".equalsIgnoreCase(col)) {
            return "ORDER BY COMPONENT_CODE " + dir;
        }
        return "ORDER BY " + col + " " + dir + ", COMPONENT_CODE " + dir;
    }

    private void appendComponentFilterClause(StringBuilder sql, List<Object> params,
            CreateExportComponentJobRequestDTO filter) {
        if (filter == null)
            return;

        if (filter.getComponentCode() != null && !filter.getComponentCode().isBlank()) {
            sql.append(" AND UPPER(COMPONENT_CODE) LIKE UPPER(?)");
            params.add("%" + filter.getComponentCode().trim() + "%");
        }
        if (filter.getComponentName() != null && !filter.getComponentName().isBlank()) {
            sql.append(" AND UPPER(COMPONENT_NAME) LIKE UPPER(?)");
            params.add("%" + filter.getComponentName().trim() + "%");
        }
        appendInClause(sql, params, "STATUS", filter.getStatus());
        appendInClause(sql, params, "IS_ACTIVE", filter.getIsActive());
    }
}
