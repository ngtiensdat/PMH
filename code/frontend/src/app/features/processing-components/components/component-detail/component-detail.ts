import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentService } from '../../services/component.service';
import { ProcessingComponentResponse } from '../../../../shared/models/component.model';
import { ApiResponse } from '../../../../shared/models/api-response.model';
import { BaseDetailComponent } from '../../../../shared/components/base-detail/base-detail.component';
import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';
import { ComparisonViewComponent, FieldConfig } from '../../../../shared/components/comparison-view/comparison-view';
import { DetailFooterActionsComponent } from '../../../../shared/components/detail-footer-actions/detail-footer-actions.component';
import { formatDateTimeDisplay } from '../../../../shared/utils/date.utils';

@Component({
  selector: 'app-component-detail',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule, ComparisonViewComponent, DetailFooterActionsComponent],
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

  // ── Field config: khai báo 1 lần, plain data, không function reference thay đổi ─
  readonly fieldConfig: FieldConfig[] = [
    { key: 'componentCode',    label: 'Mã cấu phần' },
    { key: 'componentName',    label: 'Tên cấu phần' },
    { key: 'messageType',      label: 'Chuẩn tin điện' },
    { key: 'connectionMethod', label: 'Phương thức kết nối' },
    { key: 'effectiveDate',    label: 'Ngày hiệu lực',
      format: (val) => formatDateTimeDisplay(val as string | number | Date) },
    { key: 'endEffectiveDate', label: 'Ngày hết hiệu lực',
      format: (val) => val ? formatDateTimeDisplay(val as string | number | Date) : '-' },
    { key: 'checkToken',       label: 'Kiểm tra Token',
      format: (val) => val === 'Y' ? 'Có kiểm tra' : 'Không kiểm tra' },
    { key: 'isActive',         label: 'Trạng thái hoạt động',
      format: (val) => val === 1 ? 'Hoạt động' : 'Không hoạt động' },
    { key: 'description',      label: 'Mô tả' },
  ];

  // ── Template alias (template dùng 'component', không đổi template) ────────
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
}
