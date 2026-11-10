import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const CLINICAL_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Clinical / EMR | CareOS-HMS',
    data: {
      moduleName: 'Clinical / EMR',
      moduleIcon: 'clinical_notes',
      description: 'Electronic medical records, clinical notes, and vitals.',
      targetPhase: 'Future Phase',
    },
  },
];
