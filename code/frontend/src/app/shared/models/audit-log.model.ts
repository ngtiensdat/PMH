export interface AuditLogItem {
  id: number;
  module: 'GROUP_CATEGORY' | 'COMPONENT';
  recordId: string;
  action: string;
  performedBy: string;
  actionDate: string;
  description: string;
  oldData?: string;
  newDataLog?: string;
  statusBefore?: number;
  statusAfter?: number;
  ipAddress?: string;
}
