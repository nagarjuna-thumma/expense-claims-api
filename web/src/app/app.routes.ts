import { Routes } from '@angular/router';
import { Approvals } from './approvals/approvals';
import { roleGuard } from './auth/auth';
import { ClaimDetail } from './claims/claim-detail';
import { MyClaims } from './claims/my-claims';

export const routes: Routes = [
  { path: '', component: MyClaims, canActivate: [roleGuard('employee')] },
  { path: 'approvals', component: Approvals, canActivate: [roleGuard('manager')] },
  { path: 'claims/:id', component: ClaimDetail },
  { path: '**', redirectTo: '' },
];
