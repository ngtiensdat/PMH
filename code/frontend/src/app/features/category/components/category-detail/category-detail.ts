import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { CategoryService } from '../../services/category.service';
import { NotificationService } from '../../../../shared/components/notification/notification.service';
import { GroupCategoryResponse } from '../../../../shared/models/group-category.model';
import { LanguageService } from '../../../../core/services/language.service';
import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';
import { HttpErrorResponse } from '@angular/common/http';
import { ComparisonCardComponent } from '../../../../shared/components/comparison-card/comparison-card';

@Component({
  selector: 'app-category-detail',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule, ComparisonCardComponent],
  templateUrl: './category-detail.html',
  styleUrl: './category-detail.css'
})
export class CategoryDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private categoryService = inject(CategoryService);
  private notificationService = inject(NotificationService);
  public languageService = inject(LanguageService);

  category: GroupCategoryResponse | null = null;
  id: number | null = null;
  isLoading = signal<boolean>(false);

  constructor() {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state?.['data']) {
      this.category = navigation.extras.state['data'];
    }
  }

  fields = [
    'paramName',
    'paramValue',
    'paramType',
    'componentCode',
    'effectiveDate',
    'endEffectiveDate',
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
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.id = +idParam;
      this.loadCategoryData(this.id);
    }
  }

  private loadCategoryData(id: number) {
    this.isLoading.set(true);
    this.categoryService.getById(id).subscribe({
      next: (res) => {
        this.category = res.data;
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.notificationService.error('Không thể nạp dữ liệu chi tiết tham số: ' + (err.error?.message || err.message));
        this.isLoading.set(false);
        this.goBack();
      }
    });
  }

  get parsedNewData(): Record<string, unknown> | null {
    if (!this.category?.newData) return null;
    try {
      return typeof this.category.newData === 'string' ? JSON.parse(this.category.newData) : this.category.newData;
    } catch { return null; }
  }

  get oldData(): Record<string, unknown> {
    if (!this.category) return {};

    if (this.category.status === 1 || this.category.isDisplay === 1) {
      return {};
    }
    return this.category as unknown as Record<string, unknown>;
  }

  get newData(): Record<string, unknown> {
    if (!this.category) return {};

    const nd = this.parsedNewData;
    if (nd) {
      return nd;
    }
    return this.category as unknown as Record<string, unknown>;
  }

  getFieldLabel(field: string): string {
    const labels: { [key: string]: string } = {
      paramName: 'Tên thành phần',
      paramValue: 'Giá trị thành phần',
      paramType: 'Danh mục theo nhóm',
      componentCode: 'Cấu phần xử lý',
      effectiveDate: 'Ngày hiệu lực',
      endEffectiveDate: 'Ngày hết hiệu lực',
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
    if (confirm('Bạn có chắc chắn muốn xóa bản ghi này?')) {
      this.categoryService.delete(this.category!.id).subscribe({
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
    this.categoryService.sendApproval(this.category!.id).subscribe({
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
    if (confirm('Bạn có chắc chắn muốn duyệt bản ghi này?')) {
      this.categoryService.batchApprove([this.category!.id]).subscribe({
        next: () => {
          this.notificationService.success('Duyệt bản ghi thành công!');
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

    this.categoryService.batchReject([this.category!.id], reason).subscribe({
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
    this.router.navigate(['/categories']);
  }
}
