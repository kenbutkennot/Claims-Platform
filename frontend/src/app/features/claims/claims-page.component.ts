import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Claim, ClaimStatus, ClaimType } from '../../core/models/claims';
import { ClaimsApiService } from '../../core/services/claims-api.service';
import { DemoSessionService } from '../../core/services/demo-session.service';
import { apiErrorMessage } from '../../shared/api-error';

@Component({
  standalone: true,
  selector: 'app-claims-page',
  imports: [DatePipe, DecimalPipe, RouterLink],
  templateUrl: './claims-page.component.html',
})
export class ClaimsPageComponent implements OnInit {
  readonly claims = signal<Claim[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly statuses: ClaimStatus[] = ['SUBMITTED', 'UNDER_REVIEW', 'INFORMATION_REQUIRED', 'APPROVED', 'REJECTED', 'SETTLED'];
  readonly types: ClaimType[] = ['MOTOR', 'PROPERTY'];
  status = '';
  type = '';
  search = '';

  constructor(readonly session: DemoSessionService, private readonly api: ClaimsApiService) {}

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.claims.set(await firstValueFrom(this.api.list({
        status: this.status as ClaimStatus || undefined,
        type: this.type as ClaimType || undefined,
        search: this.search || undefined,
      })));
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  updateSearch(value: string): void {
    this.search = value;
    void this.load();
  }

  updateStatus(value: string): void {
    this.status = value;
    void this.load();
  }

  updateType(value: string): void {
    this.type = value;
    void this.load();
  }

  statusLabel(status: ClaimStatus): string {
    return status.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());
  }

  typeLabel(type: ClaimType): string {
    return type.charAt(0) + type.slice(1).toLowerCase();
  }
}
