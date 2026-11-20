import { DatePipe, DecimalPipe, LowerCasePipe, TitleCasePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Claim, ClaimStatus, DemoUser } from '../../core/models/claims';
import { ClaimsApiService } from '../../core/services/claims-api.service';
import { DemoSessionService } from '../../core/services/demo-session.service';
import { apiErrorMessage } from '../../shared/api-error';

@Component({
  standalone: true,
  selector: 'app-claim-detail',
  imports: [DatePipe, DecimalPipe, LowerCasePipe, TitleCasePipe, RouterLink, ReactiveFormsModule],
  templateUrl: './claim-detail.component.html',
})
export class ClaimDetailComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly claim = signal<Claim | null>(null);
  readonly officers = signal<DemoUser[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly responseValues: Record<string, string> = {};
  readonly assignmentForm = this.fb.group({ officerId: this.fb.nonNullable.control('', Validators.required) });
  readonly informationForm = this.fb.group({ message: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(1200)]) });
  readonly decisionForm = this.fb.group({
    reason: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(1200)]),
    approvedAmount: this.fb.nonNullable.control<number | null>(null, [Validators.min(0)]),
  });
  readonly settlementForm = this.fb.group({ amount: this.fb.nonNullable.control<number | null>(null, [Validators.required, Validators.min(0)]) });

  constructor(
    private readonly route: ActivatedRoute,
    private readonly api: ClaimsApiService,
    readonly session: DemoSessionService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) void this.load(id);
    else this.error.set('Claim identifier is missing.');
  }

  get staff(): boolean {
    const role = this.session.current()?.role;
    return role === 'OFFICER' || role === 'MANAGER';
  }

  get canReview(): boolean {
    const user = this.session.current();
    const claim = this.claim();
    return !!user && !!claim && (user.role === 'MANAGER' || claim.assignedOfficerId === user.id);
  }

  async load(id: string): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const claim = await firstValueFrom(this.api.get(id));
      this.claim.set(claim);
      this.assignmentForm.controls.officerId.setValue(
        claim.assignedOfficerId ?? (this.session.current()?.role === 'OFFICER' ? this.session.current()!.id : ''),
      );
      if (claim.approvedAmount !== null && this.settlementForm.controls.amount.value === null) {
        this.settlementForm.controls.amount.setValue(claim.approvedAmount);
      }
      if (this.staff) this.officers.set(await firstValueFrom(this.api.users()));
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  async assign(): Promise<void> {
    const claim = this.claim();
    if (!claim || this.assignmentForm.invalid) return;
    await this.perform(() => this.api.assign(claim.id, this.assignmentForm.controls.officerId.value));
  }

  async requestInformation(): Promise<void> {
    const claim = this.claim();
    if (!claim || this.informationForm.invalid) {
      this.informationForm.markAllAsTouched();
      return;
    }
    await this.perform(() => this.api.requestInformation(claim.id, this.informationForm.controls.message.value.trim()));
    this.informationForm.reset({ message: '' });
  }

  async respond(requestId: string): Promise<void> {
    const claim = this.claim();
    const response = this.responseValues[requestId]?.trim();
    if (!claim || !response) {
      this.error.set('Enter a response before submitting.');
      return;
    }
    await this.perform(() => this.api.respond(claim.id, requestId, response));
    this.responseValues[requestId] = '';
  }

  async decide(approved: boolean): Promise<void> {
    const claim = this.claim();
    const form = this.decisionForm.getRawValue();
    if (!claim || this.decisionForm.controls.reason.invalid
      || (approved && (form.approvedAmount === null || form.approvedAmount < 0))) {
      this.decisionForm.markAllAsTouched();
      this.error.set(approved ? 'Enter a decision reason and non-negative approved amount.' : 'Enter a reason for rejection.');
      return;
    }
    await this.perform(() => this.api.decide(claim.id, approved, form.reason.trim(),
      approved ? Number(form.approvedAmount) : null));
    this.decisionForm.reset({ reason: '', approvedAmount: null });
  }

  async settle(): Promise<void> {
    const claim = this.claim();
    const amount = this.settlementForm.controls.amount.value;
    if (!claim || amount === null || this.settlementForm.invalid) {
      this.settlementForm.markAllAsTouched();
      return;
    }
    await this.perform(() => this.api.settle(claim.id, Number(amount)));
  }

  statusLabel(status: ClaimStatus): string {
    return status.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());
  }

  private async perform(operation: () => ReturnType<ClaimsApiService['get']>): Promise<void> {
    const claim = this.claim();
    if (!claim) return;
    this.busy.set(true);
    this.error.set('');
    this.notice.set('');
    try {
      await firstValueFrom(operation());
      await this.load(claim.id);
      this.notice.set('Claim updated successfully.');
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }
}
