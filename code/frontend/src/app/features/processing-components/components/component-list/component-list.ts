import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ComponentService } from '../../services/component.service';
import { HttpErrorResponse } from '@angular/common/http';
import { exportToCsv } from '../../../../shared/utils/csv-export.utils';
import { TableColumnDef } from '../../../../shared/utils/table-column.utils';
import { ProcessingComponentResponse } from '../../../../shared/models/component.model';
import { ApiResponse, BatchItemResult } from '../../../../shared/models/api-response.model';
import { BaseListComponent } from '../../../../shared/components/base-list/base-list.component';

import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { RejectReasonDialogComponent } from '../../../../shared/components/reject-reason-dialog/reject-reason-dialog';
import { AuditHistoryDialogComponent } from '../../../../shared/components/audit-history-dialog/audit-history-dialog';

@Component({
  selector: 'app-component-list',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, FormsModule, SharedTaigaModule,
    ConfirmDialogComponent, RejectReasonDialogComponent, AuditHistoryDialogComponent
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

  // ── Export ─────────────────────────────────────────────────────────────────
  onExportExcel(): void {
    this.componentService.exportExcel().subscribe({
      next: (res) => {
        const data = (res.data || []) as unknown as ProcessingComponentResponse[];
        if (data.length === 0) {
          this.notificationService.warning(this.languageService.labels().messages?.warning?.noComponentDataToExport || 'Không có dữ liệu cấu phần để xuất!');
          return;
        }
        exportToCsv(
          data,
          ['Mã cấu phần', 'Tên cấu phần', 'Chuẩn tin điện', 'Phương thức kết nối', 'Kiểm tra Token', 'Trạng thái duyệt', 'Hoạt động', 'Ngày hiệu lực'],
          (row) => [
            row.componentCode, row.componentName, row.messageType || '', row.connectionMethod || '',
            row.checkToken,
            row.status === this.ParamStatus.APPROVED ? 'Đã duyệt' : 'Chưa duyệt',
            row.isActive === this.ActiveStatus.ACTIVE ? 'Hoạt động' : 'Không hoạt động',
            row.effectiveDate
          ],
          'Cau_phan_xu_ly'
        );
      },
      error: (err: HttpErrorResponse) => {
        const prefix = this.languageService.labels().messages?.errorPrefix?.exportExcel || 'Lỗi xuất Excel: ';
        this.notificationService.error(prefix + (err.error?.message || err.message));
      }
    });
  }
}
