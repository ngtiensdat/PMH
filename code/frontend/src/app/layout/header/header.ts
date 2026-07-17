import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LanguageService } from '../../core/services/language.service';
import { NotificationService } from '../../shared/components/notification/notification.service';
import { SharedTaigaModule } from '../../shared/shared-taiga.module';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, SharedTaigaModule],
  templateUrl: './header.html',
  styleUrl: './header.css'
})
export class HeaderComponent {
  public languageService = inject(LanguageService);
  private notificationService = inject(NotificationService);

  protected readonly notificationCount = signal(0);
  protected isLangDropdownOpen = signal(false);
  protected isUserDropdownOpen = signal(false);

  toggleLangDropdown() {
    this.isLangDropdownOpen.update(v => !v);
  }

  toggleUserDropdown() {
    this.isUserDropdownOpen.update(v => !v);
  }

  selectLanguage(lang: 'VIE' | 'EN') {
    this.languageService.setLanguage(lang);
    this.isLangDropdownOpen.set(false);
  }

  selectUser(name: string, role: string, code: string) {
    this.languageService.updateUserProfile(name, role, code);
    this.isUserDropdownOpen.set(false);
    this.notificationService.success('Đã chuyển sang vai trò: ' + role);
    window.location.reload();
  }
}
