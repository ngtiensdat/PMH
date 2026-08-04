import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { ComponentService } from '../../services/component.service';
import { NotificationService } from '../../../../shared/components/notification/notification.service';
import { ProcessingComponentResponse } from '../../../../shared/models/component.model';
import { LanguageService } from '../../../../core/services/language.service';
import { ComparisonCardComponent } from '../../../../shared/components/comparison-card/comparison-card';

import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';

@Component({
  selector: 'app-component-detail',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule, ComparisonCardComponent],
  templateUrl: './component-detail.html',
  styleUrl: './component-detail.css'
})
export class ComponentDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private componentService = inject(ComponentService);
  private notificationService = inject(NotificationService);
  public languageService = inject(LanguageService);

  component: ProcessingComponentResponse | null = null;
  code: string | null = null;
  isLoading = signal<boolean>(false);

  constructor() {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state?.['data']) {
      this.component = navigation.extras.state['data'];
    }
  }

  fields = [
    'componentCode',
    'componentName',
    'messageType',
    'connectionMethod',
    'effectiveDate',
    'endEffectiveDate',
    'checkToken',
    'isActive',
    'description'
  ];

  statusMap: { [key: number]: { label: string, css: string } } = {
    1: { label: 'Tạo mới', css: 'badge-new' },
    3: { label: 'Chờ duyệt', css: 'badge-pending' },
    4: { label: 'Đã duyệt', css: 'badge-approved' },
    5: { label: 'Từ chối', css: 'badge-rejected' },
    7: { label: 'Hủy duyệt', css: 'badge-canceled' }
  };

  ngOnInit() {
    const codeParam = this.route.snapshot.paramMap.get('code');
    if (codeParam) {
      this.code = codeParam;
      this.loadComponentData(this.code);
    }
  }

  private loadComponentData(code: string) {
    this.isLoading.set(true);
    this.componentService.getByCode(code).subscribe({
      next: (res) => {
        this.component = res.data;
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.notificationService.error('Không thể nạp dữ liệu chi tiết cấu phần: ' + (err.error?.message || err.message));
        this.isLoading.set(false);
        this.goBack();
      }
    });
  }

  get parsedNewData(): Record<string, unknown> | null {
    if (!this.component?.newData) return null;
    try {
      return typeof this.component.newData === 'string' ? JSON.parse(this.component.newData) : this.component.newData;
    } catch { return null; }
  }

  get oldData(): Record<string, unknown> {
    if (!this.component) return {};
    
    if (this.component.status === 1 || this.component.isDisplay === 1) {
      return {};
    }
    return this.component as unknown as Record<string, unknown>;
  }

  get newData(): Record<string, unknown> {
    if (!this.component) return {};
    
    const nd = this.parsedNewData;
    if (nd) {
      return nd;
    }
    return this.component as unknown as Record<string, unknown>;
  }

  getFieldLabel(field: string): string {
    const labels: { [key: string]: string } = {
      componentCode: 'Mã cấu phần',
      componentName: 'Tên cấu phần',
      messageType: 'Chuẩn tin điện',
      connectionMethod: 'Phương thức kết nối',
      effectiveDate: 'Ngày hiệu lực',
      endEffectiveDate: 'Ngày hết hiệu lực',
      checkToken: 'Kiểm tra Token',
      isActive: 'Trạng thái hoạt động',
      description: 'Mô tả'
    };
    return labels[field] || field;
  }

  formatValue(field: string, val: unknown): string {
    if (val === undefined || val === null || val === '') return '-';
    
    if (field === 'effectiveDate' || field === 'endEffectiveDate') {
      const d = new Date(val as string | number | Date);
      if (isNaN(d.getTime())) return String(val);
      const pad = (n: number) => n.toString().padStart(2, '0');
      return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
    }
    if (field === 'isActive') {
      return val === 1 ? 'Hoạt động' : 'Không hoạt động';
    }
    if (field === 'checkToken') {
      return val === 'Y' ? 'Có kiểm tra' : 'Không kiểm tra';
    }
    return String(val);
  }

  isFieldChanged(field: string): boolean {
    const oldVal = this.formatValue(field, this.oldData[field]);
    const newVal = this.formatValue(field, this.newData[field]);
    
    if (oldVal === '-' && newVal === '-') return false;
    return oldVal !== newVal;
  }

  get oldDataRows() {
    return this.fields.map(f => ({
      label: this.getFieldLabel(f),
      value: this.formatValue(f, this.oldData[f]),
      isChanged: this.isFieldChanged(f)
    }));
  }

  get newDataRows() {
    return this.fields.map(f => ({
      label: this.getFieldLabel(f),
      value: this.formatValue(f, this.newData[f]),
      isChanged: this.isFieldChanged(f)
    }));
  }

  onDeleteRecord() {
    if (confirm('Bạn có chắc chắn muốn xóa cấu phần xử lý này?')) {
      this.componentService.delete(this.component!.componentCode).subscribe({
        next: () => {
          this.notificationService.success('Xóa thành công!');
          this.goBack();
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.error('Không thể xóa: ' + (err.error?.message || err.message));
        }
      });
    }
  }

  onSendApprovalRecord() {
    this.componentService.sendApproval(this.component!.componentCode).subscribe({
      next: () => {
        this.notificationService.success('Gửi duyệt thành công!');
        this.goBack();
      },
      error: (err: HttpErrorResponse) => {
        this.notificationService.error('Lỗi gửi duyệt: ' + (err.error?.message || err.message));
      }
    });
  }

  // Reject Dialog state
  isRejectOpen = false;
  rejectReason = '';

  onApproveRecord() {
    if (confirm('Bạn có chắc chắn muốn duyệt cấu phần xử lý này?')) {
      this.componentService.batchApprove([this.component!.componentCode]).subscribe({
        next: () => {
          this.notificationService.success('Duyệt cấu phần thành công!');
          this.goBack();
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.error('Lỗi khi duyệt: ' + (err.error?.message || err.message));
        }
      });
    }
  }

  onRejectRecord() {
    this.rejectReason = '';
    this.isRejectOpen = true;
  }

  onRejectReasonInput(val: string) {
    this.rejectReason = val;
  }

  onConfirmReject() {
    const reason = this.rejectReason.trim();
    this.isRejectOpen = false;

    this.componentService.batchReject([this.component!.componentCode], reason).subscribe({
      next: () => {
        this.notificationService.success('Từ chối duyệt thành công! Lý do: ' + reason);
        this.goBack();
      },
      error: (err: HttpErrorResponse) => {
        this.notificationService.error('Lỗi khi từ chối: ' + (err.error?.message || err.message));
      }
    });
  }

  goBack() {
    this.router.navigate(['/components']);
  }
}
