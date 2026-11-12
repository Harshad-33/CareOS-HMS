import { Routes } from '@angular/router';
import { PERMISSIONS } from './core/constants/permissions.constants';
import { authGuard, guestGuard, permissionGuard } from './core/guards/auth.guard';
import { AppShell } from './core/layout/app-shell';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login').then((m) => m.LoginComponent),
  },
  {
    path: '',
    component: AppShell,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'dashboard',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Workspace Overview',
          requiredPermissions: [PERMISSIONS.SYSTEM_HEALTH_VIEW],
        },
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
      },
      {
        path: 'unauthorized',
        data: { breadcrumb: 'Unauthorized' },
        loadComponent: () =>
          import('./features/auth/unauthorized/unauthorized').then((m) => m.UnauthorizedComponent),
      },
      {
        path: 'hospital',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Hospital Profile',
          requiredPermissions: [
            PERMISSIONS.PLATFORM_HOSPITALS_MANAGE,
            PERMISSIONS.HOSPITAL_WORKSPACE_READ,
          ],
        },
        loadChildren: () =>
          import('./features/hospital/hospital.routes').then((m) => m.HOSPITAL_ROUTES),
      },
      {
        path: 'users',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Users',
          requiredPermissions: [PERMISSIONS.HOSPITAL_USERS_MANAGE],
        },
        loadChildren: () =>
          import('./features/users/users.routes').then((m) => m.USERS_ROUTES),
      },
      {
        path: 'employees',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Employees',
          requiredPermissions: [PERMISSIONS.HOSPITAL_USERS_MANAGE],
        },
        loadChildren: () =>
          import('./features/employees/employees.routes').then((m) => m.EMPLOYEES_ROUTES),
      },
      {
        path: 'patients',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Patients',
          requiredPermissions: [PERMISSIONS.HOSPITAL_WORKSPACE_READ],
        },
        loadChildren: () =>
          import('./features/patients/patients.routes').then((m) => m.PATIENTS_ROUTES),
      },
      {
        path: 'appointments',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Appointments',
          requiredPermissions: [PERMISSIONS.HOSPITAL_WORKSPACE_READ],
        },
        loadChildren: () =>
          import('./features/appointments/appointments.routes').then((m) => m.APPOINTMENTS_ROUTES),
      },
      {
        path: 'doctors',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Doctors',
          requiredPermissions: [PERMISSIONS.HOSPITAL_WORKSPACE_READ],
        },
        loadChildren: () =>
          import('./features/doctors/doctors.routes').then((m) => m.DOCTORS_ROUTES),
      },
      {
        path: 'admissions',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Admissions',
          requiredPermissions: [PERMISSIONS.HOSPITAL_WORKSPACE_READ],
        },
        loadChildren: () =>
          import('./features/admissions/admissions.routes').then((m) => m.ADMISSIONS_ROUTES),
      },
      {
        path: 'clinical',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Clinical / EMR',
          requiredPermissions: [PERMISSIONS.HOSPITAL_WORKSPACE_READ],
        },
        loadChildren: () =>
          import('./features/clinical/clinical.routes').then((m) => m.CLINICAL_ROUTES),
      },
      {
        path: 'laboratory',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Laboratory',
          requiredPermissions: [PERMISSIONS.HOSPITAL_WORKSPACE_READ],
        },
        loadChildren: () =>
          import('./features/laboratory/laboratory.routes').then((m) => m.LABORATORY_ROUTES),
      },
      {
        path: 'pharmacy',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Pharmacy',
          requiredPermissions: [PERMISSIONS.HOSPITAL_WORKSPACE_READ],
        },
        loadChildren: () =>
          import('./features/pharmacy/pharmacy.routes').then((m) => m.PHARMACY_ROUTES),
      },
      {
        path: 'inventory',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Inventory',
          requiredPermissions: [PERMISSIONS.HOSPITAL_WORKSPACE_READ],
        },
        loadChildren: () =>
          import('./features/inventory/inventory.routes').then((m) => m.INVENTORY_ROUTES),
      },
      {
        path: 'billing',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Billing',
          requiredPermissions: [PERMISSIONS.HOSPITAL_WORKSPACE_READ],
        },
        loadChildren: () =>
          import('./features/billing/billing.routes').then((m) => m.BILLING_ROUTES),
      },
      {
        path: 'reports',
        canActivate: [permissionGuard],
        data: {
          breadcrumb: 'Reports',
          requiredPermissions: [
            PERMISSIONS.HOSPITAL_WORKSPACE_READ,
            PERMISSIONS.PLATFORM_AUDIT_READ,
          ],
        },
        loadChildren: () =>
          import('./features/reports/reports.routes').then((m) => m.REPORTS_ROUTES),
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
