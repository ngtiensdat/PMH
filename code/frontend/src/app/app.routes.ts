import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'categories'
  },
  {
    path: 'categories',
    loadChildren: () => import('./features/category/category.routes').then(m => m.categoryRoutes)
  },
  {
    path: 'components',
    loadChildren: () => import('./features/processing-components/processing-components.routes').then(m => m.PROCESSING_COMPONENTS_ROUTES)
  },
  { path: '**', redirectTo: 'categories' }
];
