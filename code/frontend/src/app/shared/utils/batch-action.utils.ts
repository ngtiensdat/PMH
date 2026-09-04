import { ParamStatus } from '../enums/status.enum';

export interface SelectableRecord {
  status?: number;
  STATUS?: number;
  updatedBy?: string;
  UPDATED_BY?: string;
  createdBy?: string;
  CREATED_BY?: string;
}

export function isItemSelectable<T extends SelectableRecord>(
  item: T,
  currentUsername: string
): boolean {
  const status = item.status ?? item.STATUS;
  if (status !== ParamStatus.PENDING) {
    return false;
  }
  if (currentUsername) {
    const pendingMaker = item.updatedBy || item.UPDATED_BY || item.createdBy || item.CREATED_BY || '';
    if (pendingMaker && pendingMaker.toString().toLowerCase() === currentUsername.toLowerCase()) {
      return false;
    }
  }
  return true;
}

export function isAllItemsSelected<T extends SelectableRecord>(
  items: T[],
  selectedKeys: (string | number)[],
  getKeyFn: (item: T) => string | number,
  currentUsername: string
): boolean {
  const selectableItems = items.filter((item) => isItemSelectable(item, currentUsername));
  if (selectableItems.length === 0) return false;
  return selectableItems.every((item) => selectedKeys.includes(getKeyFn(item)));
}

export function toggleAllSelection<T extends SelectableRecord>(
  items: T[],
  getKeyFn: (item: T) => string | number,
  currentUsername: string,
  event: Event
): (string | number)[] {
  const checked = (event.target as HTMLInputElement).checked;
  if (checked) {
    return items.filter((item) => isItemSelectable(item, currentUsername)).map(getKeyFn);
  }
  return [];
}
