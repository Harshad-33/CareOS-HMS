import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const DOCTORS_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Doctors | CareOS-HMS',
    data: {
      moduleName: 'Doctors',
      moduleIcon: 'medical_services',
      description: 'Physician profiles, clinical specialties, and availability schedules.',
      targetPhase: 'Future Phase',
    },
  },
];
