import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ComponentService } from '../../services/component.service';
import { NotificationService } from '../../../../shared/components/notification/notification.service';
import { ComponentSchema, zodFormValidator, zodFieldValidator } from '../../../../shared/validators/component.schema';
import { ProcessingComponentResponse, ProcessingComponentRequest } from '../../../../shared/models/component.model';
import { LanguageService } from '../../../../core/services/language.service';

import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';

@Component({
  selector: 'app-component-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SharedTaigaModule
  ],
  templateUrl: './component-dialog.html',
  styleUrl: './component-dialog.css'
})
export class ComponentDialogComponent implements OnInit {
  private fb         = inject(FormBuilder);
  private componentService    = inject(ComponentService);
  private notificationService = inject(NotificationService);
  private route      = inject(ActivatedRoute);
  private router     = inject(Router);
  private destroyRef = inject(DestroyRef);
  public languageService = inject(LanguageService);

  mode: 'add' | 'edit' | 'copy' = 'add';
  component: ProcessingComponentResponse | null = null;
  code: string | null = null;

  dialogForm!: FormGroup;

  readonly messageTypeValues = ['ISO20022', 'MT', 'MX', 'ISO8583', 'SWIFT', 'JSON', 'XML'];
  readonly connectionMethodValues = ['API', 'MQ', 'SFTP', 'TCP/IP', 'WebService', 'Batch'];

