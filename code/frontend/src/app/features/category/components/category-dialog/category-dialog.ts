import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CategoryService } from '../../services/category.service';
import { ComponentService } from '../../../processing-components/services/component.service';
import { NotificationService } from '../../../../shared/components/notification/notification.service';
import { CategorySchema, zodFormValidator, zodFieldValidator } from '../../../../shared/validators/category.schema';
import { ParamStatus, ActiveStatus, DisplayStatus, FormMode } from '../../../../shared/enums/status.enum';
import { GroupCategoryResponse, GroupCategoryRequest } from '../../../../shared/models/group-category.model';
import { LanguageService } from '../../../../core/services/language.service';
import { HttpErrorResponse } from '@angular/common/http';
import { TUI_INPUT_DATE_TIME_OPTIONS, tuiInputDateTimeOptionsProvider } from '@taiga-ui/kit';
import { DateTimeTransformer } from '../../../../shared/utils/datetime-transformer';

import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';

@Component({
  selector: 'app-category-dialog',
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
  templateUrl: './category-dialog.html',
  styleUrl: './category-dialog.css'
})
export class CategoryDialogComponent implements OnInit {
  readonly ParamStatus = ParamStatus;
  readonly ActiveStatus = ActiveStatus;
  readonly DisplayStatus = DisplayStatus;
  readonly FormMode = FormMode;

  private fb = inject(FormBuilder);
  private categoryService = inject(CategoryService);
  private componentService = inject(ComponentService);
  private notificationService = inject(NotificationService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  public languageService = inject(LanguageService);

  mode: FormMode | 'add' | 'edit' | 'copy' = FormMode.ADD;
  category: GroupCategoryResponse | null = null;
  id: number | null = null;

  constructor() {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state?.['data']) {
      this.category = navigation.extras.state['data'];
    }
  }

  dialogForm!: FormGroup;

  componentsList: { value: string; label: string }[] = [];

  activeCodes = [1, 0];
  readonly stringifyActive = (val: number): string => {
    if (val === 1) return this.languageService.labels().common.active;
    if (val === 0) return this.languageService.labels().common.inactive;
    return 'Chọn giá trị';
  };

  get componentItems(): string[] {
    return this.componentsList.map(o => o.value);
  }

  readonly stringifyComponentCode = (val: string): string => {
    if (!val) return 'Chọn giá trị';
    const found = this.componentsList.find(o => o.value === val);
    return found ? found.label : val;
  };

