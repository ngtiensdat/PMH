import { Directive, inject, computed, signal, ChangeDetectorRef } from '@angular/core';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { LanguageService } from '../../../core/services/language.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../notification/notification.service';
import { STATUS_MAP, APPROVAL_STATUS_OPTIONS, IS_ACTIVE_OPTIONS, ACTION_PILL_MAP } from '../../constants/status.constants';
import { ParamStatus, ActiveStatus, DisplayStatus } from '../../enums/status.enum';
import { BatchItemResult } from '../../models/api-response.model';
import { ApiResponse } from '../../models/api-response.model';
import { SelectableRecord } from '../../utils/batch-action.utils';
import { isItemSelectable, isAllItemsSelected, toggleAllSelection } from '../../utils/batch-action.utils';
import { TableColumnDef, reorderTableColumns, computeNextSort } from '../../utils/table-column.utils';

export type ConfirmActionType =
  | 'approve' | 'reject' | 'delete'
  | 'sendApproval' | 'cancelApproval'
  | 'batchApprove' | 'batchReject';

/**
 * Abstract base class cho mọi list component theo pattern:
 * search → table → paginate → batch approve/reject → export.
 *
 * Generic params:
 *   T — Model type (phải có createdBy/updatedBy cho Maker-Checker)
 *   K — Primary key type: number (id) hoặc string (code)
 *
 * Module mới chỉ cần implement ~10 abstract members + viết phần domain:
 *   form, columns, loadData(), ngOnInit(), routes, export.
 */
@Directive()
export abstract class BaseListComponent<T extends SelectableRecord, K extends string | number> {

  // ── Injected services ──────────────────────────────────────────────────────
  protected cdr             = inject(ChangeDetectorRef);
  protected router          = inject(Router);
  protected notificationService = inject(NotificationService);
  public    languageService  = inject(LanguageService);
  public    authService      = inject(AuthService);

  // ── Enum refs ──────────────────────────────────────────────────────────────
  readonly ParamStatus   = ParamStatus;
  readonly ActiveStatus  = ActiveStatus;
  readonly DisplayStatus = DisplayStatus;

  // ── Status constants ───────────────────────────────────────────────────────
  readonly statusMap   = STATUS_MAP;
  readonly statusCodes = APPROVAL_STATUS_OPTIONS.map(o => String(o.value));
  readonly activeCodes = IS_ACTIVE_OPTIONS.map(o => String(o.value));
  get statusOptions()  { return APPROVAL_STATUS_OPTIONS; }
  get activeOptions()  { return IS_ACTIVE_OPTIONS; }

  // ── Reactive label maps — rebuilt only when language signal changes ─────────
  private readonly _statusLabelMap = computed<Record<string, string>>(() => {
    const s = this.languageService.labels().common.status;
    return { '1': s.new, '3': s.pending, '4': s.approved, '5': s.rejected, '7': s.canceled };
  });

  private readonly _activeLabelMap = computed<Record<string, string>>(() => {
    const l = this.languageService.labels().common;
    return { '1': l.active, '0': l.inactive };
  });

  readonly stringifyStatus = (val: string): string => {
    if (!val) return '';
    return this._statusLabelMap()[String(val)] || String(val);
  };

  readonly stringifyActive = (val: string): string => {
    if (val === null || val === undefined || val === '') return '';
    return this._activeLabelMap()[String(val)] || String(val);
  };

  // ── Pagination & sort ──────────────────────────────────────────────────────
  readonly page          = signal(0);
  readonly size          = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages    = computed(() => Math.ceil(this.totalElements() / this.size()) || 1);
  readonly sortField     = signal('updatedDate');
  readonly sortDirection = signal('desc');
  readonly isLoading     = signal(false);

  // ── Column state ───────────────────────────────────────────────────────────
  abstract columns: TableColumnDef[];
  draggedColumnIndex: number | null = null;
  dragOverColumnIndex: number | null = null;
  isResizing  = false;
  justResized = false;

  // ── Columns helpers ────────────────────────────────────────────────────────
  /** displayColumns với permission check — module chỉ cần set `permissionPrefix` */
  get displayColumns(): TableColumnDef[] {
    const canApprove = this.authService.hasPermission(`${this.permissionPrefix}_APPROVE`)
                    || this.authService.hasPermission(`${this.permissionPrefix}_REJECT`);
    return canApprove ? this.columns : this.columns.filter(c => c.id !== 'checkbox');
  }

  isLastColumn(colId: string): boolean {
    const cols = this.displayColumns;
    return cols.length > 0 && cols[cols.length - 1].id === colId;
  }

