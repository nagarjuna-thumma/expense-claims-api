import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Auth, authInterceptor, roleGuard } from './auth';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Auth, useValue: { accessToken: () => 'token-123' } },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('sends the token to the API', () => {
    http.get('/api/claims/mine').subscribe();
    expect(backend.expectOne('/api/claims/mine').request.headers.get('Authorization')).toBe('Bearer token-123');
  });

  it('never sends the token anywhere else', () => {
    http.get('https://example.com/api/claims').subscribe();
    http.get('/apiary').subscribe();
    expect(backend.expectOne('https://example.com/api/claims').request.headers.has('Authorization')).toBe(false);
    expect(backend.expectOne('/apiary').request.headers.has('Authorization')).toBe(false);
  });
});

describe('roleGuard', () => {
  function run(role: 'employee' | 'manager', roles: string[]) {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: Auth, useValue: { has: (r: string) => roles.includes(r) } }],
    });
    return TestBed.runInInjectionContext(() => roleGuard(role)({} as never, {} as never));
  }

  it('lets you in with the role', () => {
    expect(run('manager', ['manager'])).toBe(true);
  });

  it('sends a non-manager home from the approvals page', () => {
    expect(String(run('manager', ['employee']))).toBe('/');
  });

  it('refuses the home page without redirecting, so there is no redirect loop', () => {
    expect(run('employee', [])).toBe(false);
  });
});
