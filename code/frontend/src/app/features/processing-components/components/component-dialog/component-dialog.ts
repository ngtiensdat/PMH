import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ComponentService } from '../../services/component.service';
import { NotificationService } from '../../../../shared/components/notification/notification.service';
import { ComponentSchema, zodFormValidator, zodFieldValidator } from '../../../../shared/validators/component.schema';
import { ParamStatus, ActiveStatus, DisplayStatus, FormMode } from '../../../../shared/enums/status.enum';
import { ProcessingComponentResponse, ProcessingComponentRequest } from '../../../../shared/models/component.model';
import { LanguageService } from '../../../../core/services/language.service';
import { HttpErrorResponse } from '@angular/common/http';
import { TUI_INPUT_DATE_TIME_OPTIONS, tuiInputDateTimeOptionsProvider } from '@taiga-ui/kit';
import { DateTimeTransformer } from '../../../../shared/utils/datetime-transformer';

import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';

@Component({
  selector: 'app-component-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SharedTaigaModule
  ],
  providers: [
    tuiInputDateTimeOptionsProvider({
      valueTransformer: new DateTimeTransformer()
    })
  ],
  templateUrl: './component-dialog.html',
  styleUrl: './component-dialog.css'
})
export class ComponentDialogComponent implements OnInit {
  readonly ParamStatus = ParamStatus;
  readonly ActiveStatus = ActiveStatus;
  readonly DisplayStatus = DisplayStatus;
  readonly FormMode = FormMode;

  private fb = inject(FormBuilder);
  private componentService = inject(ComponentService);
  private notificationService = inject(NotificationService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  public languageService = inject(LanguageService);

  mode: FormMode | 'add' | 'edit' | 'copy' = FormMode.ADD;
  component: ProcessingComponentResponse | null = null;
  code: string | null = null;

  constructor() {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state?.['data']) {
      this.component = navigation.extras.state['data'];
    }
  }

  dialogForm!: FormGroup;

  readonly messageTypeValues = ['ISO20022', 'MT', 'MX', 'ISO8583', 'SWIFT', 'JSON', 'XML'];
  readonly connectionMethodValues = ['API', 'MQ', 'SFTP', 'TCP/IP', 'WebService', 'Batch'];

  activeCodes = [1, 0];
  readonly stringifyActive = (val: number): string => {
    if (val === 1) return this.languageService.labels().common.active;
    if (val === 0) return this.languageService.labels().common.inactive;
    return 'Chọn giá trị';
  };

  get messageTypeItems(): string[] {
    return ['', ...this.messageTypeValues];
  }

  get connectionMethodItems(): string[] {
    return ['', ...this.connectionMethodValues];
  }

  readonly stringifyMessageType = (val: string): string => {
    if (!val) return 'Chọn giá trị';
    return val;
  };

  readonly stringifyConnectionMethod = (val: string): string => {
    if (!val) return 'Chọn giá trị';
    return val;
  };

  ngOnInit() {
    this.initForm();
    this.detectRouteAndMode();
  }

  private detectRouteAndMode() {
    const url = this.router.url;
    if (url.includes('/add')) {
      this.mode = 'add';
    } else if (url.includes('/edit')) {
      this.mode = 'edit';
    } else if (url.includes('/copy')) {
      this.mode = 'copy';
    }

    const codeParam = this.route.snapshot.paramMap.get('code');
    if (codeParam) {
      this.code = codeParam;
      this.loadComponentData(this.code);
    }
  }

