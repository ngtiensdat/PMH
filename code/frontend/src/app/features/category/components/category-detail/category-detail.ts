import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { CategoryService } from '../../services/category.service';
import { GroupCategoryResponse } from '../../../../shared/models/group-category.model';
import { ApiResponse } from '../../../../shared/models/api-response.model';
import { BaseDetailComponent } from '../../../../shared/components/base-detail/base-detail.component';
import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';
import { ComparisonCardComponent } from '../../../../shared/components/comparison-card/comparison-card';
import { formatDateTimeDisplay } from '../../../../shared/utils/date.utils';

@Component({
  selector: 'app-category-detail',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule, ComparisonCardComponent],
  templateUrl: './category-detail.html',
  styleUrl: './category-detail.css'
})
export class CategoryDetailComponent
  extends BaseDetailComponent<GroupCategoryResponse, number>
  implements OnInit {

  // ── Config ─────────────────────────────────────────────────────────────────
  protected override readonly routeParamKey = 'id';
  protected override readonly listRoute     = '/categories';

  // ── Service ────────────────────────────────────────────────────────────────
  private categoryService = inject(CategoryService);

  // ── Fields to display ──────────────────────────────────────────────────────
  override readonly fields = [
    'paramName', 'paramValue', 'paramType',
    'componentCode', 'effectiveDate', 'endEffectiveDate', 'description'
  ];

  // ── Template alias (template dùng 'category', không đổi template) ──────────
  get category(): GroupCategoryResponse | null { return this.entity; }

  // ── Key ────────────────────────────────────────────────────────────────────
  protected override getEntityKey(): number { return this.entity!.id; }

  // ── Load ───────────────────────────────────────────────────────────────────
  protected override loadEntityData(keyStr: string): void {
    this.isLoading.set(true);
    this.categoryService.getById(+keyStr).subscribe({
      next: (res) => { this.entity = res.data; this.isLoading.set(false); },
      error: (err: HttpErrorResponse) => {
        if (err.status !== 401) {
          const prefix = this.languageService.labels().messages?.errorPrefix?.loadDetail || 'Không thể nạp dữ liệu chi tiết tham số: ';
          this.notificationService.error(prefix + (err.error?.message || err.message));
        }
        this.isLoading.set(false);
        this.goBack();
      }
    });
  }

  // ── Service calls ──────────────────────────────────────────────────────────
  protected override callDelete(key: number): Observable<ApiResponse<unknown>>                          { return this.categoryService.delete(key); }
  protected override callSendApproval(key: number): Observable<ApiResponse<unknown>>                    { return this.categoryService.sendApproval(key); }
  protected override callBatchApprove(keys: number[]): Observable<ApiResponse<unknown>>                 { return this.categoryService.batchApprove(keys); }
  protected override callBatchReject(keys: number[], reason: string): Observable<ApiResponse<unknown>>  { return this.categoryService.batchReject(keys, reason); }

  // ── Labels & format ────────────────────────────────────────────────────────
  override getFieldLabel(field: string): string {
    const labels: Record<string, string> = {
      paramName:        'Tên thành phần',
      paramValue:       'Giá trị thành phần',
      paramType:        'Danh mục theo nhóm',
      componentCode:    'Cấu phần xử lý',
      effectiveDate:    'Ngày hiệu lực',
      endEffectiveDate: 'Ngày hết hiệu lực',
      description:      'Mô tả'
    };
    return labels[field] || field;
  }

  override formatValue(field: string, val: unknown): string {
    if (val === undefined || val === null || val === '') return '-';
    if (field === 'effectiveDate' || field === 'endEffectiveDate')
      return formatDateTimeDisplay(val as string | number | Date);
    return String(val);
  }
}
