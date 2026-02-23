import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PaymentService } from './payment.service';
import { environment } from '../../../environments/environment';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/payments`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PaymentService]
    });

    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch all payments', () => {
    const mockPayments = [
      { id: '1', amount: 850, status: 'PENDING', currency: 'EUR' },
      { id: '2', amount: 850, status: 'PAID', currency: 'EUR' }
    ];

    service.getAll().subscribe(payments => {
      expect(payments.length).toBe(2);
      expect(payments[0].status).toBe('PENDING');
      expect(payments[1].status).toBe('PAID');
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mockPayments);
  });

  it('should fetch payment by id', () => {
    const mockPayment = { id: 'pay-1', amount: 850, status: 'PENDING' };

    service.getById('pay-1').subscribe(payment => {
      expect(payment.id).toBe('pay-1');
      expect(payment.amount).toBe(850);
    });

    const req = httpMock.expectOne(`${apiUrl}/pay-1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPayment);
  });

  it('should fetch payments by rental', () => {
    const mockPayments = [{ id: '1', amount: 850, status: 'PAID' }];

    service.getByRental('rental-1').subscribe(payments => {
      expect(payments.length).toBe(1);
    });

    const req = httpMock.expectOne(`${apiUrl}/rental/rental-1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPayments);
  });

  it('should create a payment', () => {
    const request = {
      rentalId: 'rental-1',
      paymentType: 'RENT',
      amount: 850,
      currency: 'EUR',
      dueDate: '2025-03-01',
      paymentMethod: 'transfer'
    };
    const mockResponse = { id: 'pay-new', ...request, status: 'PENDING' };

    service.create(request as any).subscribe(payment => {
      expect(payment.id).toBe('pay-new');
      expect(payment.status).toBe('PENDING');
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.rentalId).toBe('rental-1');
    req.flush(mockResponse);
  });

  it('should create Stripe payment intent', () => {
    const mockIntent = { clientSecret: 'pi_secret', intentId: 'pi_123', status: 'requires_payment_method' };

    service.createStripeIntent('pay-1').subscribe(intent => {
      expect(intent.clientSecret).toBe('pi_secret');
    });

    const req = httpMock.expectOne(`${apiUrl}/pay-1/stripe-intent`);
    expect(req.request.method).toBe('POST');
    req.flush(mockIntent);
  });

  it('should confirm payment', () => {
    const mockPayment = { id: 'pay-1', amount: 850, status: 'PAID' };

    service.confirmPayment('pay-1').subscribe(payment => {
      expect(payment.status).toBe('PAID');
    });

    const req = httpMock.expectOne(`${apiUrl}/pay-1/confirm`);
    expect(req.request.method).toBe('POST');
    req.flush(mockPayment);
  });
});
