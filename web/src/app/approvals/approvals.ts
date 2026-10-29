import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Claim, ClaimsApi } from '../api/claims-api';

@Component({
  selector: 'app-approvals',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    <section class="card">
      <h2>Waiting for a decision</h2>
      @if (message(); as m) {
        <p [class]="m.kind">{{ m.text }}</p>
      }
      @if (pending().length === 0) {
        <p class="muted">Nothing to review.</p>
      } @else {
        <table>
          <thead>
            <tr><th>Employee</th><th>Description</th><th class="num">Amount</th><th>Submitted</th><th></th></tr>
          </thead>
          <tbody>
            @for (c of pending(); track c.id) {
              <tr>
                <td>{{ c.owner }}</td>
                <td><a [routerLink]="['/claims', c.id]">{{ c.description }}</a></td>
                <td class="num">{{ c.amount | currency: 'CAD' : 'symbol-narrow' }}</td>
                <td>{{ c.submittedAt | date: 'medium' }}</td>
                <td>
                  <div class="actions">
                    <button (click)="decide(c, 'approve')" [disabled]="busy() === c.id">Approve</button>
                    <button class="secondary" (click)="decide(c, 'reject')" [disabled]="busy() === c.id">Reject</button>
                  </div>
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>
  `,
})
export class Approvals {
  private readonly api = inject(ClaimsApi);

  protected readonly pending = signal<Claim[]>([]);
  protected readonly busy = signal<string | null>(null);
  protected readonly message = signal<{ kind: 'ok' | 'error'; text: string } | null>(null);

  constructor() {
    this.api.pending().subscribe((list) => this.pending.set(list));
  }

  protected decide(claim: Claim, decision: 'approve' | 'reject'): void {
    this.busy.set(claim.id);
    this.api.decide(claim.id, decision).subscribe({
      next: () => {
        this.pending.update((list) => list.filter((c) => c.id !== claim.id));
        this.message.set({ kind: 'ok', text: `${decision === 'approve' ? 'Approved' : 'Rejected'} "${claim.description}".` });
        this.busy.set(null);
      },
      error: (e: HttpErrorResponse) => {
        this.message.set({ kind: 'error', text: Approvals.explain(e) });
        if (e.status === 409) this.pending.update((list) => list.filter((c) => c.id !== claim.id));
        this.busy.set(null);
      },
    });
  }

  private static explain(e: HttpErrorResponse): string {
    switch (e.status) {
      case 403: return "You can't decide on your own claim. Another manager has to.";
      case 409: return 'Someone already decided on this claim.';
      case 429: return 'Too many requests. Wait a minute and try again.';
      default: return 'That did not work. Try again.';
    }
  }
}
