import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ComponentService } from '../../services/component.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentSchema, zodFormValidator, zodFieldValidator } from '../../../../shared/validators/component.schema';
import { ProcessingComponentResponse, ProcessingComponentRequest } from '../../../../shared/models/component.model';
import { ApiResponse } from '../../../../shared/models/api-response.model';
import { BaseDialogComponent } from '../../../../shared/components/base-dialog/base-dialog.component';
import { formatToISO } from '../../../../shared/utils/date.utils';
import { tuiInputDateTimeOptionsProvider } from '@taiga-ui/kit';
import { DateTimeTransformer } from '../../../../shared/utils/datetime-transformer';
import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';

@Component({
  selector: 'app-component-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SharedTaigaModule],
  providers: [tuiInputDateTimeOptionsProvider({ valueTransformer: new DateTimeTransformer() })],
  templateUrl: './component-dialog.html',
  styleUrl: './component-dialog.css'
})
export class ComponentDialogComponent
  extends BaseDialogComponent<ProcessingComponentResponse, string, ProcessingComponentRequest>
  implements OnInit {

  // ── Config ─────────────────────────────────────────────────────────────────
  protected override readonly routeParamName = 'code';
  protected override readonly listRoute      = '/components';
  protected override readonly detailPath     = '/components/detail';

  // ── Service ────────────────────────────────────────────────────────────────
  private componentService = inject(ComponentService);

  // ── Domain state ───────────────────────────────────────────────────────────
  readonly messageTypeValues      = ['ISO20022', 'MT', 'MX', 'ISO8583', 'SWIFT', 'JSON', 'XML'];
  readonly connectionMethodValues = ['API', 'MQ', 'SFTP', 'TCP/IP', 'WebService', 'Batch'];

  get messageTypeItems():      string[] { return ['', ...this.messageTypeValues]; }
  get connectionMethodItems(): string[] { return ['', ...this.connectionMethodValues]; }

  readonly stringifyMessageType      = (val: string): string => val || 'Chọn giá trị';
  readonly stringifyConnectionMethod = (val: string): string => val || 'Chọn giá trị';

  // ── Service calls ──────────────────────────────────────────────────────────
  protected override callCreate(dto: ProcessingComponentRequest): Observable<ApiResponse<ProcessingComponentResponse>> {
    return this.componentService.create(dto);
  }
  protected override callUpdate(key: string, dto: ProcessingComponentRequest): Observable<ApiResponse<unknown>> {
    return this.componentService.update(key, dto);
  }
  protected override callSendApproval(key: string): Observable<ApiResponse<unknown>> {
    return this.componentService.sendApproval(key);
  }
  protected override getEntityKey(entity: ProcessingComponentResponse): string { return entity.componentCode; }

  protected override loadEntityData(keyParam: string): void {
    this.componentService.getByCode(keyParam)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => { this.entity = res.data; this.populateForm(this.entity); },
        error: (err: HttpErrorResponse) => {
          if (err.status !== 401 && err.status !== 403) {
            const prefix = this.languageService.labels().messages?.errorPrefix?.loadComponent || 'Không thể nạp dữ liệu cấu phần: ';
            this.notificationService.error(prefix + (err.error?.message || err.message));
          }
          this.goBack();
        }
      });
  }

  protected override resetListState(): void {
    const s = this.componentService.getListState();
    if (s) this.componentService.setListState({ ...s, page: 0 });
  }

  // ── Form ───────────────────────────────────────────────────────────────────
  override initForm(): void {
    this.dialogForm = this.fb.group(
      {
        componentCode:    ['', zodFieldValidator(ComponentSchema, 'componentCode')],
        componentName:    ['', zodFieldValidator(ComponentSchema, 'componentName')],
        messageType:      [[], zodFieldValidator(ComponentSchema, 'messageType')],
        connectionMethod: [[], zodFieldValidator(ComponentSchema, 'connectionMethod')],
        checkToken:       [false, zodFieldValidator(ComponentSchema, 'checkToken')],
        description:      ['', zodFieldValidator(ComponentSchema, 'description')],
        effectiveDate:    ['', zodFieldValidator(ComponentSchema, 'effectiveDate')],
        endEffectiveDate: ['', zodFieldValidator(ComponentSchema, 'endEffectiveDate')]
      },
      { validators: zodFormValidator(ComponentSchema) }
    );
  }

  override populateForm(entity: ProcessingComponentResponse): void {
    const draft = this.parsedNewData;
    const data  = draft ? { ...entity, ...draft } : entity;

    this.dialogForm.patchValue({
      componentCode:    data.componentCode,
      componentName:    data.componentName,
      messageType:      data.messageType
                          ? String(data.messageType).split(',').map((s: string) => s.trim()).filter(Boolean)
                          : [],
      connectionMethod: data.connectionMethod
                          ? String(data.connectionMethod).split(',').map((s: string) => s.trim()).filter(Boolean)
                          : [],
      checkToken:       data.checkToken === 'Y',
      description:      data.description || '',
      effectiveDate:    this.mode === 'copy' ? '' : data.effectiveDate,
      endEffectiveDate: this.mode === 'copy' ? '' : data.endEffectiveDate
    });

    if (this.mode === 'edit') {
      this.dialogForm.get('componentCode')?.disable();
    }
  }

  override normalizeRaw(raw: Record<string, unknown>): Record<string, unknown> {
    return {
      ...raw,
      checkToken:       raw['checkToken'] ? 'Y' : 'N',
      messageType:      Array.isArray(raw['messageType'])      ? (raw['messageType'] as string[]).join(', ')      : (raw['messageType'] || ''),
      connectionMethod: Array.isArray(raw['connectionMethod']) ? (raw['connectionMethod'] as string[]).join(', ') : (raw['connectionMethod'] || '')
    };
  }

  override validateNormalized(mapped: Record<string, unknown>): { success: boolean; error?: string } {
    const result = ComponentSchema.safeParse(mapped);
    if (result.success) return { success: true };
    return { success: false, error: result.error.issues[0]?.message };
  }

  override buildDto(mapped: Record<string, unknown>): ProcessingComponentRequest {
    return {
      ...(mapped as unknown as ProcessingComponentRequest),
      effectiveDate:    formatToISO(mapped['effectiveDate'] as string),
      endEffectiveDate: mapped['endEffectiveDate'] ? formatToISO(mapped['endEffectiveDate'] as string) : undefined
    };
  }

  override hasFormChanged(): boolean {
    if (!this.component) return true;
    const fv   = this.dialogForm.getRawValue();
    const base = this.component as unknown as Record<string, unknown>;

    for (const f of ['componentName', 'description']) {
      if ((base[f] != null ? String(base[f]).trim() : '') !== (fv[f] != null ? String(fv[f]).trim() : '')) return true;
    }

    const splitSort = (val: string) => String(val || '').split(',').map((s: string) => s.trim()).filter(Boolean).sort().join(', ');
    if (splitSort(this.component.messageType)      !== (fv.messageType      || []).map((s: string) => s.trim()).filter(Boolean).sort().join(', ')) return true;
    if (splitSort(this.component.connectionMethod) !== (fv.connectionMethod || []).map((s: string) => s.trim()).filter(Boolean).sort().join(', ')) return true;
    if ((this.component.checkToken === 'Y') !== !!fv.checkToken) return true;

    if (this.normalizeDate(this.component.effectiveDate)    !== this.normalizeDate(fv.effectiveDate))    return true;
    if (this.normalizeDate(this.component.endEffectiveDate) !== this.normalizeDate(fv.endEffectiveDate)) return true;

    return false;
  }

  // ── Convenience getter (template compatibility) ────────────────────────────
  get component(): ProcessingComponentResponse | null { return this.entity; }

  // ── Dialog title ───────────────────────────────────────────────────────────
  override get dialogTitle(): string {
    return this.mode === 'edit'
      ? 'Sửa tham số cấu phần xử lý'
      : 'Thêm mới tham số cấu phần xử lý';
  }
}
