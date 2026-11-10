import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const EMPLOYEES_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Employees | CareOS-HMS',
    data: {
      moduleName: 'Employees',
      moduleIcon: 'badge',
      description: 'Hospital staff directory and department assignments.',
      targetPhase: 'Future Phase',
    },
  },
];
