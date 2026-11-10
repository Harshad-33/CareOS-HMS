import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const PHARMACY_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Pharmacy | CareOS-HMS',
    data: {
      moduleName: 'Pharmacy',
      moduleIcon: 'medication',
      description: 'Prescription fulfillment and medication dispensing.',
      targetPhase: 'Future Phase',
    },
  },
];