  private loadComponentData(code: string) {
    this.componentService.getByCode(code)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.component = res.data;
          this.populateForm();
        },
        error: (err: HttpErrorResponse) => {
          if (err.status !== 401 && err.status !== 403) {
            const prefix = this.languageService.labels().messages?.errorPrefix?.loadComponent || 'Không thể nạp dữ liệu cấu phần: ';
            this.notificationService.error(prefix + (err.error?.message || err.message));
          }
          this.goBack();
        }
      });
  }

  private initForm() {
    this.dialogForm = this.fb.group(
      {
        componentCode: ['', zodFieldValidator(ComponentSchema, 'componentCode')],
        componentName: ['', zodFieldValidator(ComponentSchema, 'componentName')],
        messageType: [[], zodFieldValidator(ComponentSchema, 'messageType')],
        connectionMethod: [[], zodFieldValidator(ComponentSchema, 'connectionMethod')],
        checkToken: [false, zodFieldValidator(ComponentSchema, 'checkToken')],
        description: ['', zodFieldValidator(ComponentSchema, 'description')],
        effectiveDate: ['', zodFieldValidator(ComponentSchema, 'effectiveDate')],
        endEffectiveDate: ['', zodFieldValidator(ComponentSchema, 'endEffectiveDate')]
      },
      { validators: zodFormValidator(ComponentSchema) }
    );
  }

  get parsedNewData(): Record<string, any> | null {
    if (!this.component?.newData) return null;
    try {
      return typeof this.component.newData === 'string' ? JSON.parse(this.component.newData) : this.component.newData;
    } catch { return null; }
  }

  private populateForm() {
    if (!this.component) return;
    const draft = this.parsedNewData;
    const dataToPopulate = draft ? { ...this.component, ...draft } : this.component;

    this.dialogForm.patchValue({
      componentCode: dataToPopulate.componentCode,
      componentName: dataToPopulate.componentName,
      messageType: dataToPopulate.messageType ? String(dataToPopulate.messageType).split(',').map((s: string) => s.trim()).filter(Boolean) : [],
      connectionMethod: dataToPopulate.connectionMethod ? String(dataToPopulate.connectionMethod).split(',').map((s: string) => s.trim()).filter(Boolean) : [],
      checkToken: dataToPopulate.checkToken === 'Y',
      description: dataToPopulate.description || '',
      effectiveDate: this.mode === 'copy' ? '' : dataToPopulate.effectiveDate,
      endEffectiveDate: this.mode === 'copy' ? '' : dataToPopulate.endEffectiveDate
    });

    if (this.mode === 'edit') {
      this.dialogForm.get('componentCode')?.disable();
    }
  }

  hasFormChanged(): boolean {
    if (!this.component) return true;
    const formValue = this.dialogForm.getRawValue();
    const base = this.component;

    const normalizeDate = (val: any) => {
      if (!val) return '';
      const d = new Date(val);
      if (isNaN(d.getTime())) return '';
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${y}-${m}-${day}`;
    };

    const fields = ['componentName', 'description'];
    for (const f of fields) {
      const orig = (base as any)[f] != null ? String((base as any)[f]).trim() : '';
      const curr = formValue[f] != null ? String(formValue[f]).trim() : '';
      if (orig !== curr) return true;
    }

    const origMessageType = String(base.messageType || '').split(',').map((s: string) => s.trim()).filter(Boolean).sort().join(', ');
    const currMessageType = (formValue.messageType || []).map((s: string) => s.trim()).filter(Boolean).sort().join(', ');
    if (origMessageType !== currMessageType) return true;

    const origConnectionMethod = String(base.connectionMethod || '').split(',').map((s: string) => s.trim()).filter(Boolean).sort().join(', ');
    const currConnectionMethod = (formValue.connectionMethod || []).map((s: string) => s.trim()).filter(Boolean).sort().join(', ');
    if (origConnectionMethod !== currConnectionMethod) return true;

    const origCheck = base.checkToken === 'Y';
    const currCheck = !!formValue.checkToken;
    if (origCheck !== currCheck) return true;

    if (normalizeDate(base.effectiveDate) !== normalizeDate(formValue.effectiveDate)) return true;
    if (normalizeDate(base.endEffectiveDate) !== normalizeDate(formValue.endEffectiveDate)) return true;

    return false;
  }

  onSubmit(sendForApproval = false) {
    const raw = this.dialogForm.getRawValue();
    const mappedRaw = {
      ...raw,
      checkToken: raw.checkToken ? 'Y' : 'N',
      messageType: Array.isArray(raw.messageType) ? raw.messageType.join(', ') : (raw.messageType || ''),
      connectionMethod: Array.isArray(raw.connectionMethod) ? raw.connectionMethod.join(', ') : (raw.connectionMethod || '')
    };
    const parseResult = ComponentSchema.safeParse(mappedRaw);

    if (!parseResult.success) {
      this.markFormGroupTouched(this.dialogForm);
      const issues: any[] = (parseResult.error as any).issues ?? (parseResult.error as any).errors ?? [];
      const firstError = issues[0];
      const defaultMsg = this.languageService.labels().messages?.errorPrefix?.invalidInput || 'Dữ liệu nhập không hợp lệ';
      this.notificationService.error(firstError?.message ?? defaultMsg);
      return;
    }

    if (this.mode === 'edit' && !this.hasFormChanged()) {
      const warnMsg = this.languageService.labels().messages?.warning?.noFormChange || 'Không có thay đổi nào so với dữ liệu gốc! Không cần gửi duyệt sửa.';
      this.notificationService.warning(warnMsg);
      return;
    }

    const dto: ProcessingComponentRequest = {
      ...mappedRaw,
      effectiveDate: this.formatToISO(mappedRaw.effectiveDate),
      endEffectiveDate: mappedRaw.endEffectiveDate ? this.formatToISO(mappedRaw.endEffectiveDate) : undefined
    };

    const msgs = this.languageService.labels().messages;

    if (this.mode === 'edit' && this.component) {
      this.componentService.update(this.component.componentCode, dto)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            if (sendForApproval && this.component) {
              this.componentService.sendApproval(this.component.componentCode)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                  next: () => {
                    this.notificationService.success(msgs?.success?.title || 'Thành công!', msgs?.success?.updateAndSendApproval || 'Cập nhật và Gửi duyệt thành công', '/components/detail/' + this.component!.componentCode);
                    this.goBack();
                  },
                  error: (err: any) => {
                    this.notificationService.error((msgs?.errorPrefix?.sendApproval || 'Lỗi gửi duyệt: ') + (err.error?.message || err.message));
                    this.goBack();
                  }
                });
            } else {
              this.notificationService.success(msgs?.success?.title || 'Thành công!', msgs?.success?.update || 'Cập nhật thành công', '/components/detail/' + this.component!.componentCode);
              this.goBack();
            }
          },
          error: (err: any) => {
            this.notificationService.error((msgs?.errorPrefix?.update || 'Lỗi cập nhật: ') + (err.error?.message || err.message));
          }
        });
    } else {
      this.componentService.create(dto)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (res) => {
            const compCode = res.data.componentCode || dto.componentCode;
            if (sendForApproval && compCode) {
              this.componentService.sendApproval(compCode)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                  next: () => {
                    this.notificationService.success(msgs?.success?.title || 'Thành công!', msgs?.success?.createAndSendApproval || 'Thêm mới và Gửi duyệt thành công', '/components/detail/' + compCode);
                    this.goBack();
                  },
                  error: (err: any) => {
                    this.notificationService.error((msgs?.errorPrefix?.saveAndSendFailed || 'Đã lưu bản ghi, nhưng lỗi gửi duyệt: ') + (err.error?.message || err.message));
                    this.goBack();
                  }
                });
            } else {
              this.notificationService.success(msgs?.success?.title || 'Thành công!', msgs?.success?.create || 'Thêm mới thành công', '/components/detail/' + compCode);
              this.goBack();
            }
          },
          error: (err: any) => {
            this.notificationService.error((msgs?.errorPrefix?.create || 'Lỗi thêm mới: ') + (err.error?.message || err.message));
          }
        });
    }
  }

  private formatToISO(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  private markFormGroupTouched(fg: FormGroup) {
    Object.values(fg.controls).forEach(ctrl => {
      ctrl.markAsTouched();
      if ((ctrl as any).controls) this.markFormGroupTouched(ctrl as FormGroup);
    });
  }

  goBack() {
    const currentState = this.componentService.getListState();
    if (currentState) {
      this.componentService.setListState({
        ...currentState,
        page: 0
      });
    }
    this.router.navigate(['/components']);
  }

  get dialogTitle(): string {
    switch (this.mode) {
      case FormMode.ADD:
      case FormMode.COPY: return 'Thêm mới tham số cấu phần xử lý';
      case FormMode.EDIT: return 'Sửa tham số cấu phần xử lý';
      default: return 'Chi tiết tham số cấu phần xử lý';
    }
  }

  getBreadcrumbText(): string {
    switch (this.mode) {
      case FormMode.ADD:
      case FormMode.COPY: return 'Thêm mới';
      case FormMode.EDIT: return 'Sửa';
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
}
