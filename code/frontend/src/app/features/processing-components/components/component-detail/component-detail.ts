import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentService } from '../../services/component.service';
import { ProcessingComponentResponse } from '../../../../shared/models/component.model';
import { ApiResponse } from '../../../../shared/models/api-response.model';
import { BaseDetailComponent } from '../../../../shared/components/base-detail/base-detail.component';
import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';
import { ComparisonCardComponent } from '../../../../shared/components/comparison-card/comparison-card';
import { formatDateTimeDisplay } from '../../../../shared/utils/date.utils';

@Component({
  selector: 'app-component-detail',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule, ComparisonCardComponent],
  templateUrl: './component-detail.html',
  styleUrl: './component-detail.css'
})
export class ComponentDetailComponent
  extends BaseDetailComponent<ProcessingComponentResponse, string>
  implements OnInit {

  // ── Config ─────────────────────────────────────────────────────────────────
  protected override readonly routeParamKey = 'code';
  protected override readonly listRoute     = '/components';

  // ── Service ────────────────────────────────────────────────────────────────
  private componentService = inject(ComponentService);

  // ── Fields to display ──────────────────────────────────────────────────────
  override readonly fields = [
    'componentCode', 'componentName', 'messageType', 'connectionMethod',
    'effectiveDate', 'endEffectiveDate', 'checkToken', 'isActive', 'description'
  ];

  // ── Template alias (template dùng 'component', không đổi template) ─────────
  get component(): ProcessingComponentResponse | null { return this.entity; }

  // ── Key ────────────────────────────────────────────────────────────────────
  protected override getEntityKey(): string { return this.entity!.componentCode; }

  // ── Load ───────────────────────────────────────────────────────────────────
  protected override loadEntityData(keyStr: string): void {
    this.isLoading.set(true);
    this.componentService.getByCode(keyStr).subscribe({
      next: (res) => { this.entity = res.data; this.isLoading.set(false); },
      error: (err: HttpErrorResponse) => {
        if (err.status !== 401) {
          const prefix = this.languageService.labels().messages?.errorPrefix?.loadDetail || 'Không thể nạp dữ liệu chi tiết cấu phần: ';
          this.notificationService.error(prefix + (err.error?.message || err.message));
        }
        this.isLoading.set(false);
        this.goBack();
      }
    });
  }

  // ── Service calls ──────────────────────────────────────────────────────────
  protected override callDelete(key: string): Observable<ApiResponse<unknown>>                          { return this.componentService.delete(key); }
  protected override callSendApproval(key: string): Observable<ApiResponse<unknown>>                    { return this.componentService.sendApproval(key); }
  protected override callBatchApprove(keys: string[]): Observable<ApiResponse<unknown>>                 { return this.componentService.batchApprove(keys); }
  protected override callBatchReject(keys: string[], reason: string): Observable<ApiResponse<unknown>>  { return this.componentService.batchReject(keys, reason); }

  // ── Labels & format ────────────────────────────────────────────────────────
  override getFieldLabel(field: string): string {
    const labels: Record<string, string> = {
      componentCode:    'Mã cấu phần',
      componentName:    'Tên cấu phần',
      messageType:      'Chuẩn tin điện',
      connectionMethod: 'Phương thức kết nối',
      effectiveDate:    'Ngày hiệu lực',
      endEffectiveDate: 'Ngày hết hiệu lực',
      checkToken:       'Kiểm tra Token',
      isActive:         'Trạng thái hoạt động',
      description:      'Mô tả'
    };
    return labels[field] || field;
  }

  override formatValue(field: string, val: unknown): string {
    if (val === undefined || val === null || val === '') return '-';
    if (field === 'effectiveDate' || field === 'endEffectiveDate')
      return formatDateTimeDisplay(val as string | number | Date);
    if (field === 'isActive')   return val === 1 ? 'Hoạt động' : 'Không hoạt động';
    if (field === 'checkToken') return val === 'Y' ? 'Có kiểm tra' : 'Không kiểm tra';
    return String(val);
  }
}
