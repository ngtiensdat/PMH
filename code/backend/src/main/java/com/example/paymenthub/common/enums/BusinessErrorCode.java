package com.example.paymenthub.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Danh mục mã lỗi và thông điệp lỗi quy tắc nghiệp vụ hệ thống (Business Rules).
 */
@Getter
@RequiredArgsConstructor
public enum BusinessErrorCode {

    PENDING_EDIT_NOT_ALLOWED("BUS_001", "Không được phép chỉnh sửa bản ghi đang ở trạng thái Chờ duyệt!"),
    DATA_UNCHANGED_UPDATE("BUS_002", "Dữ liệu cập nhật trùng khớp 100% với dữ liệu đang vận hành, không có thay đổi nào để gửi duyệt!"),
    APPROVED_RECORD_DELETE_NOT_ALLOWED("BUS_003", "Bản ghi đã từng được phê duyệt (isDisplay = 2) là bản ghi chuẩn của hệ thống, không được phép xóa!"),
    PENDING_RECORD_DELETE_NOT_ALLOWED("BUS_004", "Bản ghi đang ở trạng thái Chờ duyệt (STATUS = 3), không được phép xóa!"),
    INVALID_SUBMIT_STATUS("BUS_005", "Chỉ được phép gửi duyệt bản ghi ở trạng thái Mới (1), Từ chối (5) hoặc Hủy duyệt (7)!"),
    DATA_UNCHANGED_SUBMIT("BUS_006", "Bản ghi chưa có bất kỳ thay đổi nào so với dữ liệu đã duyệt, không cần gửi duyệt lại!"),
    INVALID_APPROVE_STATUS("BUS_007", "Chỉ được phép phê duyệt bản ghi đang ở trạng thái Chờ duyệt (STATUS = 3)!"),
    INVALID_REJECT_STATUS("BUS_008", "Chỉ được phép từ chối bản ghi đang ở trạng thái Chờ duyệt (STATUS = 3)!"),
    MAKER_CHECKER_SAME_USER("BUS_009", "Người phê duyệt/từ chối không được trùng với người tạo/gửi duyệt!"),
    COMPONENT_CODE_EXISTS("BUS_010", "Mã cấu phần đã tồn tại!"),
    DATA_DECODE_ERROR("BUS_011", "Lỗi giải mã dữ liệu thay đổi!"),
    INVALID_EFFECTIVE_DATE("BUS_012", "Ngày hiệu lực không được để trống!"),
    INVALID_DATE_RANGE("BUS_013", "Ngày hết hiệu lực phải sau ngày hiệu lực!");

    private final String code;
    private final String message;
}
