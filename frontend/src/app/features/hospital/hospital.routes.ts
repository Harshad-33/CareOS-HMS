import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const HOSPITAL_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Hospital Profile | CareOS-HMS',
    data: {
      moduleName: 'Hospital Profile & Tenant Configuration',
      moduleIcon: 'domain',
      description: 'Multi-hospital organization setup and tenant settings.',
      targetPhase: 'Future Phase',
    },
  },
];
