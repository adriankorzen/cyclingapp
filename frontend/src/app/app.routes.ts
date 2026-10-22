import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'activities' },
  {
    path: 'activities',
    title: 'Activities · CyclingApp',
    loadComponent: () => import('./activities/activities-page').then((m) => m.ActivitiesPage),
  },
  {
    path: 'activities/:id',
    title: 'Activity · CyclingApp',
    loadComponent: () =>
      import('./activities/activity-detail-page').then((m) => m.ActivityDetailPage),
  },
  {
    path: 'profile',
    title: 'Profile · CyclingApp',
    loadComponent: () => import('./profile/profile-page').then((m) => m.ProfilePage),
  },
  { path: '**', redirectTo: 'activities' },
];
