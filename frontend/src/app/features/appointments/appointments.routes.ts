import { Routes } from '@angular/router';
import { ModulePlaceholderComponent } from '../../shared/components/module-placeholder/module-placeholder';

export const APPOINTMENTS_ROUTES: Routes = [
  {
    path: '',
    component: ModulePlaceholderComponent,
    title: 'Appointments | CareOS-HMS',
    data: {
      moduleName: 'Appointments',
      moduleIcon: 'event_available',
      description: 'Outpatient scheduling, queues, and doctor visit slots.',
      targetPhase: 'Future Phase',
    },
  },
];
