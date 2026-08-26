import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LanguageService } from '../../core/services/language.service';
import { AuthService } from '../../core/services/auth.service';
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
  public authService = inject(AuthService);
  private notificationService = inject(NotificationService);

  protected readonly notificationCount = signal(0);
  protected isLangDropdownOpen = signal(false);
  protected isRoleDropdownOpen = signal(false);

  toggleLangDropdown() {
    this.isLangDropdownOpen.update(v => !v);
    this.isRoleDropdownOpen.set(false);
  }

  toggleRoleDropdown() {
    if (this.authService.hasMultipleRoles()) {
      this.isRoleDropdownOpen.update(v => !v);
      this.isLangDropdownOpen.set(false);
    }
  }

  switchRole(role: string) {
    this.authService.setActiveRole(role);
    this.isRoleDropdownOpen.set(false);
    this.notificationService.info(`Đã chuyển sang vai trò: ${role === 'MAKER' ? 'Chuyên viên (Maker)' : 'Kiểm soát viên (Checker)'}`);
  }

  selectLanguage(lang: 'VIE' | 'EN') {
    this.languageService.setLanguage(lang);
    this.isLangDropdownOpen.set(false);
  }

  logout() {
    this.notificationService.success(this.languageService.labels().messages.success.logout);
    this.authService.logout();
  }
}
