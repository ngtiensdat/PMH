import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LanguageService } from '../../core/services/language.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../shared/components/notification/notification.service';
import { SharedTaigaModule } from '../../shared/shared-taiga.module';
import { TransactionLogService } from '../../features/transaction-log/services/transaction-log.service';
import { ExportJobResponse } from '../../shared/models/transaction-log.model';
import { catchError, EMPTY } from 'rxjs';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule],
  templateUrl: './header.html',
  styleUrl: './header.css'
})
export class HeaderComponent implements OnInit {
  public languageService  = inject(LanguageService);
  public authService      = inject(AuthService);
  private notificationSvc = inject(NotificationService);
  private txnService      = inject(TransactionLogService);

  protected isLangDropdownOpen   = signal(false);
  protected isRoleDropdownOpen   = signal(false);

  // ── Notification Bell (6.3) ────────────────────────────────────────────────
  protected isBellOpen        = signal(false);
  protected myJobs            = signal<ExportJobResponse[]>([]);
  protected completedJobCount = signal(0);

  ngOnInit(): void {
    // Tải danh sách jobs khi Header init (bao gồm sau F5)
    this.loadMyJobs();
  }

  /** Tải lịch sử jobs — gọi khi mở Bell hoặc khi init */
  loadMyJobs(): void {
    this.txnService.getMyJobs().pipe(
      catchError(() => EMPTY)
    ).subscribe(res => {
      const jobs = res.data || [];
      this.myJobs.set(jobs);
      // Đếm số file CHƯA TẢI (downloadCount === 0 / isRead === false) và còn hạn
      const now = new Date();
      this.completedJobCount.set(
        jobs.filter(j => (j.status === 'SUCCESS' || j.status === 'COMPLETED' || j.status === 'DONE') 
                     && (!j.downloadCount || j.downloadCount === 0) 
                     && !j.isFileDeleted 
                     && j.expiresAt && new Date(j.expiresAt) > now).length
      );
    });
  }

  toggleBell(): void {
    this.isBellOpen.update(v => !v);
    if (this.isBellOpen()) {
      this.loadMyJobs(); // Refresh khi mở
    }
    this.isLangDropdownOpen.set(false);
    this.isRoleDropdownOpen.set(false);
  }

  /** Tải file từ Bell — jobId là Long (number) */
  downloadFromBell(job: ExportJobResponse): void {
    const isFirstDownload = (!job.downloadCount || job.downloadCount === 0);
    this.txnService.getDownloadUrl(job.jobId).subscribe({
      next: (res) => {
        let url = res.data;
        if (url && url.startsWith('/')) {
          url = environment.apiBase + url;
        }
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', job.fileName || `transaction_log_${job.jobId}.xlsx`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        if (isFirstDownload) {
          this.notificationSvc.info('Tải file thành công! Để bảo mật và tiết kiệm tài nguyên, file sẽ tự động xóa khỏi máy chủ sau 30 phút.');
        } else {
          this.notificationSvc.success('Đang tải lại file...');
        }
        // Refresh lại danh sách để tắt chấm đỏ badge
        this.loadMyJobs();
      },
      error: (err) => {
        const msg = err?.error?.message || 'Không thể tải file. Vui lòng thử lại.';
        this.notificationSvc.error(msg);
        this.loadMyJobs();
      }
    });
  }

  isExpired(job: ExportJobResponse): boolean {
    if (!job.expiresAt) return false;
    return new Date(job.expiresAt) < new Date();
  }

  /** Thời gian còn lại đến khi hết hạn (HH:mm) */
  getTimeRemaining(expiresAt: string | null): string {
    if (!expiresAt) return '';
    const diff = new Date(expiresAt).getTime() - Date.now();
    if (diff <= 0) return 'Đã hết hạn';
    const h = Math.floor(diff / 3600000);
    const m = Math.floor((diff % 3600000) / 60000);
    return `Còn ${h}g ${m.toString().padStart(2, '0')}p`;
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'Đang chờ', PROCESSING: 'Đang xử lý',
      SUCCESS: 'Hoàn thành', COMPLETED: 'Hoàn thành', DONE: 'Hoàn thành', FAILED: 'Thất bại'
    };
    return map[status] || status;
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'bell-job-pending', PROCESSING: 'bell-job-processing',
      SUCCESS: 'bell-job-completed', COMPLETED: 'bell-job-completed', DONE: 'bell-job-completed', FAILED: 'bell-job-failed'
    };
    return map[status] || '';
  }

  // ── Existing methods ───────────────────────────────────────────────────────
  toggleLangDropdown() {
    this.isLangDropdownOpen.update(v => !v);
    this.isRoleDropdownOpen.set(false);
    this.isBellOpen.set(false);
  }

  toggleRoleDropdown() {
    if (this.authService.hasMultipleRoles()) {
      this.isRoleDropdownOpen.update(v => !v);
      this.isLangDropdownOpen.set(false);
      this.isBellOpen.set(false);
    }
  }

  getRoleDisplayLabel(roleCode: string): string {
    if (!roleCode) return '';
    const cleanCode = roleCode.replace(/^ROLE_/i, '').trim();
    if (!cleanCode) return roleCode;
    return cleanCode.split('_').filter(Boolean)
      .map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join(' ');
  }

  switchRole(role: string) {
    this.authService.setActiveRole(role);
    this.isRoleDropdownOpen.set(false);
    this.notificationSvc.info(`Đã chuyển sang vai trò: ${this.getRoleDisplayLabel(role)}`);
  }

  selectLanguage(lang: 'VIE' | 'EN') {
    this.languageService.setLanguage(lang);
    this.isLangDropdownOpen.set(false);
  }

  logout() {
    this.notificationSvc.success(this.languageService.labels().messages.success.logout);
    this.authService.logout();
  }
}