  trackByColId(_index: number, col: TableColumnDef): string { return col.id; }
  getActionPillClass(action: string): string { return ACTION_PILL_MAP[action] || 'pill-default'; }

  // ── Resize ─────────────────────────────────────────────────────────────────
  onResizeStart(event: MouseEvent, col: TableColumnDef): void {
    event.stopPropagation();
    event.preventDefault();
    this.isResizing = true;
    this.justResized = true;
    const startX = event.clientX;
    const startWidth = col.width;
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
    let rafId: number | null = null;

    const onMouseMove = (e: MouseEvent) => {
      const newWidth = Math.max(col.id === 'checkbox' ? 40 : 80, startWidth + e.clientX - startX);
      if (rafId) cancelAnimationFrame(rafId);
      rafId = requestAnimationFrame(() => { col.width = newWidth; this.cdr.markForCheck(); });
    };
    const onMouseUp = () => {
      if (rafId) cancelAnimationFrame(rafId);
      this.isResizing = false;
      setTimeout(() => { this.justResized = false; }, 150);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
      this.cdr.detectChanges();
    };
    document.addEventListener('mousemove', onMouseMove, { passive: true });
    document.addEventListener('mouseup', onMouseUp, { once: true });
  }

  // ── Drag & drop columns ────────────────────────────────────────────────────
  onDragStart(colId: string, event: DragEvent): void {
    const index = this.columns.findIndex(c => c.id === colId);
    if (this.isResizing || this.justResized || index === -1 || this.columns[index].isFixed) {
      event.preventDefault(); return;
    }
    this.draggedColumnIndex = index;
  }
  onDragOver(event: DragEvent, colId: string): void {
    const index = this.columns.findIndex(c => c.id === colId);
    if (index === -1 || this.columns[index].isFixed || this.draggedColumnIndex === null) return;
    event.preventDefault();
    this.dragOverColumnIndex = index;
  }
  onDragLeave(): void { this.dragOverColumnIndex = null; }
  onDrop(colId: string): void {
    this.columns = reorderTableColumns(this.columns, this.draggedColumnIndex, colId);
    this.draggedColumnIndex = null;
    this.dragOverColumnIndex = null;
  }

  // ── Sort ───────────────────────────────────────────────────────────────────
  toggleSort(colId: string): void {
    if (this.isResizing || this.justResized || colId === 'checkbox' || colId === 'stt' || colId === 'actions') return;
    const res = computeNextSort(this.sortField(), this.sortDirection(), colId);
    this.sortField.set(res.sortField);
    this.sortDirection.set(res.sortDirection);
    this.page.set(0);
    this.loadData();
  }

  // ── Pagination ─────────────────────────────────────────────────────────────
  setPage(p: number): void { this.page.set(p); this.loadData(); }
  onSearch(): void { this.page.set(0); this.loadData(); }

  // ── Confirm dialog state ───────────────────────────────────────────────────
  isConfirmOpen   = false;
  confirmTitle    = '';
  confirmMessage  = '';
  confirmAction: ConfirmActionType | null = null;
  confirmTargetKey: K | null = null;

  // ── Reject dialog state ────────────────────────────────────────────────────
  isRejectOpen     = false;
  rejectReason     = '';
  rejectTargetKeys: K[] = [];
  onRejectReasonInput(val: string): void { this.rejectReason = val; }

  // ── History dialog state ───────────────────────────────────────────────────
  isHistoryOpen     = false;
  historyTargetName = '';

  // ── Abstract: config & data ────────────────────────────────────────────────
  protected abstract readonly batchUnit: string;
  protected abstract readonly permissionPrefix: string;
  protected abstract getItemKey(item: T): K;
  protected abstract getListItems(): T[];

  // Selection bridge — subclass provides typed signal via these methods
  protected abstract getSelectedKeys(): K[];
  protected abstract setSelectedKeys(keys: K[]): void;
  protected abstract updateSelectedKeys(fn: (keys: K[]) => K[]): void;

  // Abstract: service calls (each ~1 line in subclass)
  protected abstract executeBatchApprove(keys: K[]): Observable<ApiResponse<BatchItemResult[]>>;
  protected abstract executeBatchReject(keys: K[], reason: string): Observable<ApiResponse<BatchItemResult[]>>;
  protected abstract executeDelete(key: K): Observable<ApiResponse<unknown>>;
  protected abstract executeSendApproval(key: K): Observable<ApiResponse<unknown>>;
  protected abstract executeCancelApproval(key: K): Observable<ApiResponse<unknown>>;

