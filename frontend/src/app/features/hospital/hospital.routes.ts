import { Routes } from '@angular/router';
import { PERMISSIONS } from '../../core/constants/permissions.constants';
import { permissionGuard } from '../../core/guards/auth.guard';
import { HospitalComponent } from './hospital.component';
import { HospitalRegistrationComponent } from './components/hospital-registration/hospital-registration';

export const HOSPITAL_ROUTES: Routes = [
  {
    path: '',
    component: HospitalComponent,
    title: 'Hospital Management | CareOS-HMS',
  },
  {
    path: 'register',
    component: HospitalRegistrationComponent,
    canActivate: [permissionGuard],
    title: 'Register Hospital | CareOS-HMS',
    data: {
      breadcrumb: 'Register Hospital',
      requiredPermissions: [PERMISSIONS.PLATFORM_HOSPITALS_MANAGE],
    },
  },
];

