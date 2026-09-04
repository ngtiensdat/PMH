import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CategoryService } from '../../services/category.service';
import { ComponentService } from '../../../processing-components/services/component.service';
import { HttpErrorResponse } from '@angular/common/http';
import { CategorySchema, zodFormValidator, zodFieldValidator } from '../../../../shared/validators/category.schema';
import { GroupCategoryResponse, GroupCategoryRequest } from '../../../../shared/models/group-category.model';
import { ApiResponse } from '../../../../shared/models/api-response.model';
import { BaseDialogComponent } from '../../../../shared/components/base-dialog/base-dialog.component';
import { formatToISO } from '../../../../shared/utils/date.utils';
import { tuiInputDateTimeOptionsProvider } from '@taiga-ui/kit';
import { DateTimeTransformer } from '../../../../shared/utils/datetime-transformer';
import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';

@Component({
  selector: 'app-category-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SharedTaigaModule],
  providers: [tuiInputDateTimeOptionsProvider({ valueTransformer: new DateTimeTransformer() })],
  templateUrl: './category-dialog.html',
  styleUrl: './category-dialog.css'
})
export class CategoryDialogComponent
  extends BaseDialogComponent<GroupCategoryResponse, number, GroupCategoryRequest>
  implements OnInit {

  // ── Config ─────────────────────────────────────────────────────────────────
  protected override readonly routeParamName = 'id';
  protected override readonly listRoute      = '/categories';
  protected override readonly detailPath     = '/categories/detail';

  // ── Services ───────────────────────────────────────────────────────────────
  private categoryService  = inject(CategoryService);
  private componentService = inject(ComponentService);

  // ── Domain state ───────────────────────────────────────────────────────────
  componentsList: { value: string; label: string }[] = [];

  get componentItems(): string[] { return this.componentsList.map(o => o.value); }

  readonly stringifyComponentCode = (val: string): string => {
    if (!val) return 'Chọn giá trị';
    const found = this.componentsList.find(o => o.value === val);
    return found ? found.label : val;
  };

  // ── Service calls ──────────────────────────────────────────────────────────
  protected override callCreate(dto: GroupCategoryRequest): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.categoryService.create(dto);
  }
  protected override callUpdate(key: number, dto: GroupCategoryRequest): Observable<ApiResponse<unknown>> {
    return this.categoryService.update(key, dto);
  }
  protected override callSendApproval(key: number): Observable<ApiResponse<unknown>> {
    return this.categoryService.sendApproval(key);
  }
  protected override getEntityKey(entity: GroupCategoryResponse): number { return entity.id; }

  protected override loadEntityData(keyParam: string): void {
    this.categoryService.getById(+keyParam)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => { this.entity = res.data; this.populateForm(this.entity); },
        error: (err: HttpErrorResponse) => {
          if (err.status !== 401 && err.status !== 403) {
            const prefix = this.languageService.labels().messages?.errorPrefix?.loadCategory || 'Không thể nạp dữ liệu tham số: ';
            this.notificationService.error(prefix + (err.error?.message || err.message));
          }
          this.goBack();
        }
      });
  }

  protected override resetListState(): void {
    const s = this.categoryService.getListState();
    if (s) this.categoryService.setListState({ ...s, page: 0 });
  }

  // ── Form ───────────────────────────────────────────────────────────────────
  override initForm(): void {
    this.dialogForm = this.fb.group(
      {
        paramType:        ['', zodFieldValidator(CategorySchema, 'paramType')],
        paramValue:       ['', zodFieldValidator(CategorySchema, 'paramValue')],
        paramName:        ['', zodFieldValidator(CategorySchema, 'paramName')],
        description:      ['', zodFieldValidator(CategorySchema, 'description')],
        componentCode:    [[], zodFieldValidator(CategorySchema, 'componentCode')],
        effectiveDate:    ['', zodFieldValidator(CategorySchema, 'effectiveDate')],
        endEffectiveDate: ['', zodFieldValidator(CategorySchema, 'endEffectiveDate')]
      },
      { validators: zodFormValidator(CategorySchema) }
    );
  }

  override populateForm(entity: GroupCategoryResponse): void {
    const draft = this.parsedNewData;
    const data  = draft ? { ...entity, ...draft } : entity;

    this.dialogForm.patchValue({
      paramType:        data.paramType,
      paramValue:       data.paramValue,
      paramName:        data.paramName,
      description:      data.description,
      componentCode:    data.componentCode
                          ? String(data.componentCode).split(',').map((s: string) => s.trim()).filter(Boolean)
                          : [],
      effectiveDate:    this.mode === 'copy' ? '' : data.effectiveDate,
      endEffectiveDate: this.mode === 'copy' ? '' : data.endEffectiveDate
    });

    if (this.mode === 'edit') {
      this.dialogForm.get('paramType')?.disable();
      this.dialogForm.get('paramValue')?.disable();
    }
  }

  override normalizeRaw(raw: Record<string, unknown>): Record<string, unknown> {
    return {
      ...raw,
      componentCode: Array.isArray(raw['componentCode'])
        ? (raw['componentCode'] as string[]).join(', ')
        : (raw['componentCode'] || '')
    };
  }

  override validateNormalized(mapped: Record<string, unknown>): { success: boolean; error?: string } {
    const result = CategorySchema.safeParse(mapped);
    if (result.success) return { success: true };
    return { success: false, error: result.error.issues[0]?.message };
  }

  override buildDto(mapped: Record<string, unknown>): GroupCategoryRequest {
    return {
      ...(mapped as unknown as GroupCategoryRequest),
      effectiveDate:    formatToISO(mapped['effectiveDate'] as string),
      endEffectiveDate: mapped['endEffectiveDate'] ? formatToISO(mapped['endEffectiveDate'] as string) : undefined
    };
  }

  override hasFormChanged(): boolean {
    if (!this.entity) return true;
    const fv   = this.dialogForm.getRawValue();
    const base = this.entity as unknown as Record<string, unknown>;

    for (const f of ['paramName', 'description'] as const) {
      if ((base[f] != null ? String(base[f]).trim() : '') !== (fv[f] != null ? String(fv[f]).trim() : '')) return true;
    }

    const origCode = String(this.entity.componentCode || '').split(',').map((s: string) => s.trim()).filter(Boolean).sort().join(', ');
    const currCode = (fv.componentCode || []).map((s: string) => s.trim()).filter(Boolean).sort().join(', ');
    if (origCode !== currCode) return true;

    if (this.normalizeDate(this.entity.effectiveDate)    !== this.normalizeDate(fv.effectiveDate))    return true;
    if (this.normalizeDate(this.entity.endEffectiveDate) !== this.normalizeDate(fv.endEffectiveDate)) return true;

    return false;
  }

  // ── Dialog title ───────────────────────────────────────────────────────────
  override get dialogTitle(): string {
    return this.mode === 'edit'
      ? 'Sửa tham số danh mục theo nhóm'
      : 'Thêm mới tham số danh mục theo nhóm';
  }

  // ── Domain actions ─────────────────────────────────────────────────────────
  selectAllComponents(): void { this.dialogForm.get('componentCode')?.setValue(this.componentItems); }
  clearAllComponents():  void { this.dialogForm.get('componentCode')?.setValue([]); }

  private loadActiveComponents(): void {
    this.componentService.getActiveList(4)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.componentsList = (res.data || []).map(c => ({
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

  // ── Lifecycle (override để thêm loadActiveComponents) ────────────────────
  override ngOnInit(): void {
    super.ngOnInit();
    this.loadActiveComponents();
  }
}
