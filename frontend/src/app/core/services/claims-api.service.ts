import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  Claim,
  ClaimStatus,
  ClaimType,
  CreateClaim,
  DashboardSummary,
  DemoUser,
  OfficerWorkload,
} from '../models/claims';

@Injectable({ providedIn: 'root' })
export class ClaimsApiService {
  constructor(private readonly http: HttpClient) {}

  list(filters: { status?: ClaimStatus; type?: ClaimType; search?: string } = {}) {
    let params = new HttpParams();
    if (filters.status) params = params.set('status', filters.status);
    if (filters.type) params = params.set('type', filters.type);
    if (filters.search?.trim()) params = params.set('search', filters.search.trim());
    return this.http.get<Claim[]>('/api/claims', { params });
  }

  get(id: string) {
    return this.http.get<Claim>(`/api/claims/${id}`);
  }

  submit(body: CreateClaim) {
    return this.http.post<Claim>('/api/claims', body);
  }

  assign(id: string, officerId: string) {
    return this.http.patch<Claim>(`/api/claims/${id}/assign`, { officerId });
  }

  requestInformation(id: string, message: string) {
    return this.http.post<Claim>(`/api/claims/${id}/information-requests`, { message });
  }

  respond(id: string, requestId: string, response: string) {
    return this.http.post<Claim>(
      `/api/claims/${id}/information-requests/${requestId}/response`,
      { response },
    );
  }

  decide(id: string, approved: boolean, reason: string, approvedAmount: number | null) {
    return this.http.post<Claim>(`/api/claims/${id}/decision`, {
      approved,
      reason,
      approvedAmount,
    });
  }

  settle(id: string, amount: number) {
    return this.http.post<Claim>(`/api/claims/${id}/settlement`, { amount });
  }

  summary() {
    return this.http.get<DashboardSummary>('/api/dashboard/summary');
  }

  workload() {
    return this.http.get<OfficerWorkload[]>('/api/dashboard/workload');
  }

  users() {
    return this.http.get<DemoUser[]>('/api/demo/users');
  }
}
