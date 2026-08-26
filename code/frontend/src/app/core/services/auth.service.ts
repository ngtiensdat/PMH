import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LanguageService } from './language.service';
import { environment } from '../../../environments/environment';

export interface UserResponse {
  token: string;
  username: string;
  fullName: string;
  role: string;
  roles?: string[];
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private languageService = inject(LanguageService);

  private readonly USER_KEY = 'pmh_user_info';
  private readonly ACTIVE_ROLE_KEY = 'pmh_active_role';

  currentUser = signal<UserResponse | null>(this.getStoredUser());
  isLoggedIn = computed(() => !!this.currentUser());

  activeRole = signal<string>(this.getStoredActiveRole());
  hasMultipleRoles = computed(() => (this.currentUser()?.roles?.length ?? 0) > 1);
  userRoles = computed(() => this.currentUser()?.roles || (this.currentUser()?.role ? [this.currentUser()!.role] : []));
  userRole = computed(() => this.activeRole() || this.currentUser()?.role || '');

  isMaker = computed(() => (this.activeRole() || '').toUpperCase() === 'MAKER');
  isChecker = computed(() => (this.activeRole() || '').toUpperCase() === 'CHECKER');

  constructor() {
    const user = this.currentUser();
    if (user) {
      this.languageService.updateUserProfile(user.fullName, this.activeRole(), user.username);
    }
    this.checkAuthStatus();
  }

  setActiveRole(role: string): void {
    const user = this.currentUser();
    if (!user) return;
    localStorage.setItem(this.ACTIVE_ROLE_KEY, role);
    this.activeRole.set(role);
    this.languageService.updateUserProfile(user.fullName, role, user.username);
  }

  checkAuthStatus(): void {
    if (!this.getStoredUser()) return;

    this.http.get<ApiResponse<UserResponse>>(`${environment.apiBase}/api/auth/me`, { withCredentials: true }).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.setSession(res.data);
        }
      },
      error: (err) => {
        if (err.status === 401) {
          this.refreshToken().subscribe({
            error: () => this.clearSession()
          });
        }
      }
    });
  }

  login(credentials: { username: string; password: string }): Observable<ApiResponse<UserResponse>> {
    return this.http.post<ApiResponse<UserResponse>>(`${environment.apiBase}/api/auth/login`, credentials, { withCredentials: true }).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.setSession(res.data);
        }
      })
    );
  }

  refreshToken(): Observable<ApiResponse<UserResponse>> {
    return this.http.post<ApiResponse<UserResponse>>(`${environment.apiBase}/api/auth/refresh`, {}, { withCredentials: true }).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.setSession(res.data);
        }
      })
    );
  }

  logout() {
    this.http.post(`${environment.apiBase}/api/auth/logout`, {}, { withCredentials: true }).subscribe({
      next: () => this.clearSessionAndRedirect(),
      error: () => this.clearSessionAndRedirect()
    });
  }

  getToken(): string | null {
    return null; // Cookie HttpOnly tự động được trình duyệt đính kèm an toàn
  }

  private setSession(userData: UserResponse) {
    localStorage.setItem(this.USER_KEY, JSON.stringify(userData));
    this.currentUser.set(userData);

    const roles = userData.roles && userData.roles.length > 0 ? userData.roles : [userData.role];
    let initialRole = localStorage.getItem(this.ACTIVE_ROLE_KEY);
    if (!initialRole || !roles.includes(initialRole)) {
      initialRole = roles[0] || userData.role || 'MAKER';
      localStorage.setItem(this.ACTIVE_ROLE_KEY, initialRole);
    }

    this.activeRole.set(initialRole);
    this.languageService.updateUserProfile(userData.fullName, initialRole, userData.username);
  }

  private clearSessionAndRedirect() {
    this.clearSession();
    this.router.navigate(['/login']);
  }

  private clearSession() {
    localStorage.removeItem(this.USER_KEY);
    localStorage.removeItem(this.ACTIVE_ROLE_KEY);
    this.currentUser.set(null);
    this.activeRole.set('MAKER');
    this.languageService.userCode.set('make');
  }

  private getStoredUser(): UserResponse | null {
    const json = localStorage.getItem(this.USER_KEY);
    if (!json) return null;
    try {
      return JSON.parse(json);
    } catch (e) {
      return null;
    }
  }

  private getStoredActiveRole(): string {
    const user = this.getStoredUser();
    if (!user) return 'MAKER';
    const storedRole = localStorage.getItem(this.ACTIVE_ROLE_KEY);
    const roles = user.roles && user.roles.length > 0 ? user.roles : [user.role];
    if (storedRole && roles.includes(storedRole)) {
      return storedRole;
    }
    return roles[0] || user.role || 'MAKER';
  }
}
