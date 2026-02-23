import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SubscriptionService } from './subscription.service';
import { environment } from '../../../environments/environment';

describe('SubscriptionService', () => {
  let service: SubscriptionService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/subscriptions`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SubscriptionService]
    });

    service = TestBed.inject(SubscriptionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch all plans', () => {
    const mockPlans = [
      { id: '1', name: 'FREE', displayName: 'Gratuit', monthlyPrice: 0 },
      { id: '2', name: 'PRO', displayName: 'Pro', monthlyPrice: 19.99 }
    ];

    service.getPlans().subscribe(plans => {
      expect(plans.length).toBe(2);
      expect(plans[0].name).toBe('FREE');
      expect(plans[1].name).toBe('PRO');
    });

    const req = httpMock.expectOne(`${apiUrl}/plans`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPlans);
  });

  it('should fetch current subscription', () => {
    const mockSub = {
      id: '1',
      plan: { id: '2', name: 'PRO', displayName: 'Pro' },
      status: 'ACTIVE'
    };

    service.getCurrentSubscription().subscribe(sub => {
      expect(sub.status).toBe('ACTIVE');
      expect(sub.plan.name).toBe('PRO');
    });

    const req = httpMock.expectOne(`${apiUrl}/current`);
    expect(req.request.method).toBe('GET');
    req.flush(mockSub);
  });

  it('should create checkout session', () => {
    const mockResponse = { sessionId: 'cs_123', url: 'https://checkout.stripe.com/cs_123' };

    service.createCheckout('plan-uuid', 'monthly').subscribe(response => {
      expect(response.url).toContain('stripe.com');
    });

    const req = httpMock.expectOne(`${apiUrl}/checkout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ planId: 'plan-uuid', billingPeriod: 'monthly' });
    req.flush(mockResponse);
  });

  it('should create billing portal session', () => {
    const mockResponse = { url: 'https://billing.stripe.com/session' };

    service.createBillingPortal().subscribe(response => {
      expect(response.url).toContain('stripe.com');
    });

    const req = httpMock.expectOne(`${apiUrl}/billing-portal`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should cancel subscription', () => {
    service.cancelSubscription().subscribe();

    const req = httpMock.expectOne(`${apiUrl}/cancel`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('should fetch usage limits', () => {
    const mockUsage = { propertiesUsed: 3, propertiesLimit: 10, contractsUsed: 2, contractsLimit: 10 };

    service.getUsageLimits().subscribe(usage => {
      expect(usage.propertiesUsed).toBe(3);
      expect(usage.propertiesLimit).toBe(10);
    });

    const req = httpMock.expectOne(`${apiUrl}/usage`);
    expect(req.request.method).toBe('GET');
    req.flush(mockUsage);
  });
});
