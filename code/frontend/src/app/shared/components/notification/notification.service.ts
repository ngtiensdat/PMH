import { Injectable, inject } from '@angular/core';
import { TuiNotificationService } from '@taiga-ui/core';

@Injectable({
providedIn: 'root'
})
export class NotificationService {
private readonly alerts = inject(TuiNotificationService);

success(message: string, title = 'Thành công', actionPath?: string, duration?: number) {
this.alerts.open(message, { label: title, appearance: 'success', block: 'end', inline: 'start' } as any).subscribe();
}

error(message: string, title = 'Lỗi', actionPath?: string, duration?: number) {
this.alerts.open(message, { label: title, appearance: 'error', block: 'end', inline: 'start' } as any).subscribe();
}

warning(message: string, title = 'Cảnh báo', actionPath?: string, duration?: number) {
this.alerts.open(message, { label: title, appearance: 'warning', block: 'end', inline: 'start' } as any).subscribe();
}

info(message: string, title = 'Thông tin', actionPath?: string, duration?: number) {
this.alerts.open(message, { label: title, appearance: 'info', block: 'end', inline: 'start' } as any).subscribe();
}
}
