import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const PATIENTS_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Patients | CareOS-HMS',
    data: {
      moduleName: 'Patients',
      moduleIcon: 'personal_injury',
      description: 'Patient registration, demographics, and medical record identifiers.',
      targetPhase: 'Future Phase',
    },
  },
];
