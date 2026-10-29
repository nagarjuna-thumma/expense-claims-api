import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { Claim, ClaimsApi, HistoryEntry } from '../api/claims-api';
import { StatusLabelPipe } from './status-label';

@Component({
  selector: 'app-claim-detail',
  imports: [CurrencyPipe, DatePipe, StatusLabelPipe],
  template: `
    @if (notFound()) {
      <section class="card"><p>Claim not found.</p></section>
    } @else if (claim(); as c) {
      <section class="card">
        <h2>{{ c.description }}</h2>
        <p>
          {{ c.amount | currency: 'CAD' : 'symbol-narrow' }} by {{ c.owner }} ·
          <span class="status {{ c.status.toLowerCase() }}">{{ c.status | statusLabel }}</span>
          @if (c.decidedBy) { by {{ c.decidedBy }} }
        </p>
        <h3>History</h3>
        <ol class="history">
          @for (h of history(); track h.seq) {
            <li><strong>{{ h.action }}</strong> by {{ h.actor }} · {{ h.at | date: 'medium' }}</li>
          }
        </ol>
      </section>
    }
  `,
})
export class ClaimDetail implements OnInit {
  private readonly api = inject(ClaimsApi);

  /** Bound from the :id route parameter. */
  readonly id = input.required<string>();

  protected readonly claim = signal<Claim | null>(null);
  protected readonly history = signal<HistoryEntry[]>([]);
  protected readonly notFound = signal(false);

  ngOnInit(): void {
    forkJoin({ claim: this.api.get(this.id()), history: this.api.history(this.id()) }).subscribe({
      next: ({ claim, history }) => {
        this.claim.set(claim);
        this.history.set(history);
      },
      // The API answers 404 for someone else's claim too, so there's nothing more to say.
      error: (e: HttpErrorResponse) => this.notFound.set(e.status === 404),
    });
  }
}
