import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SharedTaigaModule } from '../../shared-taiga.module';

@Component({
  selector: 'app-reject-reason-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, SharedTaigaModule],
  templateUrl: './reject-reason-dialog.html'
})
export class RejectReasonDialogComponent {
  @Input() isOpen = false;
  @Input() targetCount = 1;
  reason = '';

  @Output() confirm = new EventEmitter<string>();
  @Output() cancel = new EventEmitter<void>();

  onConfirm() {
    this.confirm.emit(this.reason);
    this.reason = '';
  }

  onCancel() {
    this.cancel.emit();
    this.reason = '';
  }
}
