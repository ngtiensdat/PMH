import { Directive, inject, DestroyRef, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { NotificationService } from '../notification/notification.service';
import { LanguageService } from '../../../core/services/language.service';
import { AuthService } from '../../../core/services/auth.service';
import { ApiResponse } from '../../models/api-response.model';
import { ParamStatus, ActiveStatus, DisplayStatus, FormMode } from '../../enums/status.enum';

/**
 * Abstract base class cho mọi dialog component (add/edit/copy).
 *
 * Generic params:
 *   T   — Model response type (phải có newData? cho draft merge)
 *   K   — Primary key type: number (id) hoặc string (code)
 *   Req — Request DTO type
 *
 * Module mới chỉ cần implement ~12 abstract members (mỗi cái 1-5 dòng) + viết phần
 * domain: initForm, populateForm, normalizeRaw, validateNormalized, buildDto, hasFormChanged.
 */
@Directive()
export abstract class BaseDialogComponent<
  T extends { newData?: unknown },
  K extends string | number,
  Req
> implements OnInit {

  // ── Injected services ──────────────────────────────────────────────────────
  protected fb                  = inject(FormBuilder);
  protected notificationService = inject(NotificationService);
  protected route               = inject(ActivatedRoute);
  protected router              = inject(Router);
  protected destroyRef          = inject(DestroyRef);
  public    languageService     = inject(LanguageService);
  public    authService         = inject(AuthService);

  // ── Enum refs ──────────────────────────────────────────────────────────────
  readonly ParamStatus   = ParamStatus;
  readonly ActiveStatus  = ActiveStatus;
  readonly DisplayStatus = DisplayStatus;
  readonly FormMode      = FormMode;

  // ── Shared state ───────────────────────────────────────────────────────────
  mode: FormMode | 'add' | 'edit' | 'copy' = FormMode.ADD;
  entity: T | null = null;
  dialogForm!: FormGroup;

  activeCodes = [1, 0];
  readonly stringifyActive = (val: number): string => {
    if (val === 1) return this.languageService.labels().common.active;
    if (val === 0) return this.languageService.labels().common.inactive;
    return 'Chọn giá trị';
  };

  // ── Constructor: capture navigation state BEFORE router clears it ──────────
  constructor() {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state?.['data']) {
      this.entity = navigation.extras.state['data'] as T;
    }
  }

  // ── parsedNewData — shared draft-merge logic ───────────────────────────────
  get parsedNewData(): Record<string, unknown> | null {
    if (!this.entity?.newData) return null;
    try {
      const nd = this.entity.newData;
      return typeof nd === 'string' ? JSON.parse(nd) : (nd as Record<string, unknown>);
    } catch { return null; }
  }

  // ── Abstract: config (1 line each) ────────────────────────────────────────
  protected abstract readonly routeParamName: string;  // 'id' | 'code'
  protected abstract readonly listRoute: string;        // '/categories'
  protected abstract readonly detailPath: string;       // '/categories/detail'
  abstract get dialogTitle(): string;

  // ── Abstract: form (domain-specific) ──────────────────────────────────────
  abstract initForm(): void;
  abstract populateForm(entity: T): void;

  /**
   * Pre-map raw form values trước khi validate schema.
   * Ví dụ: join array → string, convert boolean → 'Y'/'N'.
   */
  abstract normalizeRaw(raw: Record<string, unknown>): Record<string, unknown>;

  /**
   * Validate giá trị đã normalize bằng schema (Zod, v.v.)
   * Trả { success: true } hoặc { success: false, error: '...' }
   */
  abstract validateNormalized(mapped: Record<string, unknown>): { success: boolean; error?: string };

  /** Build final request DTO từ giá trị đã normalize (thêm formatToISO, v.v.) */
  abstract buildDto(mapped: Record<string, unknown>): Req;

  /** Trả true nếu form values khác với entity data gốc */
  abstract hasFormChanged(): boolean;

  // ── Abstract: service calls (1 line each) ─────────────────────────────────
  protected abstract callCreate(dto: Req): Observable<ApiResponse<T>>;
  protected abstract callUpdate(key: K, dto: Req): Observable<ApiResponse<unknown>>;
  protected abstract callSendApproval(key: K): Observable<ApiResponse<unknown>>;

  /** Lấy primary key từ entity (entity.id hoặc entity.componentCode) */
  protected abstract getEntityKey(entity: T): K;

  /** Gọi API lấy entity theo route param, gán this.entity, gọi populateForm. */
  protected abstract loadEntityData(keyParam: string): void;

  /** Reset list state page về 0 trước khi navigate về list */
  protected abstract resetListState(): void;

  // ── Lifecycle ──────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.initForm();
    this.detectRouteAndMode();
  }

  private detectRouteAndMode(): void {
    const url = this.router.url;
    if (url.includes('/add'))       this.mode = 'add';
    else if (url.includes('/edit')) this.mode = 'edit';
    else if (url.includes('/copy')) this.mode = 'copy';

    const param = this.route.snapshot.paramMap.get(this.routeParamName);
    if (param) this.loadEntityData(param);
  }

  // ── Submit — template method ───────────────────────────────────────────────
  onSubmit(sendForApproval = false): void {
    const raw    = this.dialogForm.getRawValue() as Record<string, unknown>;
    const mapped = this.normalizeRaw(raw);
    const result = this.validateNormalized(mapped);

    if (!result.success) {
      this.markFormGroupTouched(this.dialogForm);
      const defaultMsg = this.languageService.labels().messages?.errorPrefix?.invalidInput || 'Dữ liệu nhập không hợp lệ';
      this.notificationService.error(result.error ?? defaultMsg);
      return;
    }

    if (this.mode === 'edit' && !this.hasFormChanged()) {
      const warnMsg = this.languageService.labels().messages?.warning?.noFormChange
                   || 'Không có thay đổi nào so với dữ liệu gốc! Không cần gửi duyệt sửa.';
      this.notificationService.warning(warnMsg);
      return;
    }

    const dto = this.buildDto(mapped);
    if (this.mode === 'edit' && this.entity) {
      this.executeUpdate(dto, sendForApproval);
    } else {
      this.executeCreate(dto, sendForApproval);
    }
  }

  // ── Execute (fully in base — dùng abstract service calls) ─────────────────
  protected executeUpdate(dto: Req, sendForApproval: boolean): void {
    const msgs = this.languageService.labels().messages;
    const key  = this.getEntityKey(this.entity!);

    this.callUpdate(key, dto)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          if (sendForApproval) {
            this.callSendApproval(key)
              .pipe(takeUntilDestroyed(this.destroyRef))
              .subscribe({
                next: () => {
                  this.notificationService.success(
                    msgs?.success?.title || 'Thành công!',
                    msgs?.success?.updateAndSendApproval || 'Cập nhật và Gửi duyệt thành công',
                    `${this.detailPath}/${key}`
                  );
                  this.goBack();
                },
                error: (err: HttpErrorResponse) => {
                  this.notificationService.error((msgs?.errorPrefix?.sendApproval || 'Lỗi gửi duyệt: ') + (err.error?.message || err.message));
                  this.goBack();
                }
              });
          } else {
            this.notificationService.success(
              msgs?.success?.title || 'Thành công!',
              msgs?.success?.update || 'Cập nhật thành công',
              `${this.detailPath}/${key}`
            );
            this.goBack();
          }
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.error((msgs?.errorPrefix?.update || 'Lỗi cập nhật: ') + (err.error?.message || err.message));
        }
      });
  }

  protected executeCreate(dto: Req, sendForApproval: boolean): void {
    const msgs = this.languageService.labels().messages;

    this.callCreate(dto)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          const key = this.getEntityKey(res.data);
          if (sendForApproval && key) {
            this.callSendApproval(key)
              .pipe(takeUntilDestroyed(this.destroyRef))
              .subscribe({
                next: () => {
                  this.notificationService.success(
                    msgs?.success?.title || 'Thành công!',
                    msgs?.success?.createAndSendApproval || 'Thêm mới và Gửi duyệt thành công',
                    `${this.detailPath}/${key}`
                  );
                  this.goBack();
                },
                error: (err: HttpErrorResponse) => {
                  this.notificationService.error((msgs?.errorPrefix?.saveAndSendFailed || 'Đã lưu bản ghi, nhưng lỗi gửi duyệt: ') + (err.error?.message || err.message));
                  this.goBack();
                }
              });
          } else {
            this.notificationService.success(
              msgs?.success?.title || 'Thành công!',
              msgs?.success?.create || 'Thêm mới thành công',
              `${this.detailPath}/${key}`
            );
            this.goBack();
          }
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.error((msgs?.errorPrefix?.create || 'Lỗi thêm mới: ') + (err.error?.message || err.message));
        }
      });
  }

  // ── Navigation ─────────────────────────────────────────────────────────────
  goBack(): void {
    this.resetListState();
    this.router.navigate([this.listRoute]);
  }

  // ── UI helpers ─────────────────────────────────────────────────────────────
  getBreadcrumbText(): string {
    switch (this.mode) {
      case FormMode.ADD:  case 'add':
      case FormMode.COPY: case 'copy': return 'Thêm mới';
      case FormMode.EDIT: case 'edit': return 'Sửa';
      default: return 'Chi tiết';
    }
  }

  getFieldError(field: string): string {
    const control = this.dialogForm.get(field);
    if (control && control.touched && control.invalid) {
      if (control.hasError('zodError')) return control.getError('zodError');
      if (this.dialogForm.hasError(field)) return this.dialogForm.getError(field);
    }
    return '';
  }

  getFormError(key: string): string {
    return this.dialogForm.hasError(key) ? this.dialogForm.getError(key) : '';
  }

  protected normalizeDate(val: unknown): string {
    if (!val) return '';
    const d = new Date(val as string | number | Date);
    if (isNaN(d.getTime())) return '';
    const y   = d.getFullYear();
    const m   = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  protected markFormGroupTouched(fg: FormGroup): void {
    Object.values(fg.controls).forEach(ctrl => {
      ctrl.markAsTouched();
      if (ctrl instanceof FormGroup) this.markFormGroupTouched(ctrl);
    });
  }
}
