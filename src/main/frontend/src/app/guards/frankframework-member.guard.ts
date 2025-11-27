import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const FrankFrameworkMemberGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.currentUser();

  if (user && user.isFrankFrameworkMember) {
    return true;
  }

  router.navigate(['/']);
  return false;
};
