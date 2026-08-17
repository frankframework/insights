import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Observable, map } from 'rxjs';
import { AuthService, User } from '../services/auth.service';

export const FrankFrameworkMemberGuard: CanActivateFn = (): boolean | Observable<boolean> => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.currentUser();

  if (user) {
    return allowMemberOrRedirect(user, router);
  }

  return authService.checkAuthStatus().pipe(
    map((fetchedUser) => {
      if (fetchedUser) {
        authService.setAuthenticated(fetchedUser);
      }
      return allowMemberOrRedirect(fetchedUser, router);
    }),
  );
};

function allowMemberOrRedirect(user: User | null, router: Router): boolean {
  if (user?.isFrankFrameworkMember) {
    return true;
  }

  router.navigate(['/']);
  return false;
}
