import { ParamStatus, ActiveStatus, DisplayStatus } from '../enums/status.enum';

/**
 * Status constants dùng chung cho cả category và component.
 * Tập trung tại đây để tránh duplicate ở 4+ nơi.
 */

// ─── Status codes (Đồng bộ với Enum) ─────────────────────────────────────────
export const APPROVAL_STATUS = {
  NEW: ParamStatus.NEW,
  PENDING: ParamStatus.PENDING,
  APPROVED: ParamStatus.APPROVED,
  REJECTED: ParamStatus.REJECTED,
  CANCELED: ParamStatus.CANCELED
} as const;

export const DISPLAY_STATUS = {
  NOT_APPROVED: DisplayStatus.INITIAL,
  APPROVED: DisplayStatus.ONCE_APPROVED
} as const;

export const IS_ACTIVE = {
  INACTIVE: ActiveStatus.INACTIVE,
  ACTIVE: ActiveStatus.ACTIVE
} as const;

// ─── Status map dùng cho badge ───────────────────────────────────────────────
export type StatusLabelKey = 'new' | 'pending' | 'approved' | 'rejected' | 'canceled';

export const STATUS_MAP: Record<number, { labelKey: StatusLabelKey; css: string }> = {
  [ParamStatus.NEW]: { labelKey: 'new', css: 'badge-new' },
  [ParamStatus.PENDING]: { labelKey: 'pending', css: 'badge-pending' },
  [ParamStatus.APPROVED]: { labelKey: 'approved', css: 'badge-approved' },
  [ParamStatus.REJECTED]: { labelKey: 'rejected', css: 'badge-rejected' },
  [ParamStatus.CANCELED]: { labelKey: 'canceled', css: 'badge-canceled' }
};

// ─── Options cho Select ──────────────────────────────────────────────────────
export const APPROVAL_STATUS_OPTIONS = [
  { value: ParamStatus.NEW, label: '1 - Tạo mới' },
  { value: ParamStatus.PENDING, label: '3 - Chờ duyệt' },
  { value: ParamStatus.APPROVED, label: '4 - Đã phê duyệt' },
  { value: ParamStatus.REJECTED, label: '5 - Từ chối' },
  { value: ParamStatus.CANCELED, label: '7 - Hủy duyệt' }
];

export const IS_ACTIVE_OPTIONS = [
  { value: ActiveStatus.ACTIVE, label: 'Hoạt động' },
  { value: ActiveStatus.INACTIVE, label: 'Không hoạt động' }
];

// ─── Action pill CSS ─────────────────────────────────────────────────────────
export const ACTION_PILL_MAP: Record<string, string> = {
  'Phê duyệt': 'pill-approve',
  'Hủy phê duyệt': 'pill-reject',
  'Hủy duyệt': 'pill-reject',
  'Gửi duyệt': 'pill-edit',
  'Sửa': 'pill-edit',
  'Tạo mới': 'pill-approve',
  'Cập nhật': 'pill-edit',
  'Gửi duyệt sửa': 'pill-edit',
  'Từ chối': 'pill-reject',
  'Xóa': 'pill-reject'
};
