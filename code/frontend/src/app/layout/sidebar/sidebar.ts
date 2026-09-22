import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { LanguageService } from '../../core/services/language.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../shared/components/notification/notification.service';
import { SharedTaigaModule } from '../../shared/shared-taiga.module';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, SharedTaigaModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class SidebarComponent implements OnInit {
  public languageService = inject(LanguageService);
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);

  public activeMenu = signal<string>('params');
  public isSubMenuOpen = signal<boolean>(true);

  ngOnInit(): void {
    this.syncActiveMenu(this.router.url);
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe((e: NavigationEnd) => {
      this.syncActiveMenu(e.urlAfterRedirects || e.url);
    });
  }

  private syncActiveMenu(url: string): void {
    if (url.startsWith('/transaction-log')) {
      this.activeMenu.set('transactions');
    } else if (url.startsWith('/components') || url.startsWith('/categories')) {
      this.activeMenu.set('params');
    }
  }

  selectMainMenu(menu: string): void {
    if (menu === 'params') {
      if (this.activeMenu() === 'params') {
        this.isSubMenuOpen.update(open => !open);
      } else {
        this.activeMenu.set('params');
        this.isSubMenuOpen.set(true);
        this.router.navigate(['/components']);
      }
    } else if (menu === 'transactions') {
      if (this.activeMenu() === 'transactions') {
        this.isSubMenuOpen.update(open => !open);
      } else {
        this.activeMenu.set('transactions');
        this.isSubMenuOpen.set(true);
        this.router.navigate(['/transaction-log']);
      }
    }
  }

  closeSubMenu(): void {
    this.isSubMenuOpen.set(false);
  }

  onLogout(): void {
    this.notificationService.success(this.languageService.labels().messages.success.logout);
    this.authService.logout();
  }
}
