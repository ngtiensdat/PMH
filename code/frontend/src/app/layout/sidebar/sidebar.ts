import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { LanguageService } from '../../core/services/language.service';
import { SharedTaigaModule } from '../../shared/shared-taiga.module';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, SharedTaigaModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class SidebarComponent {
  public languageService = inject(LanguageService);
  public activeMenu = signal<string>('params');
  public isSubMenuOpen = signal<boolean>(true);

  selectMainMenu(menu: string) {
    if (menu === 'params') {
      if (this.activeMenu() === 'params') {
        // Bấm lại vào Tham số thì toggle ẩn/hiện thanh bên phụ
        this.isSubMenuOpen.update(open => !open);
      } else {
        this.activeMenu.set('params');
        this.isSubMenuOpen.set(true);
      }
    } else {
      this.activeMenu.set(menu);
      this.isSubMenuOpen.set(false);
    }
  }

  closeSubMenu() {
    this.isSubMenuOpen.set(false);
  }
}
