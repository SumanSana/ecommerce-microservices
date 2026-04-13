import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);


  if (authService.currentUser()?.role == 'ADMIN') {
    return true; // Access Granted
  }

  // 2. Access Denied Logic
  console.error(`Insufficient privileges for: ${state.url}`);

  // Redirect to unauthorized page
  router.navigate(['/unauthorized'], {
    queryParams: { message: 'Insufficient privileges to access admin area' }
  });

  return false; // Access Blocked
};