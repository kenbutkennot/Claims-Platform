import { Routes } from '@angular/router';
import { staffGuard, claimantGuard } from './core/guards/role.guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'claims' },
  { path: 'claims/new', canActivate: [claimantGuard], loadComponent: () => import('./features/claims/submit-claim.component').then((m) => m.SubmitClaimComponent) },
  { path: 'claims/:id', loadComponent: () => import('./features/claims/claim-detail.component').then((m) => m.ClaimDetailComponent) },
  { path: 'dashboard', canActivate: [staffGuard], loadComponent: () => import('./features/officer/dashboard-page.component').then((m) => m.DashboardPageComponent) },
  { path: 'claims', loadComponent: () => import('./features/claims/claims-page.component').then((m) => m.ClaimsPageComponent) },
  { path: '**', redirectTo: 'claims' }
];
