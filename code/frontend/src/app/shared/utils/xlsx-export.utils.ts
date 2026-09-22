import * as XLSX from 'xlsx';

/**
 * Xuất dữ liệu sang file XLSX với auto-fit cột và header in đậm.
 *
 * @param data       Mảng dữ liệu cần xuất
 * @param headers    Tên các cột tiêu đề (tiếng Việt)
 * @param rowMapper  Hàm map từng dòng dữ liệu → mảng giá trị theo thứ tự headers
 * @param filename   Tên file (không cần đuôi .xlsx)
 * @param sheetName  Tên sheet (mặc định: 'Dữ liệu')
 */
export function exportToXlsx<T>(
  data: T[],
  headers: string[],
  rowMapper: (row: T) => (string | number | boolean | null | undefined)[],
  filename: string,
  sheetName = 'Dữ liệu'
): void {
  if (!data || data.length === 0) return;

  // Tạo mảng rows: dòng đầu là header, các dòng tiếp là dữ liệu
  const rows: (string | number | boolean | null | undefined)[][] = [
    headers,
    ...data.map(rowMapper)
  ];

  // Tạo worksheet từ mảng 2D
  const ws = XLSX.utils.aoa_to_sheet(rows);

  // ── Auto-fit chiều rộng cột ────────────────────────────────────────────────
  const colWidths = headers.map((h, colIdx) => {
    // Độ rộng tiêu đề
    let maxLen = h.length;
    // Kiểm tra độ rộng tối đa trong từng cột
    data.forEach(row => {
      const val = rowMapper(row)[colIdx];
      const len = val != null ? String(val).length : 0;
      if (len > maxLen) maxLen = len;
    });
    // Giới hạn max 50 chars, min 10 chars, thêm 2 padding
    return { wch: Math.min(50, Math.max(10, maxLen + 2)) };
  });
  ws['!cols'] = colWidths;

  // ── Style header: in đậm ──────────────────────────────────────────────────
  // SheetJS CE không hỗ trợ styling đầy đủ (cần SheetJS Pro).
  // Dùng cách thay thế: freeze header row để luôn thấy tiêu đề khi cuộn.
  ws['!freeze'] = { xSplit: 0, ySplit: 1 }; // Freeze dòng đầu

  // Tạo workbook và ghi file
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, sheetName);

  const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '_').replace(/-/g, '');
  XLSX.writeFile(wb, `${filename}_${timestamp}.xlsx`);
}
