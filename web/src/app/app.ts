import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from './auth/auth';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header>
      <h1>Expense claims</h1>
      @if (auth.loggedIn()) {
        <nav>
          @if (auth.has('employee')) {
            <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">My claims</a>
          }
          @if (auth.has('manager')) {
            <a routerLink="/approvals" routerLinkActive="active">Approvals</a>
          }
        </nav>
        <span class="user">{{ auth.username() }} <button class="link" (click)="auth.logout()">Log out</button></span>
      }
    </header>
    <main>
      @if (auth.loggedIn() && !auth.has('employee') && !auth.has('manager')) {
        <section class="card">
          <p>Your account can't submit or review claims. Auditors check the audit log through <code>GET /audit/verify</code>.</p>
        </section>
      } @else if (auth.loggedIn()) {
        <router-outlet />
      } @else {
        <section class="card">
          <p>Sign in with your company account to submit or review expense claims.</p>
          <button (click)="auth.login()">Sign in</button>
        </section>
      }
    </main>
  `,
})
export class App {
  protected readonly auth = inject(Auth);
}
