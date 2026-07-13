package com.example.paymenthub.service;

import com.example.paymenthub.dto.response.AuditLogDTO;

import java.util.List;

public interface AuditLogService {

    /**
     * Ghi nhật ký một thao tác.
     *
     * @param module        Tên module: GROUP_CATEGORY | COMPONENT
     * @param recordId      ID bản ghi (String để dùng chung cho cả Long ID và String code)
     * @param action        Tên hành động: Tạo mới | Cập nhật | Xóa | Gửi duyệt | Phê duyệt | Từ chối
     * @param performedBy   Username người thực hiện
     * @param oldData       JSON object trước khi thay đổi (null nếu tạo mới)
     * @param newData       JSON object sau khi thay đổi (null nếu xóa)
     * @param description   Mô tả chi tiết
     * @param statusBefore  Trạng thái trước
     * @param statusAfter   Trạng thái sau
     */
    void log(String module, String recordId, String action, String performedBy,
             String oldData, String newData, String description,
             Integer statusBefore, Integer statusAfter);

    /**
     * Lấy lịch sử thao tác của một bản ghi.
     */
    List<AuditLogDTO> getHistory(String module, String recordId);
}
