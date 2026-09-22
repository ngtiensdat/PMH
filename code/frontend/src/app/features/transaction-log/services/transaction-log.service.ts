import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/models/api-response.model';
import {
  TransactionLogResponse,
  TransactionLogFilter,
  ExportJobResponse
} from '../../../shared/models/transaction-log.model';

/**
 * Service cho module Transaction Log.
 *
 * Các API Export Job:
 *  - POST /api/transaction-log/export-jobs        → Tạo job, backend trả 202
 *  - GET  /api/transaction-log/export-jobs/active → Polling tiến độ (Redis cache)
 *  - GET  /api/transaction-log/export-jobs/my-jobs → Lịch sử 12h (Notification Bell)
 *  - GET  /api/transaction-log/export-jobs/{jobId}/download → Presigned URL / local URL
 */
@Injectable({ providedIn: 'root' })
export class TransactionLogService {
  private readonly apiBase = `${environment.apiBase}/api/transaction-log`;

  constructor(private http: HttpClient) {}

  // ── Search ────────────────────────────────────────────────────────────────

  search(
    filters: TransactionLogFilter,
    page = 0,
    size = 20,
    sort = 'id,desc'
  ): Observable<ApiResponse<PageResponse<TransactionLogResponse>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (filters.accountNo)        params = params.set('accountNo',        filters.accountNo);
    if (filters.transactionCode)  params = params.set('transactionCode',  filters.transactionCode);
    if (filters.status)           params = params.set('status',           filters.status);
    if (filters.fromDate)         params = params.set('fromDate',         filters.fromDate);
    if (filters.toDate)           params = params.set('toDate',           filters.toDate);

    return this.http.get<ApiResponse<PageResponse<TransactionLogResponse>>>(
      `${this.apiBase}/search`,
      { params }
    );
  }

  // ── Export Job ────────────────────────────────────────────────────────────

  /**
   * Tạo Export Job — Backend trả 202 Accepted trong < 100ms.
   * Không block UI, không chờ xuất xong.
   */
  createExportJob(filters: TransactionLogFilter): Observable<ApiResponse<ExportJobResponse>> {
    return this.http.post<ApiResponse<ExportJobResponse>>(
      `${this.apiBase}/export-jobs`,
      filters
    );
  }

  /**
   * Polling tiến độ job đang chạy (PENDING/PROCESSING).
   * Backend đọc từ Redis Cache — không query Oracle, nhanh < 5ms.
   * Trả về null nếu không có job nào đang chạy.
   */
  getActiveJob(): Observable<ApiResponse<ExportJobResponse | null>> {
    return this.http.get<ApiResponse<ExportJobResponse | null>>(
      `${this.apiBase}/export-jobs/active`
    );
  }

  /**
   * Lịch sử jobs trong 12h gần nhất của user.
   * Dùng cho Header Notification Bell.
   */
  getMyJobs(): Observable<ApiResponse<ExportJobResponse[]>> {
    return this.http.get<ApiResponse<ExportJobResponse[]>>(
      `${this.apiBase}/export-jobs/my-jobs`
    );
  }

  /**
   * Sinh URL tải file CSV đã xuất.
   * MinIO: Presigned URL (browser tải trực tiếp từ MinIO).
   * Local: URL endpoint Spring Boot nội bộ.
   * jobId là Long (number) từ backend.
   */
  getDownloadUrl(jobId: number): Observable<ApiResponse<string>> {
    return this.http.get<ApiResponse<string>>(
      `${this.apiBase}/export-jobs/${jobId}/download`
    );
  }
}
