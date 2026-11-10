import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const ADMISSIONS_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Admissions | CareOS-HMS',
    data: {
      moduleName: 'Admissions',
      moduleIcon: 'hotel',
      description: 'Inpatient admissions, ward transfers, and bed management.',
      targetPhase: 'Future Phase',
    },
  },
];
