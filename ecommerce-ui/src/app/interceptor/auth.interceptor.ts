import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, throwError, BehaviorSubject, filter, take } from 'rxjs';

const isRefreshing$ = new BehaviorSubject<boolean>(false);
const refreshTokenSubject$ = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId) || req.url.includes('/auth/')) {
    return next(req);
  }

  const token = authService.getAccessToken();
  
  let authReq = req;
  if (token && typeof token === 'string' && token.trim() !== '') {
    authReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
    console.log('Is Token Attached?', authReq.headers.get('Authorization'));
  }

  return next(authReq).pipe(
    catchError((error) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !req.url.includes('/auth/refresh')) {
        
        if (authService.isRefreshTokenExpired()) {
          authService.logout();
          return throwError(() => error);
        }

        if (!isRefreshing$.value) {
          isRefreshing$.next(true);
          refreshTokenSubject$.next(null);

          return authService.refreshToken().pipe(
            switchMap((res) => {
              isRefreshing$.next(false);
              refreshTokenSubject$.next(res.access_token);
              return next(req.clone({ headers: req.headers.set('Authorization', `Bearer ${res.access_token}`) }));
            }),
            catchError((err) => {
              isRefreshing$.next(false);
              authService.logout();
              return throwError(() => err);
            })
          );
        } else {
          return refreshTokenSubject$.pipe(
            filter(t => t !== null),
            take(1),
            switchMap((newToken) => next(req.clone({ headers: req.headers.set('Authorization', `Bearer ${newToken}`) })))
          );
        }
      }
      return throwError(() => error);
    })
  );
};