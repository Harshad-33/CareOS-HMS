import { Routes } from '@angular/router';
import { PERMISSIONS } from '../../core/constants/permissions.constants';
import { permissionGuard } from '../../core/guards/auth.guard';
import { EmployeeDirectoryComponent } from './components/employee-directory/employee-directory.component';
import { EmployeeProfileComponent } from './components/employee-profile/employee-profile.component';
import { EmployeeRegistrationComponent } from './components/employee-registration/employee-registration.component';

export const EMPLOYEES_ROUTES: Routes = [
  {
    path: '',
    component: EmployeeDirectoryComponent,
    canActivate: [permissionGuard],
    title: 'Employee Management | CareOS-HMS',
    data: {
      breadcrumb: 'Employees',
      requiredPermissions: [
        PERMISSIONS.HOSPITAL_EMPLOYEES_READ,
        PERMISSIONS.PLATFORM_EMPLOYEES_READ,
      ],
    },
  },
  {
    path: 'register',
    component: EmployeeRegistrationComponent,
    canActivate: [permissionGuard],
    title: 'Register Employee | CareOS-HMS',
    data: {
      breadcrumb: 'Register Employee',
      requiredPermissions: [PERMISSIONS.HOSPITAL_EMPLOYEES_MANAGE],
    },
  },
  {
    path: ':publicId',
    component: EmployeeProfileComponent,
    canActivate: [permissionGuard],
    title: 'Employee Profile | CareOS-HMS',
    data: {
      breadcrumb: 'Employee Profile',
      requiredPermissions: [
        PERMISSIONS.HOSPITAL_EMPLOYEES_READ,
        PERMISSIONS.PLATFORM_EMPLOYEES_READ,
      ],
    },
  },
];
