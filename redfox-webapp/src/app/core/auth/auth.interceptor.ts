import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (req.url.startsWith('/auth/')) {
    return next(req);
  }

  req = enhanceRequestWithToken(req, authService);

  return next(req).pipe(
    catchError((error) => {
      if (error.status === 401) {
        return authService.refreshToken().pipe(
          switchMap(() => {
            req = enhanceRequestWithToken(req, authService);
            return next(req);
          }),
          catchError(() => {
            router.navigate(['/']);
            return throwError(() => error);
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};

function enhanceRequestWithToken(req: any, authService: AuthService) {
  const token = authService.getAccessTokenValue();
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }
  return req;
}
