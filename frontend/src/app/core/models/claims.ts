export type Role = 'CLAIMANT' | 'OFFICER' | 'MANAGER';
export type ClaimType = 'MOTOR' | 'PROPERTY';
export type ClaimStatus =
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'INFORMATION_REQUIRED'
  | 'APPROVED'
  | 'REJECTED'
  | 'SETTLED';
export type InformationRequestStatus = 'OPEN' | 'RESPONDED' | 'CLOSED';

export interface DemoUser {
  id: string;
  name: string;
  email: string;
  role: Role;
}

export interface InformationRequest {
  id: string;
  message: string;
  status: InformationRequestStatus;
  requestedBy: string;
  requestedByName: string;
  requestedAt: string;
  response: string | null;
  respondedAt: string | null;
}

export interface ClaimHistory {
  id: string;
  previousStatus: ClaimStatus | null;
  newStatus: ClaimStatus | null;
  action: string;
  remarks: string | null;
  changedBy: string;
  changedByName: string;
  createdAt: string;
}

export interface Claim {
  id: string;
  claimNumber: string;
  claimantId: string;
  claimantName: string;
  assignedOfficerId: string | null;
  assignedOfficerName: string | null;
  claimType: ClaimType;
  status: ClaimStatus;
  incidentDate: string;
  incidentDescription: string;
  estimatedAmount: number;
  approvedAmount: number | null;
  settledAmount: number | null;
  decisionReason: string | null;
  incidentLocation: string | null;
  vehicleRegistration: string | null;
  submittedAt: string;
  updatedAt: string;
  settledAt: string | null;
  informationRequests: InformationRequest[];
  history: ClaimHistory[];
}

export interface DashboardSummary {
  totalClaims: number;
  awaitingReview: number;
  underReview: number;
  informationRequired: number;
  approvedUnsettled: number;
  rejected: number;
  estimatedOutstandingExposure: number;
  approvedUnsettledAmount: number;
}

export interface OfficerWorkload {
  officerId: string;
  officerName: string;
  assignedClaims: number;
  activeClaims: number;
}

export interface CreateClaim {
  claimType: ClaimType;
  incidentDate: string;
  incidentDescription: string;
  estimatedAmount: number;
  incidentLocation: string | null;
  vehicleRegistration: string | null;
}
