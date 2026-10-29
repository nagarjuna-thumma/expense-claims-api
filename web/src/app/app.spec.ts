import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { Auth } from './auth/auth';

describe('App', () => {
  function render(roles: string[] | null) {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        {
          provide: Auth,
          useValue: {
            loggedIn: signal(roles !== null),
            username: signal(roles ? 'bob' : null),
            has: (role: string) => roles?.includes(role) ?? false,
            login: () => {},
            logout: () => {},
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('asks you to sign in when there is no session', () => {
    const el = render(null);
    expect(el.textContent).toContain('Sign in');
    expect(el.querySelector('nav')).toBeNull();
  });

  it('only shows Approvals to managers', () => {
    expect(render(['employee']).querySelector('nav')?.textContent).not.toContain('Approvals');
    TestBed.resetTestingModule();
    expect(render(['employee', 'manager']).querySelector('nav')?.textContent).toContain('Approvals');
  });
});
