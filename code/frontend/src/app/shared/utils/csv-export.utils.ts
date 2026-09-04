export function exportToCsv<T>(
  data: T[],
  headers: string[],
  rowMapper: (row: T) => (string | number | boolean | null | undefined)[],
  filename: string
): void {
  if (!data || data.length === 0) return;

  let csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + headers.join(',') + '\n';

  data.forEach((item) => {
    const rowValues = rowMapper(item).map((val) => {
      if (val === null || val === undefined) return '""';
      const str = String(val).replace(/"/g, '""');
      return `"${str}"`;
    });
    csvContent += rowValues.join(',') + '\n';
  });

  const encodedUri = encodeURI(csvContent);
  const link = document.createElement('a');
  link.setAttribute('href', encodedUri);
  link.setAttribute('download', `${filename}_${Date.now()}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}
