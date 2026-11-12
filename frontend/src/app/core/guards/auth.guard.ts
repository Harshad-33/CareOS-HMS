import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserScope } from '../models/auth.model';
import { AuthStateService } from '../services/auth-state.service';

export interface ProtectedRouteData {
  requiredPermissions?: readonly string[];
  requiredScope?: UserScope;
}

export const authGuard: CanActivateFn = (_route, state) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (authState.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: state.url && state.url !== '/' ? { returnUrl: state.url } : undefined,
  });
};

export const guestGuard: CanActivateFn = () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (authState.isAuthenticated()) {
    return router.createUrlTree(['/dashboard']);
  }

  return true;
};

export const permissionGuard: CanActivateFn = (route) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (!authState.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const data = route.data as ProtectedRouteData;
  if (authState.canAccess(data.requiredPermissions, data.requiredScope)) {
    return true;
  }

  return router.createUrlTree(['/unauthorized']);
};
