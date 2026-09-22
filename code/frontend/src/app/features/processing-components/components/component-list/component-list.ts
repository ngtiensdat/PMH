import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormsModule } from '@angular/forms';
import { Observable, catchError, EMPTY } from 'rxjs';
import { ComponentService } from '../../services/component.service';
import { HttpErrorResponse } from '@angular/common/http';
import { TableColumnDef } from '../../../../shared/utils/table-column.utils';
import { ProcessingComponentResponse } from '../../../../shared/models/component.model';
import { ApiResponse, BatchItemResult } from '../../../../shared/models/api-response.model';
import { BaseListComponent } from '../../../../shared/components/base-list/base-list.component';
import { ExportJobResponse, computeExportProgress } from '../../../../shared/models/transaction-log.model';
import { environment } from '../../../../../environments/environment';

import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { RejectReasonDialogComponent } from '../../../../shared/components/reject-reason-dialog/reject-reason-dialog';
import { AuditHistoryDialogComponent } from '../../../../shared/components/audit-history-dialog/audit-history-dialog';
import { ExportConfirmDialogComponent } from '../../../../shared/components/export-confirm-dialog/export-confirm-dialog';
import { ExportProgressBannerComponent } from '../../../../shared/components/export-banner/export-banner';

@Component({
  selector: 'app-component-list',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, FormsModule, SharedTaigaModule,
    ConfirmDialogComponent, RejectReasonDialogComponent, AuditHistoryDialogComponent,
    ExportConfirmDialogComponent, ExportProgressBannerComponent
  ],
  templateUrl: './component-list.html',
  styleUrl: './component-list.css'
})
export class ComponentListComponent extends BaseListComponent<ProcessingComponentResponse, string> implements OnInit {

  // ── Config ─────────────────────────────────────────────────────────────────
  protected override readonly batchUnit        = 'cấu phần';
  protected override readonly permissionPrefix = 'COMPONENT';

  // ── Service ────────────────────────────────────────────────────────────────
  private componentService = inject(ComponentService);
  getHistoryFn = (code: string, page: number, size: number) => this.componentService.getHistory(code, page, size);

  // ── Form ───────────────────────────────────────────────────────────────────
  searchForm: FormGroup = inject(FormBuilder).group({
    componentCode: [[]], componentName: [[]], status: [[]], isActive: [[]]
  });

  // ── Filter option lists ────────────────────────────────────────────────────
  componentCodesList: { value: string; label: string }[] = [];
  componentNamesList: { value: string; label: string }[] = [];
  get componentCodeItems(): string[] { return [...new Set(this.componentCodesList.map(o => o.value).filter(Boolean))]; }
  get componentNameItems(): string[] { return [...new Set(this.componentNamesList.map(o => o.value).filter(Boolean))]; }

  // ── State ──────────────────────────────────────────────────────────────────
  components = signal<ProcessingComponentResponse[]>([]);

  // ── Selection (typed signal for template) ──────────────────────────────────
  readonly selectedCodes = signal<string[]>([]);
  protected override getSelectedKeys()                              { return this.selectedCodes(); }
  protected override setSelectedKeys(keys: string[])               { this.selectedCodes.set(keys); }
  protected override updateSelectedKeys(fn: (k: string[]) => string[]) { this.selectedCodes.update(fn); }

  // ── Key & list providers ───────────────────────────────────────────────────
  protected override getItemKey(item: ProcessingComponentResponse)  { return item.componentCode; }
  protected override getListItems()                                  { return this.components(); }

  // ── Service calls (1 line each) ────────────────────────────────────────────
  protected override executeBatchApprove(keys: string[]): Observable<ApiResponse<BatchItemResult[]>>
                        { return this.componentService.batchApprove(keys); }
  protected override executeBatchReject(keys: string[], reason: string): Observable<ApiResponse<BatchItemResult[]>>
                        { return this.componentService.batchReject(keys, reason); }
  protected override executeDelete(key: string): Observable<ApiResponse<unknown>>
                        { return this.componentService.delete(key); }
  protected override executeSendApproval(key: string): Observable<ApiResponse<unknown>>
                        { return this.componentService.sendApproval(key); }
  protected override executeCancelApproval(key: string): Observable<ApiResponse<unknown>>
                        { return this.componentService.cancelApproval(key); }