  // ── Selection — fully in base ──────────────────────────────────────────────
  isSelectable(item: T): boolean {
    return isItemSelectable(item, this.authService.currentUser()?.username || '');
  }

  toggleSelectAll(event: Event): void {
    const keys = toggleAllSelection(this.getListItems(), this.getItemKey.bind(this),
      this.authService.currentUser()?.username || '', event) as K[];
    this.setSelectedKeys(keys);
  }

  toggleItemSelection(item: T, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (!this.isSelectable(item)) {
      (event.target as HTMLInputElement).checked = false;
      return;
    }
    const key = this.getItemKey(item);
    if (checked) {
      this.updateSelectedKeys(keys => keys.includes(key) ? keys : [...keys, key]);
    } else {
      this.updateSelectedKeys(keys => keys.filter(k => k !== key));
    }
  }

  isAllSelected(): boolean {
    return isAllItemsSelected(this.getListItems(), this.getSelectedKeys() as (string | number)[],
      this.getItemKey.bind(this), this.authService.currentUser()?.username || '');
  }

  // ── Single-item dialog openers (uses batchUnit) ────────────────────────────
  onDelete(key: K): void {
    this.confirmTitle    = `Xóa ${this.batchUnit}`;
    this.confirmMessage  = `Bạn có chắc chắn muốn xóa ${this.batchUnit} này?`;
    this.confirmAction   = 'delete';
    this.confirmTargetKey = key;
    this.isConfirmOpen   = true;
  }

  onSendApproval(key: K): void {
    this.confirmTitle    = 'Gửi duyệt';
    this.confirmMessage  = `Bạn có chắc chắn muốn gửi duyệt ${this.batchUnit} này?`;
    this.confirmAction   = 'sendApproval';
    this.confirmTargetKey = key;
    this.isConfirmOpen   = true;
  }

  onCancelApproval(key: K): void {
    this.confirmTitle    = 'Hủy duyệt';
    this.confirmMessage  = `Bạn có chắc chắn muốn hủy duyệt ${this.batchUnit} này? Trạng thái sẽ chuyển về Hủy duyệt (7).`;
    this.confirmAction   = 'cancelApproval';
    this.confirmTargetKey = key;
    this.isConfirmOpen   = true;
  }

  // ── Batch validation ───────────────────────────────────────────────────────
  private validateForBatch(keys: K[], action: 'approve' | 'reject'): boolean {
    const actionText = action === 'approve' ? 'duyệt' : 'từ chối';
    if (keys.length === 0) {
      this.notificationService.warning(`Vui lòng chọn ít nhất một ${this.batchUnit} để ${actionText}!`);
      return false;
    }
    const items     = this.getListItems().filter(item => (keys as (string|number)[]).includes(this.getItemKey(item)));
    const unselectable = items.filter(item => !this.isSelectable(item));
    if (unselectable.length > 0 && unselectable.length === items.length) {
      const msg = this.languageService.labels().messages?.warning?.selfApproveDenied
               || `Bạn là người tạo/sửa các ${this.batchUnit} này nên không được phép tự ${actionText}!`;
      this.notificationService.error(msg);
      return false;
    }
    return true;
  }

  // ── Batch approve / reject ─────────────────────────────────────────────────
  onBatchApprove(): void {
    const keys = this.getSelectedKeys();
    if (!this.validateForBatch(keys, 'approve')) return;
    this.confirmTitle   = 'Phê duyệt';
    this.confirmMessage = keys.length === 1
      ? `Bạn có chắc chắn phê duyệt ${this.batchUnit} này?`
      : `Bạn có chắc chắn muốn duyệt hàng loạt ${keys.length} ${this.batchUnit} đã chọn?`;
    this.confirmAction  = 'batchApprove';
    this.isConfirmOpen  = true;
  }

  onBatchReject(): void {
    const keys = this.getSelectedKeys();
    if (!this.validateForBatch(keys, 'reject')) return;
    this.rejectTargetKeys = keys;
    this.rejectReason     = '';
    this.isRejectOpen     = true;
  }

  onConfirmReject(reasonInput?: string): void {
    if (reasonInput !== undefined) this.rejectReason = reasonInput;
    const keys   = this.rejectTargetKeys;
    const reason = this.rejectReason.trim();
    this.isRejectOpen = false;
    if (keys.length === 0) return;
    this.isLoading.set(true);

    this.executeBatchReject(keys, reason).subscribe({
      next: (res) => {
        this.handleBatchResult(res.data || [], keys.length, this.batchUnit, reason, 'reject');
        this.loadData();
      },
      error: (err: HttpErrorResponse) => {
        const prefix = this.languageService.labels().messages?.errorPrefix?.reject || 'Lỗi thực hiện từ chối duyệt: ';
        this.notificationService.error(prefix + (err.error?.message || err.message));
        this.loadData();
      }
    });
  }

