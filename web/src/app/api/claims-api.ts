import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { components } from './schema';

// Generated from docs/openapi.json by `npm run api:types`, so a backend change that breaks the
// contract breaks the build here too.
export type Claim = components['schemas']['ClaimResponse'];
export type HistoryEntry = components['schemas']['HistoryEntry'];
export type SubmitClaim = components['schemas']['SubmitClaimRequest'];

/** The dev server proxies /api to the Spring Boot API on 8080, so there's no CORS to configure. */
export const API = '/api';

@Injectable({ providedIn: 'root' })
export class ClaimsApi {
  private readonly http = inject(HttpClient);

  mine(): Observable<Claim[]> {
    return this.http.get<Claim[]>(`${API}/claims/mine`);
  }

  pending(): Observable<Claim[]> {
    return this.http.get<Claim[]>(`${API}/claims/pending`);
  }

  get(id: string): Observable<Claim> {
    return this.http.get<Claim>(`${API}/claims/${id}`);
  }

  history(id: string): Observable<HistoryEntry[]> {
    return this.http.get<HistoryEntry[]>(`${API}/claims/${id}/history`);
  }

  submit(claim: SubmitClaim): Observable<Claim> {
    return this.http.post<Claim>(`${API}/claims`, claim);
  }

  decide(id: string, decision: 'approve' | 'reject'): Observable<Claim> {
    return this.http.post<Claim>(`${API}/claims/${id}/${decision}`, null);
  }
}
