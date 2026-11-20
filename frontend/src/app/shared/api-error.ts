import { HttpErrorResponse } from '@angular/common/http';

export function apiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const detail = error.error?.detail;
    if (typeof detail === 'string') return detail;
    if (error.status === 0) return 'Cannot reach the claims API. Check that the backend is running.';
    return `The request could not be completed (HTTP ${error.status}).`;
  }
  return 'An unexpected error occurred. Please try again.';
}
