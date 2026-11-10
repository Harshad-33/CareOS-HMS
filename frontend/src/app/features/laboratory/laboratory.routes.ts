import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const LABORATORY_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Laboratory | CareOS-HMS',
    data: {
      moduleName: 'Laboratory',
      moduleIcon: 'biotech',
      description: 'Diagnostic test orders, specimen tracking, and lab reports.',
      targetPhase: 'Future Phase',
    },
  },
];
