/**
 * Date utility functions dùng chung.
 * Loại bỏ duplicate parseDateString() ở category-list và component-list.
 */

/**
 * Parse date string từ nhiều định dạng về ISO string.
 * Hỗ trợ: ISO (yyyy-MM-ddTHH:mm:ss), yyyy-MM-dd, dd/MM/yyyy
 */
export function parseDateString(dateStr: string): string | null {
  if (!dateStr) return null;

  // ISO format: "2024-09-24T04:18:40" hoặc "2024-09-24T04:18:40.000Z"
  if (dateStr.includes('-') && dateStr.includes('T')) {
    return dateStr;
  }

  // yyyy-MM-dd format
  if (dateStr.includes('-') && !dateStr.includes('T')) {
    return dateStr;
  }

  // dd/MM/yyyy format
  if (dateStr.includes('/')) {
    const parts = dateStr.split('/');
    if (parts.length === 3) {
      const day   = parts[0].padStart(2, '0');
      const month = parts[1].padStart(2, '0');
      const year  = parts[2];
      return `${year}-${month}-${day}T00:00:00`;
    }
  }

  return dateStr;
}

/**
 * Format Date hoặc ISO string sang định dạng dd/MM/yyyy để hiển thị.
 */
export function formatDateDisplay(dateStr: string | null | undefined): string {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return '-';
  const day   = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year  = d.getFullYear();
  return `${day}/${month}/${year}`;
}

/**
 * Convert date string sang ISO string để gửi lên API.
 */
export function formatToISO(dateStr: string): string {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

/**
 * Kiểm tra ngày kết thúc phải sau ngày bắt đầu.
 */
export function isEndAfterStart(startStr: string, endStr: string): boolean {
  if (!startStr || !endStr) return true;
  return new Date(endStr) > new Date(startStr);
}
