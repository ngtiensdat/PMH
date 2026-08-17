/**
 * Trạng thái tham số trong quy trình phê duyệt Maker - Checker
 */
export enum ParamStatus {
  NEW = 1,        // Tạo mới
  PENDING = 3,    // Chờ duyệt
  APPROVED = 4,   // Đã duyệt
  REJECTED = 5,   // Từ chối
  CANCELED = 7    // Hủy duyệt
}

/**
 * Tình trạng hoạt động của tham số phục vụ hệ thống Payment Hub runtime
 */
export enum ActiveStatus {
  INACTIVE = 0,   // Không hoạt động
  ACTIVE = 1      // Đang hoạt động
}

/**
 * Trạng thái hiển thị đối chiếu dữ liệu
 */
export enum DisplayStatus {
  INITIAL = 1,        // Chưa từng duyệt
  ONCE_APPROVED = 2   // Đã từng duyệt
}

/**
 * Chế độ làm việc của màn hình Form
 */
export enum FormMode {
  ADD = 'add',
  EDIT = 'edit',
  COPY = 'copy'
}

/**
 * Module trong hệ thống Payment Hub
 */
export enum ModuleType {
  GROUP_CATEGORY = 'GROUP_CATEGORY',
  COMPONENT = 'COMPONENT'
}
