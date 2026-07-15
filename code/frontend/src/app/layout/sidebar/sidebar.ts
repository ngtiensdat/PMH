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

  selectMainMenu(menu: string) {
    this.activeMenu.set(menu);
  }
}
