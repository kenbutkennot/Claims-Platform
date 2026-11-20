import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { DemoSessionService } from './demo-session.service';

export const demoUserInterceptor: HttpInterceptorFn = (request, next) => {
  const user = inject(DemoSessionService).current();
  if (!user || !request.url.startsWith('/api/')) return next(request);
  return next(request.clone({ setHeaders: { 'X-Demo-User': user.id } }));
};
