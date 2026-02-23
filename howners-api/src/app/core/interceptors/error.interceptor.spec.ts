import { TestBed } from '@angular/core/testing';
import { HTTP_INTERCEPTORS, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { ErrorInterceptor } from './error.interceptor';
import { NotificationService } from '../services/notification.service';

describe('ErrorInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let router: Router;

  beforeEach(() => {
    const notifSpy = jasmine.createSpyObj('NotificationService', ['error', 'success', 'info']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [
        { provide: NotificationService, useValue: notifSpy },
        { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true }
      ]
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
    notificationService = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should pass through successful requests', () => {
    httpClient.get('/api/test').subscribe(response => {
      expect(response).toEqual({ data: 'ok' });
    });

    const req = httpMock.expectOne('/api/test');
    req.flush({ data: 'ok' });

    expect(notificationService.error).not.toHaveBeenCalled();
  });

  it('should show error notification on 500', () => {
    httpClient.get('/api/test').subscribe({
      error: (error: HttpErrorResponse) => {
        expect(error.status).toBe(500);
      }
    });

    const req = httpMock.expectOne('/api/test');
    req.flush({ message: 'Server error' }, { status: 500, statusText: 'Internal Server Error' });

    expect(notificationService.error).toHaveBeenCalled();
  });

  it('should redirect to login on 401', () => {
    spyOn(router, 'navigate');

    httpClient.get('/api/test').subscribe({
      error: (error: HttpErrorResponse) => {
        expect(error.status).toBe(401);
      }
    });

    const req = httpMock.expectOne('/api/test');
    req.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('should show forbidden message on 403', () => {
    httpClient.get('/api/test').subscribe({
      error: (error: HttpErrorResponse) => {
        expect(error.status).toBe(403);
      }
    });

    const req = httpMock.expectOne('/api/test');
    req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

    expect(notificationService.error).toHaveBeenCalled();
  });

  it('should show not found message on 404', () => {
    httpClient.get('/api/test').subscribe({
      error: (error: HttpErrorResponse) => {
        expect(error.status).toBe(404);
      }
    });

    const req = httpMock.expectOne('/api/test');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    expect(notificationService.error).toHaveBeenCalled();
  });

  it('should skip notification for primary photo 404', () => {
    httpClient.get('/api/properties/123/photos/primary').subscribe({
      error: (error: HttpErrorResponse) => {
        expect(error.status).toBe(404);
      }
    });

    const req = httpMock.expectOne('/api/properties/123/photos/primary');
    req.flush({}, { status: 404, statusText: 'Not Found' });

    expect(notificationService.error).not.toHaveBeenCalled();
  });

  it('should show conflict message on 409', () => {
    httpClient.get('/api/test').subscribe({
      error: (error: HttpErrorResponse) => {
        expect(error.status).toBe(409);
      }
    });

    const req = httpMock.expectOne('/api/test');
    req.flush({ message: 'Conflict' }, { status: 409, statusText: 'Conflict' });

    expect(notificationService.error).toHaveBeenCalled();
  });
});
