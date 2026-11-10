import { Routes } from '@angular/router';
import { AppShell } from './core/layout/app-shell';
import { Home } from './features/home/home';

export const routes: Routes = [
  {
    path: '',
    component: AppShell,
    children: [
      {
        path: '',
        component: Home,
        title: 'CareOS-HMS',
      },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
