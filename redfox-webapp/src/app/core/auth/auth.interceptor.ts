import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (req.url.startsWith('/api/oauth2/')) {
    return next(req);
  }

  return next(req).pipe(
    catchError((error) => {
      if (error.status === 401) {
        return authService.refreshToken().pipe(
          switchMap(() => next(req)),
          catchError(() => {
            sessionStorage.removeItem('authenticated');
            router.navigate(['/']);
            return throwError(() => error);
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};
