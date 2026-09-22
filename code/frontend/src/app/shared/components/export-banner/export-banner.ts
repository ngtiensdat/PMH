import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedTaigaModule } from '../../shared-taiga.module';
import { ExportJobResponse, computeExportProgress } from '../../models/transaction-log.model';

@Component({
  selector: 'app-export-progress-banner',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule],
  templateUrl: './export-banner.html',
  styleUrl: './export-banner.css'
})
export class ExportProgressBannerComponent {
  @Input() activeJob: ExportJobResponse | null = null;
  @Input() exportError: string | null = null;
  @Input() moduleName: string = 'dữ liệu';

  @Output() download = new EventEmitter<ExportJobResponse>();

  get progress(): number {
    return this.activeJob ? computeExportProgress(this.activeJob) : 0;
  }

  get isExporting(): boolean {
    const s = this.activeJob?.status;
    return s === 'PENDING' || s === 'PROCESSING';
  }

  get isCompleted(): boolean {
    const s = this.activeJob?.status;
    return s === 'SUCCESS' || s === 'COMPLETED' || s === 'DONE';
  }

  get isFailed(): boolean {
    return this.activeJob?.status === 'FAILED';
  }

  get isExpired(): boolean {
    if (!this.activeJob?.expiresAt) return false;
    return new Date(this.activeJob.expiresAt) < new Date();
  }

  onDownloadClick(): void {
    if (this.activeJob) {
      this.download.emit(this.activeJob);
    }
  }
}
