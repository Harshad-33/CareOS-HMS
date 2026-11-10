import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const BILLING_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Billing | CareOS-HMS',
    data: {
      moduleName: 'Billing',
      moduleIcon: 'receipt_long',
      description: 'Patient invoicing, payments, and insurance billing.',
      targetPhase: 'Future Phase',
    },
  },
];
