package com.example.paymenthub.service.export;

import org.apache.poi.ss.usermodel.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

/**
 * Tiện ích hỗ trợ tạo style và ghi dữ liệu ra các ô trong Excel.
 */
public class ExcelExportHelper {

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // ─── Cell style builders ────────────────────────────────────────────────

    public static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(false);
        return style;
    }

    public static CellStyle createDefaultStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public static CellStyle createNumberStyle(Workbook wb) {
        CellStyle style = createDefaultStyle(wb);
        style.setAlignment(HorizontalAlignment.RIGHT);
        DataFormat fmt = wb.createDataFormat();
        style.setDataFormat(fmt.getFormat("#,##0"));
        return style;
    }

    public static CellStyle createDatetimeStyle(Workbook wb) {
        CellStyle style = createDefaultStyle(wb);
        DataFormat fmt = wb.createDataFormat();
        style.setDataFormat(fmt.getFormat("dd/MM/yyyy HH:mm:ss"));
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    // ─── Cell writers ────────────────────────────────────────────────────────

    public static void setCellString(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value.toString() : "");
        cell.setCellStyle(style);
    }

    public static void setCellLong(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Number n) cell.setCellValue(n.longValue());
        else cell.setCellValue(value != null ? value.toString() : "");
        cell.setCellStyle(style);
    }

    public static void setCellDecimal(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Number n) cell.setCellValue(n.doubleValue());
        else if (value instanceof BigDecimal bd) cell.setCellValue(bd.doubleValue());
        else cell.setCellValue(value != null ? value.toString() : "");
        cell.setCellStyle(style);
    }

    public static void setCellDate(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Timestamp ts) {
            cell.setCellValue(ts.toLocalDateTime().format(DT_FORMATTER));
        } else if (value instanceof java.time.LocalDateTime ldt) {
            cell.setCellValue(ldt.format(DT_FORMATTER));
        } else if (value instanceof java.util.Date d) {
            cell.setCellValue(new Timestamp(d.getTime()).toLocalDateTime().format(DT_FORMATTER));
        } else if (value != null) {
            cell.setCellValue(value.toString());
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    public static void createHeaderRow(Row row, String[] headers, CellStyle headerStyle) {
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    public static void autoSizeColumns(Sheet sheet, int columnCount) {
        // Đặt cố định độ rộng cột để đạt tốc độ xuất tối đa 0ms (thay vì mất 2-4 phút đo 10 triệu ô)
        for (int i = 0; i < columnCount; i++) {
            sheet.setColumnWidth(i, com.example.paymenthub.common.enums.ExportConstants.EXCEL_DEFAULT_COL_WIDTH);
        }
    }

    // ─── Filter summary string formatters ────────────────────────────────────

    public static String buildTransactionFilterSummary(com.example.paymenthub.dto.request.CreateExportJobRequestDTO filter) {
        if (filter == null || !filter.hasFilter()) {
            return "Điều kiện lọc: Tất cả dữ liệu (Không áp dụng bộ lọc)";
        }
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (filter.getAccountNo() != null && !filter.getAccountNo().isBlank()) {
            parts.add("Số TK: " + filter.getAccountNo().trim());
        }
        if (filter.getTransactionCode() != null && !filter.getTransactionCode().isBlank()) {
            parts.add("Mã GD: " + filter.getTransactionCode().trim());
        }
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            String st = filter.getStatus().trim();
            if ("SUCCESS".equalsIgnoreCase(st)) st = "Thành công";
            else if ("FAILED".equalsIgnoreCase(st)) st = "Thất bại";
            parts.add("Trạng thái: " + st);
        }
        if (filter.getFromDate() != null && !filter.getFromDate().isBlank()) {
            parts.add("Từ ngày: " + filter.getFromDate().trim());
        }
        if (filter.getToDate() != null && !filter.getToDate().isBlank()) {
            parts.add("Đến ngày: " + filter.getToDate().trim());
        }
        return "Điều kiện lọc: " + String.join("; ", parts);
    }

    public static String buildCategoryFilterSummary(com.example.paymenthub.dto.request.CreateExportCategoryJobRequestDTO filter) {
        if (filter == null || !filter.hasFilter()) {
            return "Điều kiện lọc: Tất cả dữ liệu (Không áp dụng bộ lọc)";
        }
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (filter.getParamType() != null && !filter.getParamType().isBlank()) {
            parts.add("Nhóm danh mục: " + filter.getParamType().trim());
        }
        if (filter.getParamValue() != null && !filter.getParamValue().isBlank()) {
            parts.add("Giá trị thành phần: " + filter.getParamValue().trim());
        }
        if (filter.getParamName() != null && !filter.getParamName().isBlank()) {
            parts.add("Tên thành phần: " + filter.getParamName().trim());
        }
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            parts.add("Trạng thái duyệt: " + filter.getStatus().toString());
        }
        if (filter.getIsActive() != null && !filter.getIsActive().isEmpty()) {
            parts.add("Hoạt động: " + filter.getIsActive().toString());
        }
        return "Điều kiện lọc: " + String.join("; ", parts);
    }

    public static String buildComponentFilterSummary(com.example.paymenthub.dto.request.CreateExportComponentJobRequestDTO filter) {
        if (filter == null || !filter.hasFilter()) {
            return "Điều kiện lọc: Tất cả dữ liệu (Không áp dụng bộ lọc)";
        }
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (filter.getComponentCode() != null && !filter.getComponentCode().isBlank()) {
            parts.add("Mã cấu phần: " + filter.getComponentCode().trim());
        }
        if (filter.getComponentName() != null && !filter.getComponentName().isBlank()) {
            parts.add("Tên cấu phần: " + filter.getComponentName().trim());
        }
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            parts.add("Trạng thái duyệt: " + filter.getStatus().toString());
        }
        if (filter.getIsActive() != null && !filter.getIsActive().isEmpty()) {
            parts.add("Hoạt động: " + filter.getIsActive().toString());
        }
        return "Điều kiện lọc: " + String.join("; ", parts);
    }
}

