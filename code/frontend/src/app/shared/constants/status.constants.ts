/**
 * Status constants dùng chung cho cả category và component.
 * Tập trung tại đây để tránh duplicate ở 4+ nơi.
 */

// ─── Status codes ───────────────────────────────────────────────────────────
export const APPROVAL_STATUS = {
  NEW: 1,
  PENDING: 3,
  APPROVED: 4,
  REJECTED: 5,
  CANCELED: 7
} as const;

export const DISPLAY_STATUS = {
  NOT_APPROVED: 1,
  APPROVED: 2
} as const;

export const IS_ACTIVE = {
  INACTIVE: 0,
  ACTIVE: 1
} as const;

// ─── Status map dùng cho badge ───────────────────────────────────────────────
export type StatusLabelKey = 'new' | 'pending' | 'approved' | 'rejected' | 'canceled';

export const STATUS_MAP: Record<number, { labelKey: StatusLabelKey; css: string }> = {
  [APPROVAL_STATUS.NEW]: { labelKey: 'new', css: 'badge-new' },
  [APPROVAL_STATUS.PENDING]: { labelKey: 'pending', css: 'badge-pending' },
  [APPROVAL_STATUS.APPROVED]: { labelKey: 'approved', css: 'badge-approved' },
  [APPROVAL_STATUS.REJECTED]: { labelKey: 'rejected', css: 'badge-rejected' },
  [APPROVAL_STATUS.CANCELED]: { labelKey: 'canceled', css: 'badge-canceled' }
};

// ─── Options cho Select ──────────────────────────────────────────────────────
export const APPROVAL_STATUS_OPTIONS = [
  { value: APPROVAL_STATUS.NEW, label: '1 - Tạo mới' },
  { value: APPROVAL_STATUS.PENDING, label: '3 - Chờ duyệt' },
  { value: APPROVAL_STATUS.APPROVED, label: '4 - Đã phê duyệt' },
  { value: APPROVAL_STATUS.REJECTED, label: '5 - Từ chối' },
  { value: APPROVAL_STATUS.CANCELED, label: '6 - Hủy duyệt' }
];

export const IS_ACTIVE_OPTIONS = [
  { value: IS_ACTIVE.ACTIVE, label: 'Hoạt động' },
  { value: IS_ACTIVE.INACTIVE, label: 'Không hoạt động' }
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
