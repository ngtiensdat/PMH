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
  userCode = signal<string>(localStorage.getItem('app_usercode') || 'USER01');

  avatarText = computed(() => {
    const name = this.userName() || this.labels().navigation.user.name;
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 3) {
      // Ví dụ: Hoàng Văn Thuận -> HVT
      return (parts[0][0] + parts[1][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.substring(0, 3).toUpperCase();
  });

  constructor() {
    // Nếu chưa có tên người dùng trong localStorage thì khởi tạo mặc định theo labels
    if (!this.userName()) {
      this.userName.set(this.labels().navigation.user.name);
    }
    if (!this.userRole()) {
      this.userRole.set(this.labels().navigation.user.role);
    }
    if (!localStorage.getItem('app_usercode')) {
      localStorage.setItem('app_usercode', 'USER01');
    }
  }

  setLanguage(lang: 'VIE' | 'EN') {
    this.currentLang.set(lang);
    localStorage.setItem('app_lang', lang);
    
    // Cập nhật lại tên/vai trò tương ứng nếu chưa từng chỉnh sửa thủ công
    const defaultVnName = APP_LABELS_VN.navigation.user.name;
    const defaultEnName = APP_LABELS_EN.navigation.user.name;
    
    if (this.userName() === defaultVnName || this.userName() === defaultEnName) {
      const newName = lang === 'VIE' ? defaultVnName : defaultEnName;
      this.userName.set(newName);
      localStorage.setItem('app_username', newName);
    }

    const defaultVnRole = APP_LABELS_VN.navigation.user.role;
    const defaultEnRole = APP_LABELS_EN.navigation.user.role;
    if (this.userRole() === defaultVnRole || this.userRole() === defaultEnRole) {
      const newRole = lang === 'VIE' ? defaultVnRole : defaultEnRole;
      this.userRole.set(newRole);
      localStorage.setItem('app_userrole', newRole);
    }
  }

  updateUserProfile(name: string, role: string, code: string) {
    this.userName.set(name);
    this.userRole.set(role);
    this.userCode.set(code);
    localStorage.setItem('app_username', name);
    localStorage.setItem('app_userrole', role);
    localStorage.setItem('app_usercode', code);
  }
}
