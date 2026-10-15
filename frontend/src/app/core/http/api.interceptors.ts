import { HttpErrorResponse, HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessionStore } from '../auth/session.store';
import { isSuspended } from './api-error';

const API_URL = '/api/';

/** API classes use relative paths ('sales'); this prefixes the backend base (proxied in dev). */
export const apiUrlInterceptor: HttpInterceptorFn = (req, next) => {
  if (/^(https?:)?\/\//.test(req.url) || req.url.startsWith('/')) return next(req);
  return next(req.clone({ url: API_URL + req.url }));
};

/** Platform token for /api/platform/**, business token otherwise. 401 / suspended → clear that session and go to its login. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(SessionStore);
  const router = inject(Router);
  const platform = /(^|\/)platform\//.test(req.url);
  const isLogin = /(^|\/)auth\/login$/.test(req.url);
  const token = isLogin ? null : platform ? session.platform()?.token : session.business()?.token;
  const authReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((error: unknown) => {
      if (token && error instanceof HttpErrorResponse) {
        if (error.status === HttpStatusCode.Unauthorized)
          void router.navigateByUrl(session.endSession(platform, 'expired', router.url));
        else if (isSuspended(error))
          void router.navigateByUrl(session.endSession(platform, 'suspended'));
      }
      return throwError(() => error);
    }),
  );
};
