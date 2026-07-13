import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Tự động đính kèm X-Username vào header nếu chưa có
  const username = req.headers.get('X-Username') || 'USER01';
  
  const authReq = req.clone({
    headers: req.headers.set('X-Username', username)
  });

  return next(authReq);
};
