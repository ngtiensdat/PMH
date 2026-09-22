import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { noAuthGuard } from './core/guards/no-auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [noAuthGuard],
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'transaction-log'
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
  {
    path: 'transaction-log',
    canActivate: [authGuard],
    loadChildren: () => import('./features/transaction-log/transaction-log.routes').then(m => m.transactionLogRoutes)
  },
  { path: '**', redirectTo: 'transaction-log' }
];
