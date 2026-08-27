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
      // 1. Nếu gặp lỗi 401 Unauthorized ở API thường (Phiên làm việc hết hạn)
      if (error.status === 401 && !req.url.includes('/api/auth/login') && !req.url.includes('/api/auth/refresh')) {
        // Tự động gia hạn Access Token ngầm
        return authService.refreshToken().pipe(
          switchMap(() => {
            return next(req.clone({ withCredentials: true }));
          }),
          catchError(refreshErr => {
            // Gia hạn thất bại -> Đăng xuất về màn /login
            authService.logout();
            return throwError(() => refreshErr);
          })
        );
      }

      // 2. Với các lỗi khác (bao gồm 403 Forbidden): Giữ nguyên phiên đăng nhập, để Component tự hiển thị 1 thông báo lỗi duy nhất
      return throwError(() => error);
    })
  );
};
