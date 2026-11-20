import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ClaimType } from '../../core/models/claims';
import { ClaimsApiService } from '../../core/services/claims-api.service';
import { apiErrorMessage } from '../../shared/api-error';

@Component({
  standalone: true,
  selector: 'app-submit-claim',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './submit-claim.component.html',
})
export class SubmitClaimComponent {
  private readonly fb = inject(FormBuilder);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly today = new Date().toISOString().slice(0, 10);
  readonly form = this.fb.group({
    claimType: this.fb.nonNullable.control<ClaimType>('MOTOR', Validators.required),
    incidentDate: this.fb.nonNullable.control('', Validators.required),
    incidentDescription: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(2000)]),
    estimatedAmount: this.fb.nonNullable.control<number | null>(null, [Validators.required, Validators.min(0)]),
    incidentLocation: this.fb.nonNullable.control(''),
    vehicleRegistration: this.fb.nonNullable.control(''),
  });

  constructor(private readonly api: ClaimsApiService, private readonly router: Router) {}

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set('');
    const value = this.form.getRawValue();
    try {
      const claim = await firstValueFrom(this.api.submit({
        claimType: value.claimType,
        incidentDate: value.incidentDate,
        incidentDescription: value.incidentDescription.trim(),
        estimatedAmount: Number(value.estimatedAmount),
        incidentLocation: value.incidentLocation.trim() || null,
        vehicleRegistration: value.claimType === 'MOTOR' ? value.vehicleRegistration.trim() || null : null,
      }));
      await this.router.navigate(['/claims', claim.id]);
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }
}
