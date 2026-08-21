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

  private readonly TOKEN_KEY = 'pmh_jwt_token';
  private readonly USER_KEY = 'pmh_user_info';

  currentUser = signal<UserResponse | null>(this.getStoredUser());
  isLoggedIn = computed(() => !!this.currentUser());
  userRole = computed(() => this.currentUser()?.role || '');

  constructor() {
    const user = this.currentUser();
    if (user) {
      this.languageService.updateUserProfile(user.fullName, user.role, user.username);
    }
    this.checkAuthStatus();
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
        if (err.status === 401 || err.status === 403) {
          this.clearSession();
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

  logout() {
    this.http.post(`${environment.apiBase}/api/auth/logout`, {}, { withCredentials: true }).subscribe({
      next: () => this.clearSessionAndRedirect(),
      error: () => this.clearSessionAndRedirect()
    });
  }

  getToken(): string | null {
    return null; // Token is stored safely in HttpOnly cookie
  }

  private setSession(userData: UserResponse) {
    localStorage.setItem(this.USER_KEY, JSON.stringify(userData));
    this.currentUser.set(userData);
    this.languageService.updateUserProfile(userData.fullName, userData.role, userData.username);
  }

  private clearSessionAndRedirect() {
    this.clearSession();
    this.router.navigate(['/login']);
  }

  private clearSession() {
    localStorage.removeItem(this.USER_KEY);
    this.currentUser.set(null);
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
}
