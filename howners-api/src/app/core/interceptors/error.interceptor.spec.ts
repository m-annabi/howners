import { HttpErrorResponse, HttpHandler, HttpRequest } from '@angular/common/http';
import { throwError } from 'rxjs';
import { ErrorInterceptor } from './error.interceptor';

describe('ErrorInterceptor recovery', () => {
  let notifications: any;
  let storage: any;
  let router: any;
  let interceptor: ErrorInterceptor;
  beforeEach(() => {
    notifications = jasmine.createSpyObj('notifications', ['error']);
    storage = jasmine.createSpyObj('storage', ['getItem', 'removeItem']);
    router = { url: '/payments/123', navigate: jasmine.createSpy('navigate') };
    interceptor = new ErrorInterceptor(notifications, jasmine.createSpyObj('upgrade', ['show']), storage, router);
  });
  function fail(status: number, url = '/api/payments'): void {
    const handler = { handle: () => throwError(() => new HttpErrorResponse({ status })) } as HttpHandler;
    interceptor.intercept(new HttpRequest('GET', url), handler).subscribe({ error: () => {} });
  }
  it('preserves the current page on session expiration', () => {
    storage.getItem.and.returnValue('token');
    fail(401);
    expect(storage.removeItem).toHaveBeenCalledWith('access_token');
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login'], { queryParams: { returnUrl: '/payments/123' } });
  });
  it('does not redirect for invalid login credentials', () => {
    fail(401, '/api/auth/login');
    expect(router.navigate).not.toHaveBeenCalled();
  });
  it('asks to verify the result before repeating a failed mutation', () => {
    fail(503);
    expect(notifications.error.calls.mostRecent().args[0]).toContain('vérifier si votre action a abouti');
  });
  it('explains rate limiting', () => {
    fail(429);
    expect(notifications.error.calls.mostRecent().args[0]).toContain('Patientez');
  });
});
