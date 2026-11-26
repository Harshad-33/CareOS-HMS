import { Routes } from '@angular/router';
import { PERMISSIONS } from '../../../core/constants/permissions.constants';
import { permissionGuard } from '../../../core/guards/auth.guard';

export const HR_LEAVES_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard',
  },
  {
    path: 'dashboard',
    canActivate: [permissionGuard],
    data: {
      breadcrumb: 'Leave Dashboard',
      requiredPermissions: [
        PERMISSIONS.HR_LEAVE_VIEW_ALL,
        PERMISSIONS.HR_LEAVE_APPROVE,
        PERMISSIONS.HOSPITAL_USERS_MANAGE,
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
      requiredPermissions: [
        PERMISSIONS.HR_LEAVE_MANAGE_POLICY,
        PERMISSIONS.HOSPITAL_USERS_MANAGE,
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
      requiredPermissions: [
        PERMISSIONS.HR_LEAVE_MANAGE_BALANCES,
        PERMISSIONS.HOSPITAL_USERS_MANAGE,
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
      requiredPermissions: [
        PERMISSIONS.HR_LEAVE_APPROVE,
        PERMISSIONS.HOSPITAL_USERS_MANAGE,
      ],
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
      requiredPermissions: [
        PERMISSIONS.HR_LEAVE_VIEW_ALL,
        PERMISSIONS.HOSPITAL_WORKSPACE_READ,
      ],
    },
    loadComponent: () =>
      import('./components/leave-calendar/leave-calendar.component').then(
        (m) => m.LeaveCalendarComponent
      ),
  },
];
