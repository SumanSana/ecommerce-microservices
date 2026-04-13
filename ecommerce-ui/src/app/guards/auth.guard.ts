import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true; // Door is open
  }

  // Redirect to login and save the URL they were trying to reach
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};