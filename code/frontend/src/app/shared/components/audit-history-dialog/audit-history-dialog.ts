import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { ACTION_PILL_MAP } from '../../constants/status.constants';
import { NotificationService } from '../notification/notification.service';
import { LanguageService } from '../../../core/services/language.service';
import { SharedTaigaModule } from '../../shared-taiga.module';
import { HttpErrorResponse } from '@angular/common/http';

export interface MappedHistoryItem {
  user: {
    name: string;
    code: string;
    avatar: string;
  };
  date: string;
  action: string;
  ip: string;
  content: string;
}

@Component({
  selector: 'app-audit-history-dialog',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule],
  templateUrl: './audit-history-dialog.html',
  styleUrl: './audit-history-dialog.css'
})
export class AuditHistoryDialogComponent implements OnChanges {
  @Input() isOpen = false;
  @Input() targetId: number | string | null = null;
  @Input() targetName = '';
  @Input() fetchFn?: (id: any, page: number, size: number) => Observable<any>;

  @Output() close = new EventEmitter<void>();

  private notificationService = inject(NotificationService);
  public languageService = inject(LanguageService);
  private cdr = inject(ChangeDetectorRef);

  historyData: MappedHistoryItem[] = [];
  historyPage = 0;
  readonly historyPageSize = 5;
  historyTotalPages = 1;
  isLoading = false;

  ngOnChanges(changes: SimpleChanges) {
    if (changes['isOpen'] && this.isOpen && this.targetId !== null) {
      this.historyPage = 0;
      this.loadHistoryData();
    }
  }

  loadHistoryData() {
    if (this.targetId === null || !this.fetchFn) return;
    this.isLoading = true;
    this.fetchFn(this.targetId, this.historyPage, this.historyPageSize).subscribe({
      next: (res) => {
        const pageData = res.data;
        const mapped = (pageData.content || []).map((log: any) => {
          const name = log.performedBy || 'SYSTEM';
          const avatar = name.substring(0, 2).toUpperCase();

          const d = new Date(log.actionDate);
          const pad = (n: number) => n.toString().padStart(2, '0');
          const dateStr = isNaN(d.getTime()) ? '-' :
            `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;

          return {
            user: {
              name: name,
              code: name === 'SYSTEM' ? 'Hệ thống' : 'Mã CB: ' + name,
              avatar: avatar
            },
            date: dateStr,
            action: log.action,
            ip: log.ipAddress || '127.0.0.1',
            content: log.description
          };
        });
        this.historyData = mapped;
        this.historyTotalPages = pageData.page?.totalPages ?? pageData.totalPages ?? 1;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        const prefix = this.languageService.labels().messages?.errorPrefix?.history || 'Không thể tải lịch sử thao tác: ';
        this.notificationService.error(prefix + (err.error?.message || err.message));
      }
    });
  }

  onPageChange(page: number) {
    this.historyPage = page;
    this.loadHistoryData();
  }

  getActionPillClass(action: string): string {
    return ACTION_PILL_MAP[action] || 'pill-default';
  }

  onClose() {
    this.close.emit();
  }

  trackByHistoryIndex(index: number, item: MappedHistoryItem): any {
    return index;
  }
}
