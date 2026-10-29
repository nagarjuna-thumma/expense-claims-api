import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Claim, ClaimsApi } from '../api/claims-api';
import { StatusLabelPipe } from './status-label';

@Component({
  selector: 'app-my-claims',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe, StatusLabelPipe],
  template: `
    <section class="card">
      <h2>New claim</h2>
      <form [formGroup]="form" (ngSubmit)="submit()">
        <label>
          What was it for
          <input formControlName="description" maxlength="200" placeholder="Taxi to client site" />
        </label>
        <label>
          Amount (CAD)
          <input formControlName="amount" type="number" step="0.01" min="0.01" max="10000" />
        </label>
        @if (form.controls.amount.touched && form.controls.amount.invalid) {
          <p class="error">Amount must be between 0.01 and 10,000.</p>
        }
        <button type="submit" [disabled]="form.invalid || saving()">Submit</button>
        @if (error()) {
          <p class="error">{{ error() }}</p>
        }
      </form>
    </section>

    <section class="card">
      <h2>My claims</h2>
      @if (claims().length === 0) {
        <p class="muted">No claims yet.</p>
      } @else {
        <table>
          <thead>
            <tr><th>Description</th><th class="num">Amount</th><th>Status</th><th>Submitted</th></tr>
          </thead>
          <tbody>
            @for (c of claims(); track c.id) {
              <tr>
                <td><a [routerLink]="['/claims', c.id]">{{ c.description }}</a></td>
                <td class="num">{{ c.amount | currency: 'CAD' : 'symbol-narrow' }}</td>
                <td><span class="status {{ c.status.toLowerCase() }}">{{ c.status | statusLabel }}</span></td>
                <td>{{ c.submittedAt | date: 'medium' }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>
  `,
})
export class MyClaims {
  private readonly api = inject(ClaimsApi);

  protected readonly claims = signal<Claim[]>([]);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  // Same limits as SubmitClaimRequest in the API. The API still checks them.
  protected readonly form = inject(FormBuilder).nonNullable.group({
    description: ['', [Validators.required, Validators.maxLength(200)]],
    amount: [null as number | null, [Validators.required, Validators.min(0.01), Validators.max(10000)]],
  });

  constructor() {
    this.load();
  }

  protected submit(): void {
    const { description, amount } = this.form.getRawValue();
    if (this.form.invalid || amount === null) return;
    this.saving.set(true);
    this.error.set(null);
    this.api.submit({ description: description.trim(), amount }).subscribe({
      next: (claim) => {
        this.claims.update((list) => [claim, ...list]);
        this.form.reset();
        this.saving.set(false);
      },
      error: () => {
        this.error.set('The claim could not be submitted. Check the amount and try again.');
        this.saving.set(false);
      },
    });
  }

  private load(): void {
    this.api.mine().subscribe((list) => this.claims.set(list));
  }
}
