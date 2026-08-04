import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedTaigaModule } from '../../shared-taiga.module';

export interface ComparisonRow {
  label: string;
  value: string;
  isChanged: boolean;
}

@Component({
  selector: 'app-comparison-card',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule],
  styleUrl: './comparison-card.css',
  template: `
    <div class="comparison-card" [ngClass]="cardType === 'old' ? 'old-data-card' : 'new-data-card'">
      <div class="comparison-card-header" [ngClass]="cardType === 'old' ? 'header-old' : 'header-new'">
        <span class="card-header-icon" [ngClass]="cardType === 'old' ? 'orange-icon' : 'green-icon'">
          <tui-icon [icon]="cardType === 'old' ? '@tui.triangle-alert' : '@tui.info'" style="--tui-icon-size: 1.15rem;"></tui-icon>
        </span>
        <span class="card-header-title">{{ title }}</span>
      </div>
      <div class="comparison-card-body">
        <div 
          class="info-row-detail" 
          *ngFor="let row of rows"
          [class.is-changed]="row.isChanged"
        >
          <span class="info-label-detail">{{ row.label }}</span>
          <span class="info-value-detail">{{ row.value }}</span>
        </div>
      </div>
    </div>
  `
})
export class ComparisonCardComponent {
  @Input() title: string = '';
  @Input() cardType: 'old' | 'new' = 'old';
  @Input() rows: ComparisonRow[] = [];
}