  // ── Columns ────────────────────────────────────────────────────────────────
  override columns: TableColumnDef[] = [
    { id: 'checkbox',         label: '',                      isFixed: true,  width: 40  },
    { id: 'stt',              label: 'STT',                   isFixed: true,  width: 60  },
    { id: 'componentCode',    label: 'Mã cấu phần',           isFixed: true,  width: 160 },
    { id: 'componentName',    label: 'Tên cấu phần',          isFixed: true,  width: 220 },
    { id: 'messageType',      label: 'Chuẩn tin điện',        isFixed: false, width: 160 },
    { id: 'connectionMethod', label: 'Tên kết nối',           isFixed: false, width: 160 },
    { id: 'checkToken',       label: 'Kiểm tra Token/ký số',  isFixed: false, width: 180 },
    { id: 'status',           label: 'Trạng thái tham số',    isFixed: false, width: 180 },
    { id: 'isActive',         label: 'Tình trạng hoạt động',  isFixed: false, width: 180 },
    { id: 'actions',          label: 'Thao tác',              isFixed: false, width: 250 }
  ];

  private readonly columnLabelMap = computed<Record<string, string>>(() => {
    const l = this.languageService.labels();
    return {
      stt: l.common.stt,  componentCode: l.components.code,  componentName: l.components.name,
      messageType: l.components.messageType,  connectionMethod: l.components.connectionMethod,
      checkToken: l.components.checkToken,  status: l.components.paramStatus,
      isActive: l.components.activeStatus,  actions: l.common.actions
    };
  });

  getColumnLabel(id: string): string { return this.columnLabelMap()[id] || id; }

  // ── History dialog ─────────────────────────────────────────────────────────
  historyTargetCode: string | null = null;

  openHistoryDialog(item: ProcessingComponentResponse): void {
    this.historyTargetCode = item.componentCode;
    this.historyTargetName = item.componentName || item.componentCode;
    this.isHistoryOpen     = true;
  }

  trackByCode(_: number, item: ProcessingComponentResponse): string { return item.componentCode; }
  trackByHistoryId(_: number, item: { id?: number }): number | undefined { return item.id; }

  // ── Export Job state ──────────────────────────────────────────────────────
  activeExportJob = signal<ExportJobResponse | null>(null);
  exportError = signal<string | null>(null);

  showConfirmDialog = signal(false);
  confirmDialogMsg = signal('');

  get isExporting(): boolean {
    const s = this.activeExportJob()?.status;
    return s === 'PENDING' || s === 'PROCESSING';
  }

  // ── Lifecycle ──────────────────────────────────────────────────────────────
  override ngOnInit(): void {
    this.loadFilterOptions();
    const saved = this.componentService.getListState();
    if (saved) {
      this.page.set(saved.page);
      this.size.set(saved.size);
      this.searchForm.patchValue(saved.filters);
    }
    this.loadData();
    this.restoreActiveJobState();
  }

  private restoreActiveJobState(): void {
    this.componentService.getActiveJob().pipe(
      catchError(() => EMPTY)
    ).subscribe(res => {
      const job = res.data;
      if (job && (job.status === 'PENDING' || job.status === 'PROCESSING')) {
        this.activeExportJob.set(job);
        this.pollExportUntilDone(job.jobId);
      }
    });
  }

  private loadFilterOptions(): void {
    this.componentService.search({}, 0, 1000).subscribe({
      next: (res) => {
        const list = res.data?.content || [];
        this.componentCodesList = list.map(c => ({ value: c.componentCode, label: c.componentCode }));
        this.componentNamesList = list.map(c => ({ value: c.componentName, label: c.componentName }));
      },
      error: () => { this.componentCodesList = []; this.componentNamesList = []; }
    });
  }

  // ── Data loading ───────────────────────────────────────────────────────────
  private buildComponentFilters() {
    const raw = this.searchForm.value;
    const placeholder = this.languageService.labels().common.selectValue;
    return {
      componentCode: Array.isArray(raw.componentCode) ? raw.componentCode.join(', ')
                   : (raw.componentCode && raw.componentCode !== placeholder ? String(raw.componentCode) : ''),
      componentName: Array.isArray(raw.componentName) ? raw.componentName.join(', ')
                   : (raw.componentName && raw.componentName !== placeholder ? String(raw.componentName) : ''),
      status:   this.toNumberArray(raw.status, placeholder),
      isActive: this.toNumberArray(raw.isActive, placeholder)
    };
  }

