export function formatVND(amount: number | null | undefined): string {
  if (amount == null) return '—';
  return new Intl.NumberFormat('vi-VN').format(amount);
}

export function getTxnStatusClass(status: string | null | undefined): string {
  switch (status?.toUpperCase()) {
    case 'SUCCESS': return 'txn-badge txn-success';
    case 'FAILED': return 'txn-badge txn-failed';
    default: return 'txn-badge txn-default';
  }
}

export function getTxnStatusLabel(status: string | null | undefined): string {
  switch (status?.toUpperCase()) {
    case 'SUCCESS': return 'Thành công';
    case 'FAILED': return 'Thất bại';
    default: return status ?? '—';
  }
}
