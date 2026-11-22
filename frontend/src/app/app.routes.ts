import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login-page/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'files',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/browser/browser-page/browser-page').then((m) => m.BrowserPage),
  },
  {
    path: 'files/:folderId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/browser/browser-page/browser-page').then((m) => m.BrowserPage),
  },
  { path: '', pathMatch: 'full', redirectTo: 'files' },
  { path: '**', redirectTo: 'files' },
];