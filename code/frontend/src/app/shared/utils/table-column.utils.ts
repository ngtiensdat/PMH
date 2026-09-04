export interface TableColumnDef {
  id: string;
  label: string;
  isFixed?: boolean;
  width: number;
}

export function computeNextSort(
  currentField: string,
  currentDir: string,
  targetColId: string,
  defaultField: string = 'updatedDate'
): { sortField: string; sortDirection: string } {
  if (currentField === targetColId) {
    if (currentDir === 'asc') {
      return { sortField: targetColId, sortDirection: 'desc' };
    } else {
      if (targetColId !== defaultField) {
        return { sortField: defaultField, sortDirection: 'desc' };
      } else {
        return { sortField: targetColId, sortDirection: 'asc' };
      }
    }
  }
  return { sortField: targetColId, sortDirection: 'asc' };
}

export function reorderTableColumns<T extends TableColumnDef>(
  columns: T[],
  draggedIndex: number | null,
  targetColId: string
): T[] {
  const targetIndex = columns.findIndex((c) => c.id === targetColId);
  if (draggedIndex === null || targetIndex === -1 || columns[targetIndex].isFixed) {
    return columns;
  }
  const draggedCol = columns[draggedIndex];
  const updated = [...columns];
  updated.splice(draggedIndex, 1);
  updated.splice(targetIndex, 0, draggedCol);
  return updated;
}
