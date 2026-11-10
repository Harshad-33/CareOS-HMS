import { Routes } from '@angular/router';
import { AppShell } from './core/layout/app-shell';

export const routes: Routes = [
  {
    path: '',
    component: AppShell,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'dashboard',
        data: { breadcrumb: 'Workspace Overview' },
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
      },
      {
        path: 'hospital',
        data: { breadcrumb: 'Hospital Profile' },
        loadChildren: () =>
          import('./features/hospital/hospital.routes').then((m) => m.HOSPITAL_ROUTES),
      },
      {
        path: 'users',
        data: { breadcrumb: 'Users' },
        loadChildren: () =>
          import('./features/users/users.routes').then((m) => m.USERS_ROUTES),
      },
      {
        path: 'employees',
        data: { breadcrumb: 'Employees' },
        loadChildren: () =>
          import('./features/employees/employees.routes').then((m) => m.EMPLOYEES_ROUTES),
      },
      {
        path: 'patients',
        data: { breadcrumb: 'Patients' },
        loadChildren: () =>
          import('./features/patients/patients.routes').then((m) => m.PATIENTS_ROUTES),
      },
      {
        path: 'appointments',
        data: { breadcrumb: 'Appointments' },
        loadChildren: () =>
          import('./features/appointments/appointments.routes').then((m) => m.APPOINTMENTS_ROUTES),
      },
      {
        path: 'doctors',
        data: { breadcrumb: 'Doctors' },
        loadChildren: () =>
          import('./features/doctors/doctors.routes').then((m) => m.DOCTORS_ROUTES),
      },
      {
        path: 'admissions',
        data: { breadcrumb: 'Admissions' },
        loadChildren: () =>
          import('./features/admissions/admissions.routes').then((m) => m.ADMISSIONS_ROUTES),
      },
      {
        path: 'clinical',
        data: { breadcrumb: 'Clinical / EMR' },
        loadChildren: () =>
          import('./features/clinical/clinical.routes').then((m) => m.CLINICAL_ROUTES),
      },
      {
        path: 'laboratory',
        data: { breadcrumb: 'Laboratory' },
        loadChildren: () =>
          import('./features/laboratory/laboratory.routes').then((m) => m.LABORATORY_ROUTES),
      },
      {
        path: 'pharmacy',
        data: { breadcrumb: 'Pharmacy' },
        loadChildren: () =>
          import('./features/pharmacy/pharmacy.routes').then((m) => m.PHARMACY_ROUTES),
      },
      {
        path: 'inventory',
        data: { breadcrumb: 'Inventory' },
        loadChildren: () =>
          import('./features/inventory/inventory.routes').then((m) => m.INVENTORY_ROUTES),
      },
      {
        path: 'billing',
        data: { breadcrumb: 'Billing' },
        loadChildren: () =>
          import('./features/billing/billing.routes').then((m) => m.BILLING_ROUTES),
      },
      {
        path: 'reports',
        data: { breadcrumb: 'Reports' },
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
