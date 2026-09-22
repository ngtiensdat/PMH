import {
  Component, OnInit, OnDestroy, signal, inject, computed
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Observable, interval, Subscription, EMPTY } from 'rxjs';
import { switchMap, takeWhile, catchError } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';

import { BaseListComponent } from '../../../../shared/components/base-list/base-list.component';
import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';
import { TableColumnDef } from '../../../../shared/utils/table-column.utils';
import { formatVND, getTxnStatusClass, getTxnStatusLabel } from '../../../../shared/utils/format.utils';

import { tuiInputDateTimeOptionsProvider } from '@taiga-ui/kit';
import { DateTimeTransformer } from '../../../../shared/utils/datetime-transformer';

import { TransactionLogService } from '../../services/transaction-log.service';
import { ApiResponse, BatchItemResult } from '../../../../shared/models/api-response.model';
import {
  TransactionLogResponse,
  TransactionLogFilter,
  ExportJobResponse,
  ExportJobStatus,
  computeExportProgress
} from '../../../../shared/models/transaction-log.model';
import { environment } from '../../../../../environments/environment';

import { TransactionLogSearchSchema, zodSearchFormValidator } from '../../../../shared/validators/transaction-log.schema';
import { ExportConfirmDialogComponent } from '../../../../shared/components/export-confirm-dialog/export-confirm-dialog';
import { ExportProgressBannerComponent } from '../../../../shared/components/export-banner/export-banner';

