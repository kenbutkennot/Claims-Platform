import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { DemoSessionService } from '../services/demo-session.service';

export const staffGuard: CanActivateFn = () => {
  const role = inject(DemoSessionService).current()?.role;
  return role === 'OFFICER' || role === 'MANAGER' ? true : inject(Router).parseUrl('/claims');
};

export const claimantGuard: CanActivateFn = () =>
  inject(DemoSessionService).current()?.role === 'CLAIMANT'
    ? true
    : inject(Router).parseUrl('/claims');
