import { Directive, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { NotificationService } from '../notification/notification.service';
import { LanguageService } from '../../../core/services/language.service';
import { AuthService } from '../../../core/services/auth.service';
import { ParamStatus, DisplayStatus, ActiveStatus } from '../../enums/status.enum';
import { ApiResponse } from '../../models/api-response.model';
import { formatDateTimeDisplay } from '../../utils/date.utils';

/** Constraint: entity phải có status, newData, isDisplay */
export type DetailEntity = { status: number; newData?: unknown; isDisplay?: number };

@Directive()
export abstract class BaseDetailComponent<T extends DetailEntity, K extends string | number>
  implements OnInit {

  // ── Injections ─────────────────────────────────────────────────────────────
  protected route               = inject(ActivatedRoute);
  protected router              = inject(Router);
  protected notificationService = inject(NotificationService);
  public    languageService     = inject(LanguageService);
  public    authService         = inject(AuthService);

  // ── Enum refs ──────────────────────────────────────────────────────────────
  readonly ParamStatus   = ParamStatus;
  readonly DisplayStatus = DisplayStatus;
  readonly ActiveStatus  = ActiveStatus;

  // ── State ──────────────────────────────────────────────────────────────────
  entity: T | null = null;
  isLoading = signal<boolean>(false);
  protected entityKeyStr: string | null = null;

  // ── Dialog state ───────────────────────────────────────────────────────────
  isDeleteOpen  = false;
  isApproveOpen = false;
  isRejectOpen  = false;
  rejectReason  = '';

  // ── Status label map (IDENTICAL across all modules) ───────────────────────
  readonly statusMap: Record<number, { label: string; css: string }> = {
    [ParamStatus.NEW]:      { label: 'Tạo mới',   css: 'badge-new' },
    [ParamStatus.PENDING]:  { label: 'Chờ duyệt', css: 'badge-pending' },
    [ParamStatus.APPROVED]: { label: 'Đã duyệt',  css: 'badge-approved' },
    [ParamStatus.REJECTED]: { label: 'Từ chối',   css: 'badge-rejected' },
    [ParamStatus.CANCELED]: { label: 'Hủy duyệt', css: 'badge-canceled' }
  };

  // ── Config (subclass cung cấp) ─────────────────────────────────────────────
  protected abstract readonly routeParamKey: string;
  protected abstract readonly listRoute: string;
  abstract readonly fields: string[];

  // ── Domain (subclass implement) ────────────────────────────────────────────
  protected abstract getEntityKey(): K;
  protected abstract loadEntityData(keyStr: string): void;
  abstract getFieldLabel(field: string): string;
  abstract formatValue(field: string, val: unknown): string;

  // ── Service calls (subclass wire) ──────────────────────────────────────────
  protected abstract callDelete(key: K): Observable<ApiResponse<unknown>>;
  protected abstract callSendApproval(key: K): Observable<ApiResponse<unknown>>;
  protected abstract callBatchApprove(keys: K[]): Observable<ApiResponse<unknown>>;
  protected abstract callBatchReject(keys: K[], reason: string): Observable<ApiResponse<unknown>>;

  // ── Constructor ────────────────────────────────────────────────────────────
  constructor() {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state?.['data']) {
      this.entity = navigation.extras.state['data'] as T;
    }
  }

  // ── Lifecycle ──────────────────────────────────────────────────────────────
  ngOnInit(): void {
    const param = this.route.snapshot.paramMap.get(this.routeParamKey);
    if (param) {
      this.entityKeyStr = param;
      this.loadEntityData(param);
    }
  }

  // ── Data getters (IDENTICAL across all modules) ───────────────────────────
  get parsedNewData(): Record<string, unknown> | null {
    if (!this.entity?.newData) return null;
    try {
      const nd = this.entity.newData;
      return typeof nd === 'string' ? JSON.parse(nd) : (nd as Record<string, unknown>);
    } catch { return null; }
  }

  get oldData(): Record<string, unknown> {
    if (!this.entity) return {};
    if (this.entity.status === ParamStatus.NEW || this.entity.isDisplay === DisplayStatus.INITIAL) return {};
    return this.entity as unknown as Record<string, unknown>;
  }

  get newData(): Record<string, unknown> {
    if (!this.entity) return {};
    return this.parsedNewData ?? (this.entity as unknown as Record<string, unknown>);
  }

  isFieldChanged(field: string): boolean {
    const oldVal = this.formatValue(field, this.oldData[field]);
    const newVal = this.formatValue(field, this.newData[field]);
    if (oldVal === '-' && newVal === '-') return false;
    return oldVal !== newVal;
  }

  get oldDataRows() {
    return this.fields.map(f => ({
      label:     this.getFieldLabel(f),
      value:     this.formatValue(f, this.oldData[f]),
      isChanged: this.isFieldChanged(f)
    }));
  }

  get newDataRows() {
    return this.fields.map(f => ({
      label:     this.getFieldLabel(f),
      value:     this.formatValue(f, this.newData[f]),
      isChanged: this.isFieldChanged(f)
    }));
  }

  // ── Dialog openers ─────────────────────────────────────────────────────────
  onDeleteRecord():                 void { this.isDeleteOpen  = true; }
  onApproveRecord():                void { this.isApproveOpen = true; }
  onRejectRecord():                 void { this.rejectReason = ''; this.isRejectOpen = true; }
  onRejectReasonInput(val: string): void { this.rejectReason = val; }

  // ── Actions ────────────────────────────────────────────────────────────────
  onConfirmDelete(): void {
    this.isDeleteOpen = false;
    const msgs = this.languageService.labels().messages;
    this.callDelete(this.getEntityKey()).subscribe({
      next: () => {
        const msg = this.entity?.isDisplay === DisplayStatus.ONCE_APPROVED
          ? (msgs?.success?.cancelEditSuccess || 'Hủy yêu cầu sửa thành công!')
          : (msgs?.success?.delete            || 'Xóa thành công!');
        this.notificationService.success(msg);
        this.goBack();
      },
      error: (err: HttpErrorResponse) =>
        this.notificationService.error((msgs?.errorPrefix?.executeFailed || 'Thực thi thất bại: ') + (err.error?.message || err.message))
    });
  }

  onSendApprovalRecord(): void {
    const msgs = this.languageService.labels().messages;
    this.callSendApproval(this.getEntityKey()).subscribe({
      next: () => {
        this.notificationService.success(msgs?.success?.sendApproval || 'Gửi duyệt thành công!');
        this.goBack();
      },
      error: (err: HttpErrorResponse) =>
        this.notificationService.error((msgs?.errorPrefix?.sendApproval || 'Lỗi gửi duyệt: ') + (err.error?.message || err.message))
    });
  }

  onConfirmApprove(): void {
    this.isApproveOpen = false;
    const msgs = this.languageService.labels().messages;
    this.callBatchApprove([this.getEntityKey()]).subscribe({
      next: () => {
        this.notificationService.success(msgs?.success?.approve || 'Duyệt thành công!');
        this.goBack();
      },
      error: (err: HttpErrorResponse) => {
        this.notificationService.error((msgs?.errorPrefix?.approveFailed || 'Lỗi khi duyệt: ') + (err.error?.message || err.message));
        if (this.entityKeyStr) this.loadEntityData(this.entityKeyStr);
      }
    });
  }

  onConfirmReject(): void {
    const reason = this.rejectReason.trim();
    this.isRejectOpen = false;
    const msgs = this.languageService.labels().messages;
    this.callBatchReject([this.getEntityKey()], reason).subscribe({
      next: () => {
        this.notificationService.success(`Đã từ chối duyệt thành công! Lý do: ${reason}`);
        this.goBack();
      },
      error: (err: HttpErrorResponse) => {
        this.notificationService.error((msgs?.errorPrefix?.rejectFailed || 'Lỗi khi từ chối: ') + (err.error?.message || err.message));
        if (this.entityKeyStr) this.loadEntityData(this.entityKeyStr);
      }
    });
  }

  goBack(): void {
    this.router.navigate([this.listRoute]);
  }
}