@Component({
  selector: 'app-transaction-log-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SharedTaigaModule,
    DatePipe,
    ExportConfirmDialogComponent,
    ExportProgressBannerComponent
  ],
  providers: [tuiInputDateTimeOptionsProvider({ valueTransformer: new DateTimeTransformer() })],
  templateUrl: './transaction-log-list.html',
  styleUrl: './transaction-log-list.css'
})
export class TransactionLogListComponent
  extends BaseListComponent<TransactionLogResponse, number>
  implements OnInit, OnDestroy {

  // ── Config ─────────────────────────────────────────────────────────────────
  protected override readonly batchUnit = 'giao dịch';
  protected override readonly permissionPrefix = 'TXN_LOG';

  // ── Service ────────────────────────────────────────────────────────────────
  private txnService = inject(TransactionLogService);

  // ── Form với Zod validation ────────────────────────────────────────────────
  searchForm: FormGroup = inject(FormBuilder).group(
    {
      accountNo: [''],
      transactionCode: [''],
      status: [''],
      fromDate: [''],
      toDate: ['']
    },
    { validators: zodSearchFormValidator(TransactionLogSearchSchema) }
  );

  // ── Data state ─────────────────────────────────────────────────────────────
  transactions = signal<TransactionLogResponse[]>([]);

  // ── Selection (Read-only: không cần chọn hàng, set no-op) ─────────────────
  private readonly _selectedKeys = signal<number[]>([]);
  protected override getSelectedKeys() { return this._selectedKeys(); }
  protected override setSelectedKeys(keys: number[]) { this._selectedKeys.set(keys); }
  protected override updateSelectedKeys(fn: (k: number[]) => number[]) { this._selectedKeys.update(fn); }
  protected override getItemKey(item: TransactionLogResponse) { return item.id; }
  protected override getListItems() { return this.transactions(); }

  // ── Read-only: no-op implementations cho abstract service calls ────────────
  protected override executeBatchApprove(_keys: number[]): Observable<ApiResponse<BatchItemResult[]>> { return EMPTY; }
  protected override executeBatchReject(_keys: number[], _r: string): Observable<ApiResponse<BatchItemResult[]>> { return EMPTY; }
  protected override executeDelete(_key: number): Observable<ApiResponse<unknown>> { return EMPTY; }
  protected override executeSendApproval(_key: number): Observable<ApiResponse<unknown>> { return EMPTY; }
  protected override executeCancelApproval(_key: number): Observable<ApiResponse<unknown>> { return EMPTY; }

  protected override readonly defaultSortField = 'createdAt';

  // ── Columns ────────────────────────────────────────────────────────────────
  override columns: TableColumnDef[] = [
    { id: 'stt', label: 'STT', isFixed: true, width: 60 },
    { id: 'transactionCode', label: 'Mã giao dịch', isFixed: true, width: 180 },
    { id: 'accountNo', label: 'Số tài khoản', isFixed: false, width: 155 },
    { id: 'amount', label: 'Số tiền', isFixed: false, width: 140 },
    { id: 'status', label: 'Trạng thái', isFixed: false, width: 130 },
    { id: 'referenceNo', label: 'Mã tham chiếu', isFixed: false, width: 155 },
    { id: 'description', label: 'Mô tả', isFixed: false, width: 220 },
    { id: 'createdAt', label: 'Thời gian tạo', isFixed: false, width: 160 },
    { id: 'updatedAt', label: 'Cập nhật lúc', isFixed: false, width: 160 }
  ];



  override get displayColumns(): TableColumnDef[] { return this.columns; }

  // ── Transaction status select options ────────────────────────────────────
  readonly statusItems: string[] = ['', 'SUCCESS', 'FAILED'];
  readonly stringifyTxnStatus = (val: string): string => val ? getTxnStatusLabel(val) : 'Tất cả';
  readonly getTxnStatusClass = getTxnStatusClass;
  readonly getTxnStatusLabel = getTxnStatusLabel;
  readonly formatAmount = (amount: number) => formatVND(amount);

  trackById(_: number, item: TransactionLogResponse): number { return item.id; }

  // ── Export Job state ───────────────────────────────────────────────────────
  activeExportJob = signal<ExportJobResponse | null>(null);
  exportProgress = computed(() => {
    const job = this.activeExportJob();
    return job ? computeExportProgress(job) : 0;
  });
  isExporting = computed(() => {
    const s = this.activeExportJob()?.status;
    return s === 'PENDING' || s === 'PROCESSING';
  });
  exportError = signal<string | null>(null);

  /** Confirm Dialog state */
  showConfirmDialog = signal(false);
  confirmDialogMsg = signal('');

  private pollingSubscription: Subscription | null = null;

  // ── Lifecycle ──────────────────────────────────────────────────────────────
  override ngOnInit(): void {
    this.sortField.set('createdAt');
    this.sortDirection.set('desc');
    this.size.set(20);
    this.loadData();

    // 6.4: Khôi phục trạng thái khi F5 — gọi getActiveJob() ngay khi init
    this.restoreActiveJobState();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  // ── Data loading ───────────────────────────────────────────────────────────
  private buildFilters(): TransactionLogFilter {
    const v = this.searchForm.value;
    return {
      accountNo: v.accountNo || undefined,
      transactionCode: v.transactionCode || undefined,
      status: v.status || undefined,
      fromDate: v.fromDate || undefined,
      toDate: v.toDate || undefined,
      sortBy: this.sortField(),
      sortDirection: this.sortDirection()
    };
  }

  override loadData(): void {
    if (this.searchForm.invalid) {
      const errs = this.searchForm.errors;
      const msg = errs ? Object.values(errs)[0] : 'Dữ liệu bộ lọc không hợp lệ';
      this.notificationService.warning(String(msg));
      return;
    }

    this.isLoading.set(true);
    this.txnService.search(
      this.buildFilters(),
      this.page(),
      this.size(),
      `${this.sortField()},${this.sortDirection()}`
    ).subscribe({
      next: (res) => {
        this.transactions.set(res.data.content || []);
        this.totalElements.set(
          res.data.page?.totalElements ?? res.data.totalElements ?? 0
        );
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status !== 401 && this.authService.isLoggedIn()) {
          this.notificationService.error('Lỗi tải dữ liệu: ' + (err.error?.message || err.message));
        }
        this.isLoading.set(false);
      }
    });
  }

  onReset(): void {
    this.searchForm.reset({ accountNo: '', transactionCode: '', status: '', fromDate: '', toDate: '' });
    this.page.set(0);
    this.loadData();
  }

  // ── Export Job ─────────────────────────────────────────────────────────────

  /**
   * 6.4: Khôi phục trạng thái khi F5 / mở lại trình duyệt.
   * Gọi getActiveJob() ngay khi component init — nếu có job đang chạy thì kích hoạt lại polling.
   */
  private restoreActiveJobState(): void {
    this.txnService.getActiveJob().pipe(
      catchError(() => EMPTY)
    ).subscribe(res => {
      const job = res.data;
      if (job && (job.status === 'PENDING' || job.status === 'PROCESSING')) {
        this.activeExportJob.set(job);
        this.startPolling();
      }
    });
  }

  /**
   * 6.1: Hiển thị Confirm Dialog trước khi export.
   * Linh hoạt số lượng bản ghi dựa trên bộ lọc (totalElements).
   */
  onClickExport(): void {
    const total = this.totalElements();
    if (total === 0) {
      this.notificationService.warning('Không có bản ghi nào phù hợp với bộ lọc hiện tại để xuất.');
      return;
    }

    const filters = this.buildFilters();
    const hasFilter = !!(filters.accountNo || filters.transactionCode || filters.status
      || filters.fromDate || filters.toDate);

    const formattedTotal = total.toLocaleString('vi-VN');

    if (hasFilter) {
      this.confirmDialogMsg.set(
        `Bạn có chắc chắn muốn xuất ${formattedTotal} bản ghi giao dịch theo bộ lọc hiện tại sang file XLSX?`
      );
    } else {
      this.confirmDialogMsg.set(
        `Bạn có chắc chắn muốn xuất tất cả ${formattedTotal} bản ghi giao dịch sang file XLSX?`
      );
    }
    this.showConfirmDialog.set(true);
  }

  /** Người dùng xác nhận trong Dialog → thực sự gọi API tạo job */
  onConfirmExport(): void {
    this.showConfirmDialog.set(false);
    this.exportError.set(null);

    this.txnService.createExportJob(this.buildFilters()).subscribe({
      next: (res) => {
        this.activeExportJob.set(res.data);
        this.notificationService.success('Yêu cầu xuất file đã được tiếp nhận. Đang xử lý...');
        this.startPolling();
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 409) {
          this.notificationService.warning('Bạn đã có một job xuất file đang chạy. Vui lòng chờ hoàn thành.');
        } else if (err.status === 503) {
          this.notificationService.warning('Hệ thống đang bận tối đa. Vui lòng thử lại sau.');
        } else {
          const msg = err.error?.message || err.message;
          this.exportError.set(msg);
          this.notificationService.error('Lỗi tạo yêu cầu xuất file: ' + msg);
        }
      }
    });
  }

  onCancelExport(): void {
    this.showConfirmDialog.set(false);
  }

  /** Tải file CSV — jobId là number (Long từ backend) */
  onDownload(job: ExportJobResponse): void {
    this.txnService.getDownloadUrl(job.jobId).subscribe({
      next: (res) => {
        let url = res.data;
        // Local Storage trả đường dẫn tương đối "/api/..."
        // → phải ghép apiBase để tải từ Spring Boot (8080), không phải Angular dev server (4200)
        if (url && url.startsWith('/')) {
          url = environment.apiBase + url;
        }
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', job.fileName || `transaction_log_${job.jobId}.xlsx`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      },
      error: (err: HttpErrorResponse) => {
        this.notificationService.error('Lỗi tải file: ' + (err.error?.message || err.message));
      }
    });
  }

  isExpired(job: ExportJobResponse): boolean {
    if (!job.expiresAt) return false;
    return new Date(job.expiresAt) < new Date();
  }

  // ── Polling tiến độ (mỗi 3 giây) ─────────────────────────────────────────

  private startPolling(): void {
    this.stopPolling();
    this.pollingSubscription = interval(3000).pipe(
      switchMap(() => this.txnService.getActiveJob().pipe(
        catchError(() => EMPTY)
      )),
      takeWhile(() => {
        const status = this.activeExportJob()?.status;
        return status === 'PENDING' || status === 'PROCESSING';
      }, true)
    ).subscribe({
      next: (res) => {
        const job = res.data;
        this.activeExportJob.set(job);
        const isDone = job && (job.status === 'COMPLETED' || job.status === 'DONE' || job.status === 'SUCCESS');
        const isFailed = job && job.status === 'FAILED';
        if (isDone || isFailed) {
          this.stopPolling();
          if (isDone) {
            this.notificationService.success(`Xuất file hoàn thành! ${job.totalRows?.toLocaleString()} bản ghi.`);
          } else {
            this.notificationService.error('Xuất file thất bại: ' + (job!.errorMessage || 'Lỗi không xác định'));
          }
        }
      }
    });
  }

  private stopPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = null;
    }
  }
}
