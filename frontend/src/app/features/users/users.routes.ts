import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const USERS_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Users & Access | CareOS-HMS',
    data: {
      moduleName: 'User Accounts',
      moduleIcon: 'manage_accounts',
      description: 'User account directory and role assignments.',
      targetPhase: 'Future Phase',
    },
  },
];
