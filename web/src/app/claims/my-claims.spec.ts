import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Claim } from '../api/claims-api';
import { MyClaims } from './my-claims';

const taxi: Claim = {
  id: '6f1c2a40-0000-4000-8000-000000000001', owner: 'alice', description: 'Taxi to client site',
  amount: 42.5, status: 'SUBMITTED', submittedAt: '2026-10-29T14:00:00Z', decidedBy: null,
};

describe('MyClaims', () => {
  let backend: HttpTestingController;

  async function render() {
    const fixture = TestBed.createComponent(MyClaims);
    backend.expectOne('/api/claims/mine').flush([taxi]);
    await fixture.whenStable();
    return fixture;
  }

  function type(el: HTMLElement, selector: string, value: string) {
    const input = el.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MyClaims],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('lists my claims', async () => {
    const el: HTMLElement = (await render()).nativeElement;
    expect(el.querySelector('tbody')?.textContent).toContain('Taxi to client site');
    expect(el.querySelector('tbody')?.textContent).toContain('Submitted');
  });

  it('will not submit an amount the API would reject', async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    type(el, 'input[formControlName=description]', 'Hotel');
    type(el, 'input[formControlName=amount]', '20000');
    await fixture.whenStable();

    expect(el.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(true);
    expect(el.textContent).toContain('between 0.01 and 10,000');
  });

  it('submits a claim and shows it at the top of the list', async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    type(el, 'input[formControlName=description]', '  Train to Ottawa ');
    type(el, 'input[formControlName=amount]', '118.40');
    await fixture.whenStable();
    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();

    const req = backend.expectOne({ method: 'POST', url: '/api/claims' });
    expect(req.request.body).toEqual({ description: 'Train to Ottawa', amount: 118.4 });
    req.flush({ ...taxi, id: '6f1c2a40-0000-4000-8000-000000000002', description: 'Train to Ottawa', amount: 118.4 });
    await fixture.whenStable();

    const rows = el.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Train to Ottawa');
  });
});
