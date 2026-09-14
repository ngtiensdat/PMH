import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedTaigaModule } from '../../shared-taiga.module';
import { AuthService } from '../../../core/services/auth.service';
import { LanguageService } from '../../../core/services/language.service';
import { ParamStatus, DisplayStatus } from '../../enums/status.enum';

@Component({
  selector: 'app-detail-footer-actions',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule],
  templateUrl: './detail-footer-actions.component.html',
})
export class DetailFooterActionsComponent {

  // ── 1. CẤU HÌNH ĐẦU VÀO (Dữ liệu từ trang cha truyền xuống) ────────────────
  @Input() permissionPrefix = '';
  @Input() entityStatus?: ParamStatus | number;
  @Input() entityIsDisplay?: DisplayStatus | number;
  @Input() hasNewData = false;

  // ── 2. BÁO ĐỘNG ĐẦU RA (Gửi sự kiện bấm nút về trang cha) ──────────────────
  @Output() confirmDelete = new EventEmitter<void>();
  @Output() confirmSendApproval = new EventEmitter<void>();
  @Output() confirmApprove = new EventEmitter<void>();
  @Output() confirmReject = new EventEmitter<string>();
  @Output() back = new EventEmitter<void>();

  // ── 3. QUẢN LÝ POPUP NỘI BỘ (Ẩn/Hiện & dữ liệu 3 Hộp thoại Dialog) ─────────
  isDeleteOpen = false;   // Popup xác nhận xóa
  isApproveOpen = false;  // Popup xác nhận duyệt
  isRejectOpen = false;   // Popup nhập lý do từ chối
  rejectReason = '';      // Ô nhập lý do từ chối

  // ── 4. KHAI BÁO DỊCH VỤ & ENUM HỖ TRỢ ──────────────────────────────────────
  readonly authService = inject(AuthService);
  readonly languageService = inject(LanguageService);
  readonly ParamStatus = ParamStatus;
  readonly DisplayStatus = DisplayStatus;

  // ── 5. XỬ LÝ PHÂN QUYỀN NÚT BẤM ────────────────────────────────────────────
  can(action: string): boolean {
    return this.authService.hasPermission(`${this.permissionPrefix}_${action}`);
  }

  // ── 6. XỬ LÝ SỰ KIỆN POPUP (Đóng popup & bắn tín hiệu lên cha) ─────────────
  
  // Xử lý Popup Xóa
  onConfirmDelete(): void {
    this.isDeleteOpen = false;
    this.confirmDelete.emit();
  }

  // Xử lý Popup Duyệt
  onConfirmApprove(): void {
    this.isApproveOpen = false;
    this.confirmApprove.emit();
  }

  // Xử lý Mở Popup Từ chối
  openRejectDialog(): void {
    this.rejectReason = '';
    this.isRejectOpen = true;
  }

  // Xử lý Popup Từ chối (gửi kèm lý do)
  onConfirmReject(): void {
    const reason = this.rejectReason.trim();
    this.isRejectOpen = false;
    this.rejectReason = '';
    this.confirmReject.emit(reason);
  }
}
