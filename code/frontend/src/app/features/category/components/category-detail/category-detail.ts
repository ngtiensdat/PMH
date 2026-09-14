import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { CategoryService } from '../../services/category.service';
import { GroupCategoryResponse } from '../../../../shared/models/group-category.model';
import { ApiResponse } from '../../../../shared/models/api-response.model';
import { BaseDetailComponent } from '../../../../shared/components/base-detail/base-detail.component';
import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';
import { ComparisonViewComponent, FieldConfig } from '../../../../shared/components/comparison-view/comparison-view';
import { DetailFooterActionsComponent } from '../../../../shared/components/detail-footer-actions/detail-footer-actions.component';
import { formatDateTimeDisplay } from '../../../../shared/utils/date.utils';

@Component({
  selector: 'app-category-detail',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule, ComparisonViewComponent, DetailFooterActionsComponent],
  templateUrl: './category-detail.html',
  styleUrl: './category-detail.css'
})
export class CategoryDetailComponent
  extends BaseDetailComponent<GroupCategoryResponse, number>
  implements OnInit {

  // ── Config ─────────────────────────────────────────────────────────────────
  protected override readonly routeParamKey = 'id';
  protected override readonly listRoute = '/categories';

  // ── Service ────────────────────────────────────────────────────────────────
  private categoryService = inject(CategoryService);

  // ── Field config
  readonly fieldConfig: FieldConfig[] = [
    { key: 'paramName', label: 'Tên thành phần' },
    { key: 'paramValue', label: 'Giá trị thành phần' },
    { key: 'paramType', label: 'Danh mục theo nhóm' },
    { key: 'componentCode', label: 'Cấu phần xử lý' },
    {
      key: 'effectiveDate', label: 'Ngày hiệu lực',
      format: (val) => formatDateTimeDisplay(val as string | number | Date)
    },
    {
      key: 'endEffectiveDate', label: 'Ngày hết hiệu lực',
      format: (val) => val ? formatDateTimeDisplay(val as string | number | Date) : '-'
    },
    { key: 'description', label: 'Mô tả' },
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
  protected override callDelete(key: number): Observable<ApiResponse<unknown>> { return this.categoryService.delete(key); }
  protected override callSendApproval(key: number): Observable<ApiResponse<unknown>> { return this.categoryService.sendApproval(key); }
  protected override callBatchApprove(keys: number[]): Observable<ApiResponse<unknown>> { return this.categoryService.batchApprove(keys); }
  protected override callBatchReject(keys: number[], reason: string): Observable<ApiResponse<unknown>> { return this.categoryService.batchReject(keys, reason); }
}
