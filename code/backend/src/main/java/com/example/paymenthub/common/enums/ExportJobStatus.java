package com.example.paymenthub.common.enums;

/**
 * Trạng thái của một Export Job xuất XLSX.
 * Vòng đời: PENDING → PROCESSING → DONE / FAILED
 *
 * Giá trị chuẩn khi Worker hoàn thành: DONE.
 * SUCCESS và COMPLETED là alias cũ — không còn được Worker ghi ra, chỉ giữ để tương thích
 * với các bản ghi cũ trong DB và tránh lỗi khi parse.
 */
public enum ExportJobStatus {

    /** Job vừa được khởi tạo, đang chờ ThreadPool chạy */
    PENDING,

    /** Worker đang xử lý — đọc batch, ghi XLSX, upload MinIO */
    PROCESSING,

    /** Xuất thành công — giá trị chuẩn được Worker ghi khi hoàn thành */
    DONE,

    /** @deprecated Alias cũ của DONE — Worker không còn ghi giá trị này, chỉ giữ để parse DB cũ */
    @Deprecated
    SUCCESS,

    /** @deprecated Alias cũ của DONE — Worker không còn ghi giá trị này, chỉ giữ để parse DB cũ */
    @Deprecated
    COMPLETED,

    /** Xuất thất bại — Worker crash, lỗi MinIO, lỗi DB... */
    FAILED,

    /** File đã quá hạn 12h — Scheduler cleanup tự động đặt trạng thái này */
    EXPIRED;

    /**
     * Kiểm tra xem trạng thái có đại diện cho "hoàn thành thành công" không.
     * Bao gồm cả DONE, SUCCESS, COMPLETED để tương thích với dữ liệu cũ.
     */
    public boolean isSuccess() {
        return this == DONE || this == SUCCESS || this == COMPLETED;
    }

    /**
     * Kiểm tra xem trạng thái có đại diện cho "đang chạy" không.
     */
    public boolean isRunning() {
        return this == PENDING || this == PROCESSING;
    }
}