  // ── Confirm execute — fully in base ───────────────────────────────────────
  onConfirmExecute(): void {
    const action = this.confirmAction;
    const key    = this.confirmTargetKey;
    this.isConfirmOpen = false;
    this.isLoading.set(true);

    if (action === 'delete' && key !== null) {
      this.executeDelete(key).subscribe({
        next: () => this.handleActionSuccess('delete', 'Xóa thành công!'),
        error: (err: HttpErrorResponse) => this.handleActionError(err, 'delete', 'Không thể xóa: ')
      });
    } else if (action === 'sendApproval' && key !== null) {
      this.executeSendApproval(key).subscribe({
        next: () => this.handleActionSuccess('sendApproval', 'Gửi duyệt thành công!'),
        error: (err: HttpErrorResponse) => this.handleActionError(err, 'sendApproval', 'Lỗi gửi duyệt: ')
      });
    } else if (action === 'cancelApproval' && key !== null) {
      this.executeCancelApproval(key).subscribe({
        next: () => this.handleActionSuccess('cancelApproval', 'Hủy duyệt thành công!'),
        error: (err: HttpErrorResponse) => this.handleActionError(err, 'cancelApproval', 'Lỗi hủy duyệt: ')
      });
    } else if (action === 'batchApprove') {
      const keys = this.getSelectedKeys();
      this.executeBatchApprove(keys).subscribe({
        next: (res) => {
          this.handleBatchResult(res.data || [], keys.length, this.batchUnit, '', 'approve');
          this.loadData();
        },
        error: (err: HttpErrorResponse) => this.handleActionError(err, 'batchApprove', 'Lỗi duyệt hàng loạt: ')
      });
    }
  }

  // ── Batch result ───────────────────────────────────────────────────────────
  protected handleBatchResult(results: BatchItemResult[], total: number, unit: string, reason: string, type: 'approve' | 'reject'): void {
    const successItems = results.filter(r => r.status === 'SUCCESS');
    const failedItems  = results.filter(r => r.status === 'FAILED');

    if (failedItems.length > 0 && successItems.length === 0) {
      const action = type === 'approve' ? 'phê duyệt' : 'từ chối';
      const firstErrMsg = failedItems[0].errorMessage || `Không thể ${action} do vi phạm quy tắc Maker-Checker!`;
      this.notificationService.error(`${type === 'approve' ? 'Phê duyệt' : 'Từ chối'} thất bại: ${firstErrMsg}`);
    } else if (failedItems.length > 0 && successItems.length > 0) {
      const firstErrMsg = failedItems[0].errorMessage || 'Vi phạm quy tắc Maker-Checker!';
      const action = type === 'approve' ? 'duyệt' : 'từ chối';
      this.notificationService.warning(`Đã ${action} thành công ${successItems.length}/${total} ${unit}. Bỏ qua ${failedItems.length} ${unit}: ${firstErrMsg}`);
    } else {
      const action = type === 'approve' ? 'duyệt' : 'từ chối';
      const suffix = type === 'reject' ? `! Lý do: ${reason}` : '!';
      this.notificationService.success(`Đã ${action} thành công ${successItems.length}/${total} ${unit}${suffix}`);
    }
  }

  // ── Action result handlers ─────────────────────────────────────────────────
  protected handleActionSuccess(successKey: string, fallback: string): void {
    const msgs = this.languageService.labels().messages;
    this.notificationService.success((msgs?.success as Record<string, string>)?.[successKey] || fallback);
    this.loadData();
  }

  protected handleActionError(err: HttpErrorResponse, errorPrefixKey: string, fallback: string): void {
    const msgs = this.languageService.labels().messages;
    this.notificationService.error(
      ((msgs?.errorPrefix as Record<string, string>)?.[errorPrefixKey] || fallback)
      + (err.error?.message || err.message)
    );
    this.loadData();
  }

  // ── Utility ────────────────────────────────────────────────────────────────
  protected toNumberArray(value: unknown, placeholder?: string): number[] {
    if (Array.isArray(value)) {
      return value.map((s: unknown) => Number(s)).filter((n: number) => !isNaN(n));
    } else if (value && value !== placeholder) {
      return [Number(value)];
    }
    return [];
  }

  // ── Abstract lifecycle & data ──────────────────────────────────────────────
  abstract ngOnInit(): void;
  abstract loadData(): void;
}
