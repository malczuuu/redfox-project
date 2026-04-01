import { Routes } from '@angular/router';
import { Layout } from './layout/layout/layout';

export const routes: Routes = [
  {
    path: '',
    component: Layout,
    children: [
      { path: '', redirectTo: 'projects', pathMatch: 'full' },
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
