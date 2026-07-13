import { Routes } from '@angular/router';
import { CategoryListComponent } from './components/category-list/category-list';
import { CategoryDialogComponent } from './components/category-dialog/category-dialog';
import { CategoryDetailComponent } from './components/category-detail/category-detail';

export const categoryRoutes: Routes = [
  {
    path: '',
    component: CategoryListComponent
  },
  {
    path: 'add',
    component: CategoryDialogComponent
  },
  {
    path: 'edit/:id',
    component: CategoryDialogComponent
  },
  {
    path: 'copy/:id',
    component: CategoryDialogComponent
  },
  {
    path: 'detail/:id',
    component: CategoryDetailComponent
  }
];
