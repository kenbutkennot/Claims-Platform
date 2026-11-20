import { DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { DashboardSummary, OfficerWorkload } from '../../core/models/claims';
import { ClaimsApiService } from '../../core/services/claims-api.service';
import { DemoSessionService } from '../../core/services/demo-session.service';
import { apiErrorMessage } from '../../shared/api-error';

@Component({
  standalone: true,
  selector: 'app-dashboard-page',
  imports: [DecimalPipe],
  templateUrl: './dashboard-page.component.html',
})
export class DashboardPageComponent implements OnInit {
  readonly summary = signal<DashboardSummary | null>(null);
  readonly workload = signal<OfficerWorkload[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor(readonly session: DemoSessionService, private readonly api: ClaimsApiService) {}

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    try {
      const [summary, workload] = await Promise.all([
        firstValueFrom(this.api.summary()),
        firstValueFrom(this.api.workload()),
      ]);
      this.summary.set(summary);
      this.workload.set(workload);
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }
}