  ngOnInit() {
    this.initForm();
    this.detectRouteAndMode();
    this.loadActiveComponents();
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

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.id = +idParam;
      this.loadCategoryData(this.id);
    }
  }

  private loadCategoryData(id: number) {
    this.categoryService.getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.category = res.data;
          this.populateForm();
        },
        error: (err: HttpErrorResponse) => {
          if (err.status !== 401 && err.status !== 403) {
            const prefix = this.languageService.labels().messages?.errorPrefix?.loadCategory || 'Không thể nạp dữ liệu tham số: ';
            this.notificationService.error(prefix + (err.error?.message || err.message));
          }
          this.goBack();
        }
      });
  }

  private loadActiveComponents() {
    this.componentService.getActiveList(4)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          const list = res.data || [];
          this.componentsList = list.map(c => ({
            value: c.componentCode,
            label: `${c.componentCode} - ${c.componentName}`
          }));
        },
        error: () => {
          this.componentsList = [
            { value: 'RLT', label: 'RLT - Real-time Payment Component' },
            { value: 'FIM', label: 'FIM - Financial Information Module' },
            { value: 'TRA', label: 'TRA - Transaction Router Agent' }
          ];
        }
      });
  }

  private initForm() {
    this.dialogForm = this.fb.group(
      {
        paramType: ['', zodFieldValidator(CategorySchema, 'paramType')],
        paramValue: ['', zodFieldValidator(CategorySchema, 'paramValue')],
        paramName: ['', zodFieldValidator(CategorySchema, 'paramName')],
        description: ['', zodFieldValidator(CategorySchema, 'description')],
        componentCode: [[], zodFieldValidator(CategorySchema, 'componentCode')],
        effectiveDate: ['', zodFieldValidator(CategorySchema, 'effectiveDate')],
        endEffectiveDate: ['', zodFieldValidator(CategorySchema, 'endEffectiveDate')]
      },
      { validators: zodFormValidator(CategorySchema) }
    );
  }

  get parsedNewData(): Record<string, any> | null {
    if (!this.category?.newData) return null;
    try {
      return typeof this.category.newData === 'string' ? JSON.parse(this.category.newData) : this.category.newData;
    } catch { return null; }
  }

  private populateForm() {
    if (!this.category) return;
    const draft = this.parsedNewData;
    const dataToPopulate = draft ? { ...this.category, ...draft } : this.category;

    this.dialogForm.patchValue({
      paramType: dataToPopulate.paramType,
      paramValue: dataToPopulate.paramValue,
      paramName: dataToPopulate.paramName,
      description: dataToPopulate.description,
      componentCode: dataToPopulate.componentCode ? String(dataToPopulate.componentCode).split(',').map((s: string) => s.trim()).filter(Boolean) : [],
      effectiveDate: this.mode === 'copy' ? '' : dataToPopulate.effectiveDate,
      endEffectiveDate: this.mode === 'copy' ? '' : dataToPopulate.endEffectiveDate
    });

    if (this.mode === 'edit') {
      this.dialogForm.get('paramType')?.disable();
      this.dialogForm.get('paramValue')?.disable();
    }
  }

  hasFormChanged(): boolean {
    if (!this.category) return true;
    const formValue = this.dialogForm.getRawValue();
    const base = this.category;

    const normalizeDate = (val: string | number | Date | null | undefined) => {
      if (!val) return '';
      const d = new Date(val);
      if (isNaN(d.getTime())) return '';
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${y}-${m}-${day}`;
    };

    const fields: (keyof GroupCategoryResponse)[] = ['paramName', 'description'];
    for (const f of fields) {
      const orig = (base as any)[f] != null ? String((base as any)[f]).trim() : '';
      const curr = formValue[f] != null ? String(formValue[f]).trim() : '';
      if (orig !== curr) return true;
    }

    const origComponentCode = String(base.componentCode || '').split(',').map((s: string) => s.trim()).filter(Boolean).sort().join(', ');
    const currComponentCode = (formValue.componentCode || []).map((s: string) => s.trim()).filter(Boolean).sort().join(', ');
    if (origComponentCode !== currComponentCode) return true;

    if (normalizeDate(base.effectiveDate) !== normalizeDate(formValue.effectiveDate)) return true;
    if (normalizeDate(base.endEffectiveDate) !== normalizeDate(formValue.endEffectiveDate)) return true;

    return false;
  }

  onSubmit(sendForApproval = false) {
    const raw = this.dialogForm.getRawValue();
    const mappedRaw = {
      ...raw,
      componentCode: Array.isArray(raw.componentCode) ? raw.componentCode.join(', ') : (raw.componentCode || '')
    };
    const parseResult = CategorySchema.safeParse(mappedRaw);

    if (!parseResult.success) {
      this.markFormGroupTouched(this.dialogForm);
      const issues = parseResult.error.issues;
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

    const dto: GroupCategoryRequest = {
      ...mappedRaw,
      effectiveDate: this.formatToISO(mappedRaw.effectiveDate),
      endEffectiveDate: mappedRaw.endEffectiveDate ? this.formatToISO(mappedRaw.endEffectiveDate) : undefined
    };

    const msgs = this.languageService.labels().messages;

    if (this.mode === 'edit' && this.category) {
      this.categoryService.update(this.category.id, dto)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            if (sendForApproval && this.category) {
              this.categoryService.sendApproval(this.category.id)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                  next: () => {
                    this.notificationService.success(msgs?.success?.title || 'Thành công!', msgs?.success?.updateAndSendApproval || 'Cập nhật và Gửi duyệt thành công', '/categories/detail/' + this.category!.id);
                    this.goBack();
                  },
                  error: (err: HttpErrorResponse) => {
                    this.notificationService.error((msgs?.errorPrefix?.sendApproval || 'Lỗi gửi duyệt: ') + (err.error?.message || err.message));
                    this.goBack();
                  }
                });
            } else {
              this.notificationService.success(msgs?.success?.title || 'Thành công!', msgs?.success?.update || 'Cập nhật thành công', '/categories/detail/' + this.category!.id);
              this.goBack();
            }
          },
          error: (err: HttpErrorResponse) => {
            this.notificationService.error((msgs?.errorPrefix?.update || 'Lỗi cập nhật: ') + (err.error?.message || err.message));
          }
        });
    } else {
      this.categoryService.create(dto)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (res) => {
            const newId = res.data.id;
            if (sendForApproval && newId) {
              this.categoryService.sendApproval(newId)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                  next: () => {
                    this.notificationService.success(msgs?.success?.title || 'Thành công!', msgs?.success?.createAndSendApproval || 'Thêm mới và Gửi duyệt thành công', '/categories/detail/' + newId);
                    this.goBack();
                  },
                  error: (err: HttpErrorResponse) => {
                    this.notificationService.error((msgs?.errorPrefix?.saveAndSendFailed || 'Đã lưu bản ghi, nhưng lỗi gửi duyệt: ') + (err.error?.message || err.message));
                    this.goBack();
                  }
                });
            } else {
              this.notificationService.success(msgs?.success?.title || 'Thành công!', msgs?.success?.create || 'Thêm mới thành công', '/categories/detail/' + newId);
              this.goBack();
            }
          },
          error: (err: HttpErrorResponse) => {
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

  private markFormGroupTouched(formGroup: FormGroup) {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  selectAllComponents() {
    this.dialogForm.get('componentCode')?.setValue(this.componentItems);
  }

  clearAllComponents() {
    this.dialogForm.get('componentCode')?.setValue([]);
  }

  goBack() {
    this.router.navigate(['/categories']);
  }

  get dialogTitle(): string {
    switch (this.mode) {
      case FormMode.ADD:
      case 'add':
        return 'Thêm mới tham số cấu phần xử lý';
      case FormMode.COPY:
      case 'copy':
        return 'Thêm mới tham số cấu phần xử lý';
      case FormMode.EDIT:
      case 'edit':
        return 'Sửa tham số cấu phần xử lý';
      default:
        return 'Thêm mới tham số cấu phần xử lý';
    }
  }

  getBreadcrumbText(): string {
    switch (this.mode) {
      case FormMode.ADD:
        return 'Thêm mới';
      case FormMode.COPY:
        return 'Thêm mới';
      case FormMode.EDIT:
        return 'Sửa';
      default:
        return 'Chi tiết';
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
