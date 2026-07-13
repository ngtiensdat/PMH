import { Routes } from '@angular/router';

export const PROCESSING_COMPONENTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/component-list/component-list').then(m => m.ComponentListComponent)
  },
  {
    path: 'add',
    loadComponent: () =>
      import('./components/component-dialog/component-dialog').then(m => m.ComponentDialogComponent)
  },
  {
    path: 'edit/:code',
    loadComponent: () =>
      import('./components/component-dialog/component-dialog').then(m => m.ComponentDialogComponent)
  },
  {
    path: 'copy/:code',
    loadComponent: () =>
      import('./components/component-dialog/component-dialog').then(m => m.ComponentDialogComponent)
  },
  {
    path: 'detail/:code',
    loadComponent: () =>
      import('./components/component-detail/component-detail').then(m => m.ComponentDetailComponent)
  }
];
