import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'categories'
  },
  {
    path: 'categories',
    canActivate: [authGuard],
    loadChildren: () => import('./features/category/category.routes').then(m => m.categoryRoutes)
  },
  {
    path: 'components',
    canActivate: [authGuard],
    loadChildren: () => import('./features/processing-components/processing-components.routes').then(m => m.PROCESSING_COMPONENTS_ROUTES)
  },
  { path: '**', redirectTo: 'categories' }
];
