import { HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (req.url === '/auth/refresh') {
    req = enhanceRequestWithXsrfToken(req);
    return next(req);
  }

  if (req.url.startsWith('/auth/')) {
    return next(req);
  }

  req = enhanceRequestWithAccessToken(req, authService);
  return next(req).pipe(
    catchError((error) => {
      if (error.status === 401) {
        return authService.refreshToken().pipe(
          switchMap(() => {
            req = enhanceRequestWithAccessToken(req, authService);
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

function enhanceRequestWithAccessToken(
  req: HttpRequest<unknown>,
  authService: AuthService,
): HttpRequest<unknown> {
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

function enhanceRequestWithXsrfToken(req: HttpRequest<unknown>): HttpRequest<unknown> {
  const xsrfToken = findXsrfToken();
  if (xsrfToken.length > 0) {
    req = req.clone({ setHeaders: { 'X-Xsrf-Token': xsrfToken } });
  }
  return req;
}

function findXsrfToken(): string {
  return (
    document.cookie
      .split('; ')
      .find((row) => row.startsWith('redfox_xsrf_token='))
      ?.split('=')[1] || ''
  );
}
