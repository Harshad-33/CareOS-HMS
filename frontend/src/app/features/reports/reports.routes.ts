import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const REPORTS_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Reports | CareOS-HMS',
    data: {
      moduleName: 'Reports',
      moduleIcon: 'assessment',
      description: 'Hospital clinical and operational analytics reports.',
      targetPhase: 'Future Phase',
    },
  },
];
