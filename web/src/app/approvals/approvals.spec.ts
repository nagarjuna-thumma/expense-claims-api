import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Claim } from '../api/claims-api';
import { Approvals } from './approvals';

const claim = (id: string, owner: string, description: string): Claim => ({
  id, owner, description, amount: 80, status: 'SUBMITTED', submittedAt: '2026-10-29T14:00:00Z', decidedBy: null,
});

describe('Approvals', () => {
  let backend: HttpTestingController;

  async function render() {
    const fixture = TestBed.createComponent(Approvals);
    backend.expectOne('/api/claims/pending').flush([claim('c1', 'alice', 'Client lunch'), claim('c2', 'bob', 'Conference')]);
    await fixture.whenStable();
    return fixture;
  }

  function buttons(el: HTMLElement, row: number) {
    return el.querySelectorAll('tbody tr')[row].querySelectorAll('button');
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Approvals],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('approving removes the claim from the queue', async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    buttons(el, 0)[0].click();

    backend.expectOne({ method: 'POST', url: '/api/claims/c1/approve' }).flush(claim('c1', 'alice', 'Client lunch'));
    await fixture.whenStable();

    expect(el.querySelectorAll('tbody tr').length).toBe(1);
    expect(el.textContent).toContain('Approved "Client lunch"');
  });

  it("explains a refused self-approval and keeps the claim listed", async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    buttons(el, 1)[0].click();

    backend.expectOne('/api/claims/c2/approve').flush(null, { status: 403, statusText: 'Forbidden' });
    await fixture.whenStable();

    expect(el.textContent).toContain("can't decide on your own claim");
    expect(el.querySelectorAll('tbody tr').length).toBe(2);
  });

  it('drops a claim someone else already decided', async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    buttons(el, 0)[1].click();

    backend.expectOne('/api/claims/c1/reject').flush(null, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(el.textContent).toContain('already decided');
    expect(el.querySelectorAll('tbody tr').length).toBe(1);
  });
});
