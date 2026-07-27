import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'fleet' },
  {
    path: 'fleet',
    title: 'Flotte — FatigueTracker',
    loadComponent: () => import('./fleet/fleet.component').then((m) => m.FleetComponent)
  },
  { path: '**', redirectTo: 'fleet' }
];
