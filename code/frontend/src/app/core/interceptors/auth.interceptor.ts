import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  let requestToPass = req.clone({
    withCredentials: true
  });

  return next(requestToPass).pipe(
    catchError((error: HttpErrorResponse) => {
      // Nếu gặp lỗi 401 Unauthorized ở API thường (không phải login hay refresh)
      if (error.status === 401 && !req.url.includes('/api/auth/login') && !req.url.includes('/api/auth/refresh')) {
        // Tự động gọi ngầm /api/auth/refresh để lấy Access Token mới
        return authService.refreshToken().pipe(
          switchMap(() => {
            // Gia hạn thành công -> Phát lại request cũ ngầm dưới nền
            return next(req.clone({ withCredentials: true }));
          }),
          catchError(refreshErr => {
            // Gia hạn thất bại (Refresh Token 10h cũng đã hết hạn) -> Đăng xuất về trang /login
            authService.logout();
            return throwError(() => refreshErr);
          })
        );
      }

      if ((error.status === 401 || error.status === 403) && !req.url.includes('/api/auth/')) {
        authService.logout();
      }

      return throwError(() => error);
    })
  );
};
