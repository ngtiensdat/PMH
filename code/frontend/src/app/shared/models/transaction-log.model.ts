export interface TransactionLogResponse {
  /** Khóa chính */
  id: number;
  transactionCode: string;
  accountNo: string;
  amount: number;
  /** Trạng thái giao dịch: SUCCESS / FAILED */
  txnStatus: string;
  description: string | null;
  referenceNo: string | null;
  createdAt: string;
  updatedAt: string;

  updatedBy?: string;
  createdBy?: string;
}

/** Bộ lọc tìm kiếm — khớp với query params của Search API */
export interface TransactionLogFilter {
  accountNo?: string;
  transactionCode?: string;
  status?: string;
  fromDate?: string;
  toDate?: string;
  sortBy?: string;
  sortDirection?: string;
}

// ─── Export Job ─────────────────────────────────────────────────────────────

/** Trạng thái của Export Job */
export type ExportJobStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'SUCCESS' | 'DONE' | 'FAILED' | 'EXPIRED';

/** Response từ API Export Job — khớp với ExportJobResponseDTO.java */
export interface ExportJobResponse {
  jobId: number;              // Long PK từ Oracle DB (NUMBER auto-increment)
  status: ExportJobStatus;
  processedRows: number;
  totalRows: number | null;
  progressPercent: number;   // 0-100, backend đã tính sẵn
  fileName: string | null;   // Tên file gốc VD: Transaction_Log_20260916.csv
  downloadUrl: string | null;
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
  expiresAt: string | null;
  downloadCount?: number;
  isRead?: boolean;
  isFileDeleted?: boolean;
}

/** Phần trăm tiến độ — ưu tiên dùng progressPercent từ backend, fallback tính local */
export function computeExportProgress(job: ExportJobResponse): number {
  if (job.status === 'COMPLETED' || job.status === 'DONE' || job.status === 'SUCCESS') return 100;
  if (job.progressPercent != null && job.progressPercent >= 0) return job.progressPercent;
  if (!job.totalRows || job.totalRows === 0) return 0;
  return Math.min(99, Math.round((job.processedRows / job.totalRows) * 100));
}
