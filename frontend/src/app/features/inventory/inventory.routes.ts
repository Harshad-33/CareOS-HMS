import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const INVENTORY_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Inventory | CareOS-HMS',
    data: {
      moduleName: 'Inventory',
      moduleIcon: 'inventory_2',
      description: 'Medical supplies, batch tracking, and warehouse stock levels.',
      targetPhase: 'Future Phase',
    },
  },
];
