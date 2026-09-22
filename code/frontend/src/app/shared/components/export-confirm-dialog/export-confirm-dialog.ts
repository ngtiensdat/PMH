import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedTaigaModule } from '../../shared-taiga.module';

@Component({
  selector: 'app-export-confirm-dialog',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule],
  templateUrl: './export-confirm-dialog.html',
  styleUrl: './export-confirm-dialog.css'
})
export class ExportConfirmDialogComponent {
  @Input() isOpen = false;
  @Input() title = 'Xác nhận xuất file';
  @Input() message = '';

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  onConfirm(): void {
    this.confirm.emit();
  }

  onCancel(): void {
    this.cancel.emit();
  }
}
