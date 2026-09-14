import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ComparisonCardComponent, ComparisonRow } from '../comparison-card/comparison-card';
import { LanguageService } from '../../../core/services/language.service';

// ── Interface: mỗi module khai báo 1 lần dưới dạng plain data ──────────────
export interface FieldConfig {
  key: string;
  label: string;
  format?: (val: unknown) => string;
}

@Component({
  selector: 'app-comparison-view',
  standalone: true,
  imports: [CommonModule, ComparisonCardComponent],
  styleUrl: './comparison-view.css',
  template: `
    <div class="comparison-grid">
      <app-comparison-card
        [title]="languageService.labels().common.oldData"
        cardType="old"
        [rows]="oldRows"
      ></app-comparison-card>

      <app-comparison-card
        [title]="languageService.labels().common.newData"
        cardType="new"
        [rows]="newRows"
      ></app-comparison-card>
    </div>
  `
})
export class ComparisonViewComponent {
  public languageService = inject(LanguageService);

  @Input() oldData: Record<string, unknown> = {};
  @Input() newData: Record<string, unknown> = {};
  @Input() fieldConfig: FieldConfig[] = [];

  private renderValue(config: FieldConfig, val: unknown): string {
    if (val === undefined || val === null || val === '') return '-';
    return config.format ? config.format(val) : String(val);
  }

  private isChanged(config: FieldConfig): boolean {
    const oldVal = this.renderValue(config, this.oldData[config.key]);
    const newVal = this.renderValue(config, this.newData[config.key]);
    if (oldVal === '-' && newVal === '-') return false;
    return oldVal !== newVal;
  }

  // ── Computed rows ────────────────────────────────────────────────────────────
  get oldRows(): ComparisonRow[] {
    return this.fieldConfig.map(cfg => ({
      label: cfg.label,
      value: this.renderValue(cfg, this.oldData[cfg.key]),
      isChanged: this.isChanged(cfg)
    }));
  }

  get newRows(): ComparisonRow[] {
    return this.fieldConfig.map(cfg => ({
      label: cfg.label,
      value: this.renderValue(cfg, this.newData[cfg.key]),
      isChanged: this.isChanged(cfg)
    }));
  }

  // ──khi nào hiển thị 1 thẻ hay 2 cột─────────────────────────────
  get isSingleView(): boolean {
    // 1. Nếu oldData rỗng (bản ghi tạo mới tinh chưa duyệt) ➔ Xem 1 thẻ đơn
    if (!this.oldData || Object.keys(this.oldData).length === 0) return true;

    // 2. Nếu không có bất kỳ dòng nào bị thay đổi (bản ghi đang chạy bình thường) ➔ Xem 1 thẻ đơn
    const hasAnyChange = this.newRows.some(row => row.isChanged);
    return !hasAnyChange;
  }
}