  override loadData(): void {
    this.componentService.setListState({
      page: this.page(), size: this.size(), filters: this.searchForm.value, viewMode: 'jpa', activeTabIndex: 0
    });
    this.isLoading.set(true);

    this.componentService.search(this.buildComponentFilters(), this.page(), this.size(),
      `${this.sortField()},${this.sortDirection()}`).subscribe({
      next: (res) => {
        this.components.set(res.data.content || []);
        this.totalElements.set(res.data.page?.totalElements ?? res.data.totalElements ?? 0);
        this.selectedCodes.set([]);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status !== 401 && this.authService.isLoggedIn()) {
          const prefix = this.languageService.labels().messages?.errorPrefix?.loadData || 'Lỗi tải dữ liệu: ';
          this.notificationService.error(prefix + (err.error?.message || err.message));
        }
        this.isLoading.set(false);
      }
    });
  }

  // ── Search / Reset ─────────────────────────────────────────────────────────
  override onSearch(): void { this.page.set(0); this.loadData(); }

  onReset(): void {
    this.searchForm.reset({ componentCode: [], componentName: [], status: [], isActive: [] });
    this.page.set(0); this.loadData();
  }

  // ── Navigation ─────────────────────────────────────────────────────────────
  openAddDialog():                                   void { this.router.navigate(['/components/add']); }
  openEditDialog(item: ProcessingComponentResponse): void { this.router.navigate(['/components/edit', item.componentCode]); }
  openCopyDialog(item: ProcessingComponentResponse): void { this.router.navigate(['/components/copy', item.componentCode], { state: { data: item } }); }
  onViewDetail(item: ProcessingComponentResponse):   void { this.router.navigate(['/components/detail', item.componentCode], { state: { data: item } }); }

  // ── Export (Async Job) ─────────────────────────────────────────────────────
  onClickExport(): void {
    if (this.searchForm.invalid) {
      this.notificationService.warning('Dữ liệu bộ lọc không hợp lệ. Vui lòng kiểm tra lại.');
      return;
    }

    const total = this.totalElements();
    if (total === 0) {
      this.notificationService.warning('Không có bản ghi nào phù hợp với bộ lọc hiện tại để xuất.');
      return;
    }

    if (this.isExporting) {
      this.notificationService.warning('Bạn đã có một tiến trình xuất file đang chạy. Vui lòng chờ hoàn thành.');
      return;
    }

    const raw = this.searchForm.value;
    const filters = this.buildComponentFilters();

    const hasFilter = !!(filters.componentCode || filters.componentName ||
      (Array.isArray(raw.status) && raw.status.length) ||
      (Array.isArray(raw.isActive) && raw.isActive.length));

    const formattedTotal = total.toLocaleString('vi-VN');
    if (hasFilter) {
      this.confirmDialogMsg.set(`Bạn có chắc chắn muốn xuất ${formattedTotal} bản ghi cấu phần theo bộ lọc hiện tại sang file XLSX?`);
    } else {
      this.confirmDialogMsg.set(`Bạn có chắc chắn muốn xuất tất cả ${formattedTotal} bản ghi cấu phần sang file XLSX?`);
    }
    this.showConfirmDialog.set(true);
  }

  onConfirmExport(): void {
    this.showConfirmDialog.set(false);
    this.exportError.set(null);
    const raw = this.searchForm.value;
    const filters = this.buildComponentFilters();

    this.componentService.createExportJob({
      componentCode: filters.componentCode || undefined,
      componentName: filters.componentName || undefined,
      status:   Array.isArray(raw.status)   && raw.status.length   ? raw.status   : undefined,
      isActive: Array.isArray(raw.isActive) && raw.isActive.length ? raw.isActive : undefined,
      sortBy: this.sortField(),
      sortDirection: this.sortDirection()
    }).subscribe({
      next: (res) => {
        this.activeExportJob.set(res.data);
        this.notificationService.info('Yêu cầu xuất file đã được tiếp nhận. Đang xử lý...');
        const jobId = res.data?.jobId;
        if (jobId) this.pollExportUntilDone(jobId);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 409) {
          this.notificationService.warning('Bạn đã có một tiến trình xuất file đang chạy. Vui lòng chờ hoàn thành.');
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

  onDownload(job: ExportJobResponse): void {
    this.componentService.getDownloadUrl(job.jobId).subscribe({
      next: (res) => {
        let url = res.data;
        if (url && url.startsWith('/')) {
          url = environment.apiBase + url;
        }
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', job.fileName || `component_export_${job.jobId}.xlsx`);
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

  /** Polling cho đến khi job DONE hoặc FAILED */
  private pollExportUntilDone(jobId: number): void {
    const interval = setInterval(() => {
      this.componentService.getActiveJob().subscribe({
        next: (res) => {
          const job = res.data;
          this.activeExportJob.set(job);
          if (!job || job.jobId !== jobId || job.status === 'DONE' || job.status === 'SUCCESS' || job.status === 'COMPLETED' || job.status === 'FAILED') {
            clearInterval(interval);
            if (!job || (job && (job.status === 'DONE' || job.status === 'SUCCESS' || job.status === 'COMPLETED'))) {
              this.notificationService.success('Xuất file hoàn tất!');
            } else if (job && job.status === 'FAILED') {
              this.notificationService.error('Xuất file thất bại: ' + (job.errorMessage || 'Lỗi không xác định'));
            }
          }
        },
        error: () => clearInterval(interval)
      });
    }, 3000);
  }
}
