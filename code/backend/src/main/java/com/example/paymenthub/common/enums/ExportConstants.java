package com.example.paymenthub.common.enums;

/**
 * Hằng số cấu hình tập trung cho hệ thống Xuất Báo Cáo / Export Job.
 * Nằm trong package com.example.paymenthub.common.enums theo chuẩn tổ chức dự án.
 */
public final class ExportConstants {

    private ExportConstants() {}

    /** Kích thước lô truy vấn dữ liệu từ DB (50.000 dòng/batch cho hiệu năng tối đa) */
    public static final int DEFAULT_BATCH_SIZE = 50_000;

    /** Tần suất cập nhật tiến độ Redis (% trên UI nhảy mỗi 5.000 dòng mượt mà) */
    public static final int PROGRESS_UPDATE_STEP = 5_000;

    /** Kích thước cửa sổ trượt RAM của Apache POI SXSSFWorkbook (chỉ giữ 100 dòng trên RAM) */
    public static final int EXCEL_ROW_ACCESS_WINDOW = 100;

    /** Độ rộng mặc định cho các cột Excel (22 ký tự * 256 units) */
    public static final int EXCEL_DEFAULT_COL_WIDTH = 22 * 256;
}
