import { Injectable, signal, computed } from '@angular/core';
import { APP_LABELS_VN, APP_LABELS_EN } from '../constants/labels';

@Injectable({
  providedIn: 'root'
})
export class LanguageService {
  // Quản lý ngôn ngữ hiện tại (VIE hoặc EN), lưu trong localStorage để giữ trạng thái khi reload
  currentLang = signal<'VIE' | 'EN'>((localStorage.getItem('app_lang') as 'VIE' | 'EN') || 'VIE');

  // Computed signal tự động tính toán lại Labels khi currentLang thay đổi
  labels = computed(() => {
    return this.currentLang() === 'VIE' ? APP_LABELS_VN : APP_LABELS_EN;
  });

  // Tên người dùng, vai trò và mã người dùng đọc động từ localStorage
  userName = signal<string>(localStorage.getItem('app_username') || '');
  userRole = signal<string>(localStorage.getItem('app_userrole') || '');
  userCode = signal<string>(localStorage.getItem('app_usercode') || 'make');

  avatarText = computed(() => {
    const name = this.userName() || this.labels().navigation.user.name;
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 3) {
      return (parts[0][0] + parts[1][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.substring(0, 3).toUpperCase();
  });

  constructor() {
    if (!this.userName()) {
      this.userName.set(this.labels().navigation.user.name);
    }
    if (!this.userRole()) {
      this.userRole.set(this.labels().navigation.user.role);
    }
    if (!localStorage.getItem('app_usercode')) {
      localStorage.setItem('app_usercode', 'make');
    }
  }

  setLanguage(lang: 'VIE' | 'EN'): void {
    this.currentLang.set(lang);
    localStorage.setItem('app_lang', lang);
  }

  updateUserProfile(fullName: string, role: string, userCode?: string): void {
    if (fullName) {
      this.userName.set(fullName);
      localStorage.setItem('app_username', fullName);
    }
    if (role) {
      this.userRole.set(role);
      localStorage.setItem('app_userrole', role);
    }
    if (userCode) {
      this.userCode.set(userCode);
      localStorage.setItem('app_usercode', userCode);
    }
  }
}
