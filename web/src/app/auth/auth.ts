import { HttpInterceptorFn } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { API } from '../api/claims-api';
import { Role, rolesFromToken } from './roles';

export const authConfig: AuthConfig = {
  issuer: 'http://localhost:8180/realms/expenses',
  clientId: 'expenses-web',
  // Authorization code flow with PKCE. The SPA is a public client, so there's no secret to leak.
  responseType: 'code',
  scope: 'openid profile',
  redirectUri: window.location.origin + '/',
  postLogoutRedirectUri: window.location.origin + '/',
  requireHttps: false, // local Keycloak runs on plain http
};

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly oauth = inject(OAuthService);
  private readonly token = signal<string | null>(null);

  readonly roles = computed(() => rolesFromToken(this.token()));
  readonly loggedIn = computed(() => this.token() !== null);
  readonly username = signal<string | null>(null);

  async init(): Promise<void> {
    this.oauth.configure(authConfig);
    this.oauth.events.subscribe(() => this.refresh());
    await this.oauth.loadDiscoveryDocumentAndTryLogin();
    this.oauth.setupAutomaticSilentRefresh(); // uses the refresh token before the access token expires
    this.refresh();
  }

  has(role: Role): boolean {
    return this.roles().includes(role);
  }

  accessToken(): string | null {
    return this.token();
  }

  login(): void {
    this.oauth.initCodeFlow();
  }

  logout(): void {
    this.oauth.logOut();
  }

  private refresh(): void {
    const valid = this.oauth.hasValidAccessToken();
    this.token.set(valid ? this.oauth.getAccessToken() : null);
    this.username.set(valid ? (this.oauth.getIdentityClaims()?.['preferred_username'] ?? null) : null);
  }
}

/** Adds the bearer token to API calls only, never to Keycloak or anything else. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(Auth).accessToken();
  if (!token || !req.url.startsWith(API + '/')) return next(req);
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};

/**
 * Pages other than the home page send you home when you lack the role. The home page itself
 * just refuses, so a user with neither role can't bounce between redirects forever.
 */
export function roleGuard(role: Role): CanActivateFn {
  return () => {
    if (inject(Auth).has(role)) return true;
    return role === 'employee' ? false : inject(Router).createUrlTree(['/']);
  };
}
