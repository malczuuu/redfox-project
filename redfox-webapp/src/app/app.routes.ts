import { Routes } from '@angular/router';
import { Layout } from './layout/layout/layout';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./pages/home/home').then((m) => m.Home),
  },
  {
    path: 'oauth2/callback',
    loadComponent: () =>
      import('./pages/oauth2-callback/oauth2-callback').then((m) => m.OAuth2Callback),
  },
  {
    path: '',
    component: Layout,
    canActivate: [authGuard],
    children: [
      {
        path: 'projects',
        loadComponent: () => import('./pages/projects/project-list').then((m) => m.ProjectList),
      },
      {
        path: 'projects/:projectId/things',
        loadComponent: () => import('./pages/things/thing-list').then((m) => m.ThingList),
      },
      {
        path: 'users',
        loadComponent: () => import('./pages/users/user-list').then((m) => m.UserList),
      },
    ],
  },
];