  // Dropdown items
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
        error: (err: any) => {
          this.notificationService.error('Không thể nạp dữ liệu cấu phần: ' + (err.error?.message || err.message));
          this.goBack();
        }
      });
  }

  private initForm() {
    this.dialogForm = this.fb.group(
      {
        componentCode:    ['', zodFieldValidator(ComponentSchema, 'componentCode')],
        componentName:    ['', zodFieldValidator(ComponentSchema, 'componentName')],
        messageType:      ['', zodFieldValidator(ComponentSchema, 'messageType')],
        connectionMethod: ['', zodFieldValidator(ComponentSchema, 'connectionMethod')],
        checkToken:       ['N', zodFieldValidator(ComponentSchema, 'checkToken')],
        description:      ['', zodFieldValidator(ComponentSchema, 'description')],
        isActive:         [1,  zodFieldValidator(ComponentSchema, 'isActive')],
        effectiveDate:    ['', zodFieldValidator(ComponentSchema, 'effectiveDate')],
        endEffectiveDate: ['', zodFieldValidator(ComponentSchema, 'endEffectiveDate')]
      },
      { validators: zodFormValidator(ComponentSchema) }
    );
  }

  private populateForm() {
    const toLocalISO = (dateStr?: string | null) => {
      if (!dateStr) return '';
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return '';
      const pad = (n: number) => n.toString().padStart(2, '0');
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    };

    if (!this.component) return;
    this.dialogForm.patchValue({
      componentCode:    this.component.componentCode,
      componentName:    this.component.componentName,
      messageType:      this.component.messageType      || '',
      connectionMethod: this.component.connectionMethod || '',
      checkToken:       this.component.checkToken       === 'Y',
      description:      this.component.description      || '',
      isActive:         this.component.isActive          ?? 1,
      effectiveDate:    this.mode === 'copy' ? '' : toLocalISO(this.component.effectiveDate),
      endEffectiveDate: this.mode === 'copy' ? '' : toLocalISO(this.component.endEffectiveDate)
    });

    if (this.mode === 'edit') {
      this.dialogForm.get('componentCode')?.disable();
    }
  }

  hasFormChanged(): boolean {
    if (!this.component) return true;
    const formValue = this.dialogForm.getRawValue();

    const normalizeDate = (val: any) => {
      if (!val) return '';
      const d = new Date(val);
      return isNaN(d.getTime()) ? '' : d.toISOString();
    };

    const fields = ['componentName', 'messageType', 'connectionMethod', 'description', 'isActive'];
    for (const f of fields) {
      const orig = (this.component as any)[f] != null ? String((this.component as any)[f]).trim() : '';
      const curr = formValue[f]      != null ? String(formValue[f]).trim()      : '';
      if (orig !== curr) return true;
    }

    const origCheck = this.component.checkToken === 'Y';
    const currCheck = !!formValue.checkToken;
    if (origCheck !== currCheck) return true;

    if (normalizeDate(this.component.effectiveDate) !== normalizeDate(formValue.effectiveDate)) return true;
    if (normalizeDate(this.component.endEffectiveDate) !== normalizeDate(formValue.endEffectiveDate)) return true;

    return false;
  }

  onSubmit(sendForApproval = false) {
    const raw = this.dialogForm.getRawValue();
    const mappedRaw = {
      ...raw,
      checkToken: raw.checkToken ? 'Y' : 'N'
    };
    const parseResult = ComponentSchema.safeParse(mappedRaw);

    if (!parseResult.success) {
      this.markFormGroupTouched(this.dialogForm);
      const issues: any[] = (parseResult.error as any).issues ?? (parseResult.error as any).errors ?? [];
      const firstError = issues[0];
      this.notificationService.error(firstError?.message ?? 'Dữ liệu nhập không hợp lệ');
      return;
    }

    if (this.mode === 'edit' && !this.hasFormChanged()) {
      this.notificationService.warning('Không có thay đổi nào so với dữ liệu gốc! Không cần gửi duyệt sửa.');
      return;
    }

    const dto: ProcessingComponentRequest = {
      ...mappedRaw,
      effectiveDate:    this.formatToISO(mappedRaw.effectiveDate),
      endEffectiveDate: mappedRaw.endEffectiveDate ? this.formatToISO(mappedRaw.endEffectiveDate) : undefined
    };

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
                    this.notificationService.success('Thành công!', 'Cập nhật và Gửi duyệt thành công', '/components/detail/' + this.component!.componentCode);
                    this.goBack();
                  },
                  error: (err: any) => {
                    this.notificationService.error('Lỗi gửi duyệt: ' + (err.error?.message || err.message));
                    this.goBack();
                  }
                });
            } else {
              this.notificationService.success('Thành công!', 'Cập nhật thành công', '/components/detail/' + this.component!.componentCode);
              this.goBack();
            }
          },
          error: (err: any) => {
            this.notificationService.error('Lỗi cập nhật: ' + (err.error?.message || err.message));
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
                    this.notificationService.success('Thành công!', 'Thêm mới và Gửi duyệt thành công', '/components/detail/' + compCode);
                    this.goBack();
                  },
                  error: (err: any) => {
                    this.notificationService.error('Đã lưu bản ghi, nhưng lỗi gửi duyệt: ' + (err.error?.message || err.message));
                    this.goBack();
                  }
                });
            } else {
              this.notificationService.success('Thành công!', 'Thêm mới thành công', '/components/detail/' + compCode);
              this.goBack();
            }
          },
          error: (err: any) => {
            this.notificationService.error('Lỗi thêm mới: ' + (err.error?.message || err.message));
          }
        });
    }
  }

  private formatToISO(dateStr: string): string {
    return new Date(dateStr).toISOString();
  }

  private markFormGroupTouched(fg: FormGroup) {
    Object.values(fg.controls).forEach(ctrl => {
      ctrl.markAsTouched();
      if ((ctrl as any).controls) this.markFormGroupTouched(ctrl as FormGroup);
    });
  }

  goBack() {
    this.router.navigate(['/components']);
  }

  get dialogTitle(): string {
    switch (this.mode) {
      case 'add':  return 'Thêm mới tham số cấu phần xử lý';
      case 'copy': return 'Thêm mới tham số cấu phần xử lý';
      case 'edit': return 'Sửa tham số cấu phần xử lý';
    }
  }

  getBreadcrumbText(): string {
    switch (this.mode) {
      case 'add':  return 'Thêm mới';
      case 'copy': return 'Thêm mới';
      case 'edit': return 'Sửa';
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
