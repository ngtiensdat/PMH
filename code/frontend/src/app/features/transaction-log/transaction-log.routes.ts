import { Routes } from '@angular/router';
import { TransactionLogListComponent } from './components/transaction-log-list/transaction-log-list';

export const transactionLogRoutes: Routes = [
  {
    path: '',
    component: TransactionLogListComponent
  }
];
