import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LanguageService } from '../../core/services/language.service';
import { NotificationService } from '../../shared/components/notification/notification.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.html',
  styleUrl: './header.css'
})
export class HeaderComponent {
  public languageService = inject(LanguageService);
  private notificationService = inject(NotificationService);

  protected readonly notificationCount = signal(5);
  protected isLangDropdownOpen = signal(false);

  toggleLangDropdown() {
    this.isLangDropdownOpen.update(v => !v);
  }

  selectLanguage(lang: 'VIE' | 'EN') {
    this.languageService.setLanguage(lang);
    this.isLangDropdownOpen.set(false);
  }

  editUserProfile() {
    this.notificationService.info('Tính năng chỉnh sửa hồ sơ đang được phát triển.');
  }
}
