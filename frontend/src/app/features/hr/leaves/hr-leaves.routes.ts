import { inject } from '@angular/core';
import { CanActivateFn, Router, Routes } from '@angular/router';
import { PERMISSIONS } from '../../../core/constants/permissions.constants';
import { permissionGuard } from '../../../core/guards/auth.guard';
import { AuthStateService } from '../../../core/services/auth-state.service';

const leaveEntryRedirectGuard: CanActivateFn = () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (
    authState.hasAnyPermission([
      PERMISSIONS.HOSPITAL_LEAVES_READ,
      PERMISSIONS.HOSPITAL_LEAVES_MANAGE,
    ])
  ) {
    return router.createUrlTree(['/hr/leaves/dashboard']);
  }

  if (authState.hasPermission(PERMISSIONS.HOSPITAL_LEAVES_APPROVE)) {
    return router.createUrlTree(['/hr/leaves/inbox']);
  }

  return router.createUrlTree(['/unauthorized']);
};

export const HR_LEAVES_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [leaveEntryRedirectGuard],
    children: [],
  },
  {
    path: 'dashboard',
    canActivate: [permissionGuard],
    data: {
      breadcrumb: 'Leave Dashboard',
      requiredScope: 'HOSPITAL',
      requiredPermissions: [
        PERMISSIONS.HOSPITAL_LEAVES_READ,
        PERMISSIONS.HOSPITAL_LEAVES_MANAGE,
      ],
    },
    loadComponent: () =>
      import('./components/leave-dashboard/leave-dashboard.component').then(
        (m) => m.LeaveDashboardComponent
      ),
  },
  {
    path: 'types',
    canActivate: [permissionGuard],
    data: {
      breadcrumb: 'Leave Policies',
      requiredScope: 'HOSPITAL',
      requiredPermissions: [
        PERMISSIONS.HOSPITAL_LEAVES_READ,
        PERMISSIONS.HOSPITAL_LEAVES_MANAGE,
      ],
    },
    loadComponent: () =>
      import('./components/leave-type-list/leave-type-list.component').then(
        (m) => m.LeaveTypeListComponent
      ),
  },
  {
    path: 'balances',
    canActivate: [permissionGuard],
    data: {
      breadcrumb: 'Balance Management',
      requiredScope: 'HOSPITAL',
      requiredPermissions: [
        PERMISSIONS.HOSPITAL_LEAVES_READ,
        PERMISSIONS.HOSPITAL_LEAVES_MANAGE,
      ],
    },
    loadComponent: () =>
      import('./components/leave-balance-list/leave-balance-list.component').then(
        (m) => m.LeaveBalanceListComponent
      ),
  },
  {
    path: 'inbox',
    canActivate: [permissionGuard],
    data: {
      breadcrumb: 'Approval Inbox',
      requiredScope: 'HOSPITAL',
      requiredPermissions: [PERMISSIONS.HOSPITAL_LEAVES_APPROVE],
    },
    loadComponent: () =>
      import('./components/approval-inbox/approval-inbox.component').then(
        (m) => m.ApprovalInboxComponent
      ),
  },
  {
    path: 'calendar',
    canActivate: [permissionGuard],
    data: {
      breadcrumb: 'Leave Calendar',
      requiredScope: 'HOSPITAL',
      requiredPermissions: [
        PERMISSIONS.HOSPITAL_LEAVES_READ,
        PERMISSIONS.HOSPITAL_LEAVES_MANAGE,
      ],
    },
    loadComponent: () =>
      import('./components/leave-calendar/leave-calendar.component').then(
        (m) => m.LeaveCalendarComponent
      ),
  },
];
