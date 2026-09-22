package com.example.paymenthub.service.export.worker;

import com.example.paymenthub.common.util.JsonUtils;
import com.example.paymenthub.dto.request.CreateExportCategoryJobRequestDTO;
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
public class CategoryExportWorker extends BaseExportWorker {

    private static final String[] HEADERS = {
            "ID", "Nhóm danh mục", "Giá trị thành phần", "Tên thành phần",
            "Mô tả", "Mã cấu phần xử lý", "Trạng thái", "Hoạt động",
            "Ngày hiệu lực", "Ngày hết hiệu lực", "Điều Kiện Lọc"
    };

    public CategoryExportWorker(ExportJobRepository exportJobRepository,
            ExportLockService exportLockService,
            ExportProgressService exportProgressService,
            FileStorageService fileStorageService,
            JdbcTemplate jdbcTemplate) {
        super(exportJobRepository, exportLockService, exportProgressService, fileStorageService, jdbcTemplate);
    }

    @Override
    protected String getWorkerName() {
        return "Category";
    }

    @Override
    protected String getTempFilePrefix() {
        return "cat_export_";
    }

    @Override
    protected String getFinalFileNamePrefix() {
        return "Category";
    }

    @Override
    protected long writeExportFile(ExportJob job, Path tempFile) throws Exception {
        CreateExportCategoryJobRequestDTO filter = JsonUtils.fromJson(
                job.getFilterPayload(), CreateExportCategoryJobRequestDTO.class);
        return writeCategoryXlsxFile(job, filter, tempFile);
    }

    private long writeCategoryXlsxFile(ExportJob job, CreateExportCategoryJobRequestDTO filter, Path tempFile)
            throws Exception {
        long processedRows = 0;
        long totalRows = countCategoryFilteredRows(filter);

        exportProgressService.updateProgress(String.valueOf(job.getId()), 0, totalRows);
        job.setTotalRows(totalRows);
        exportJobRepository.save(job);

        String filterSummary = ExcelExportHelper.buildCategoryFilterSummary(filter);

        try (SXSSFWorkbook wb = new SXSSFWorkbook(ROW_ACCESS)) {
            wb.setCompressTempFiles(false);
            SXSSFSheet sheet = wb.createSheet("Danh mục tham số");
            sheet.createFreezePane(0, 1);

            CellStyle headerStyle = ExcelExportHelper.createHeaderStyle(wb);
            CellStyle dateStyle = ExcelExportHelper.createDatetimeStyle(wb);
            CellStyle defaultStyle = ExcelExportHelper.createDefaultStyle(wb);
            CellStyle numStyle = ExcelExportHelper.createNumberStyle(wb);

            ExcelExportHelper.createHeaderRow(sheet.createRow(0), HEADERS, headerStyle);

            int rowIndex = 1;
            long offset = 0;
            while (true) {
                List<Map<String, Object>> batch = fetchCategoryBatch(filter, offset);
                if (batch.isEmpty())
                    break;

                for (Map<String, Object> data : batch) {
                    Row row = sheet.createRow(rowIndex++);
                    ExcelExportHelper.setCellLong(row, 0, data.get("ID"), numStyle);
                    ExcelExportHelper.setCellString(row, 1, data.get("PARAM_TYPE"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 2, data.get("PARAM_VALUE"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 3, data.get("PARAM_NAME"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 4, data.get("DESCRIPTION"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 5, data.get("COMPONENT_CODE"), defaultStyle);
                    ExcelExportHelper.setCellString(row, 6, formatStatus(data.get("STATUS")), defaultStyle);
                    ExcelExportHelper.setCellString(row, 7, formatIsActive(data.get("IS_ACTIVE")), defaultStyle);
                    ExcelExportHelper.setCellDate(row, 8, data.get("EFFECTIVE_DATE"), dateStyle);
                    ExcelExportHelper.setCellDate(row, 9, data.get("END_EFFECTIVE_DATE"), dateStyle);
                    if (row.getRowNum() == 1) {
                        ExcelExportHelper.setCellString(row, 10, filterSummary, defaultStyle);
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

    private long countCategoryFilteredRows(CreateExportCategoryJobRequestDTO filter) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM PMH_GROUP_CATEGORY WHERE 1=1");
        appendCategoryFilterClause(sql, params, filter);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    private List<Map<String, Object>> fetchCategoryBatch(CreateExportCategoryJobRequestDTO filter, long offset) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT ID, PARAM_TYPE, PARAM_VALUE, PARAM_NAME, DESCRIPTION, COMPONENT_CODE, " +
                        "STATUS, IS_ACTIVE, EFFECTIVE_DATE, END_EFFECTIVE_DATE " +
                        "FROM PMH_GROUP_CATEGORY WHERE 1=1");
        appendCategoryFilterClause(sql, params, filter);

        sql.append(" ").append(resolveSortClause(filter));
        sql.append(" OFFSET ? ROWS FETCH FIRST ? ROWS ONLY");
        params.add(offset);
        params.add(BATCH_SIZE);

        jdbcTemplate.setFetchSize(BATCH_SIZE);
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    private String resolveSortClause(CreateExportCategoryJobRequestDTO filter) {
        String sortBy = filter != null && filter.getSortBy() != null ? filter.getSortBy().trim() : "id";
        String dir = filter != null && "asc".equalsIgnoreCase(filter.getSortDirection()) ? "ASC" : "DESC";

        String col;
        switch (sortBy) {
            case "paramType" -> col = "PARAM_TYPE";
            case "paramValue" -> col = "PARAM_VALUE";
            case "paramName" -> col = "PARAM_NAME";
            case "componentCode" -> col = "COMPONENT_CODE";
            case "status" -> col = "STATUS";
            case "isActive" -> col = "IS_ACTIVE";
            case "effectiveDate" -> col = "EFFECTIVE_DATE";
            case "endEffectiveDate" -> col = "END_EFFECTIVE_DATE";
            default -> col = "ID";
        }
        if ("ID".equalsIgnoreCase(col)) {
            return "ORDER BY ID " + dir;
        }
        return "ORDER BY " + col + " " + dir + ", ID " + dir;
    }

    private void appendCategoryFilterClause(StringBuilder sql, List<Object> params,
            CreateExportCategoryJobRequestDTO filter) {
        if (filter == null)
            return;

        if (filter.getParamType() != null && !filter.getParamType().isBlank()) {
            sql.append(" AND UPPER(PARAM_TYPE) LIKE UPPER(?)");
            params.add("%" + filter.getParamType().trim() + "%");
        }
        if (filter.getParamValue() != null && !filter.getParamValue().isBlank()) {
            sql.append(" AND UPPER(PARAM_VALUE) LIKE UPPER(?)");
            params.add("%" + filter.getParamValue().trim() + "%");
        }
        if (filter.getParamName() != null && !filter.getParamName().isBlank()) {
            sql.append(" AND UPPER(PARAM_NAME) LIKE UPPER(?)");
            params.add("%" + filter.getParamName().trim() + "%");
        }
        appendInClause(sql, params, "STATUS", filter.getStatus());
        appendInClause(sql, params, "IS_ACTIVE", filter.getIsActive());
    }
}
