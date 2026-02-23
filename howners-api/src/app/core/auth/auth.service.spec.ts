import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { StorageService } from '../services/storage.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let storageService: jasmine.SpyObj<StorageService>;
  let router: Router;

  beforeEach(() => {
    const storageSpy = jasmine.createSpyObj('StorageService', ['getItem', 'setItem', 'removeItem', 'clear']);
    storageSpy.getItem.and.returnValue(null);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [
        AuthService,
        { provide: StorageService, useValue: storageSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    storageService = TestBed.inject(StorageService) as jasmine.SpyObj<StorageService>;
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should return false for isAuthenticated when no token', () => {
    storageService.getItem.and.returnValue(null);
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('should return true for isAuthenticated when token exists', () => {
    storageService.getItem.and.returnValue('some-jwt-token');
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('should login and store token', () => {
    const mockResponse = {
      accessToken: 'jwt-token-123',
      user: { id: '1', email: 'test@test.com', firstName: 'Test', lastName: 'User', role: 'OWNER' }
    };

    service.login({ email: 'test@test.com', password: 'pass123' }).subscribe(response => {
      expect(response.accessToken).toBe('jwt-token-123');
      expect(storageService.setItem).toHaveBeenCalledWith('access_token', 'jwt-token-123');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'test@test.com', password: 'pass123' });
    req.flush(mockResponse);
  });

  it('should register and store token', () => {
    const mockResponse = {
      accessToken: 'jwt-token-456',
      user: { id: '2', email: 'new@test.com', firstName: 'New', lastName: 'User', role: 'OWNER' }
    };

    service.register({
      email: 'new@test.com', password: 'pass123',
      firstName: 'New', lastName: 'User', role: 'OWNER'
    }).subscribe(response => {
      expect(response.accessToken).toBe('jwt-token-456');
      expect(storageService.setItem).toHaveBeenCalledWith('access_token', 'jwt-token-456');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/register`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should logout and clear token', () => {
    spyOn(router, 'navigate');

    service.logout();

    expect(storageService.removeItem).toHaveBeenCalledWith('access_token');
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('should check role correctly', () => {
    // Initially no user, hasRole should be false
    expect(service.hasRole('OWNER')).toBeFalse();
    expect(service.hasRole('TENANT')).toBeFalse();
  });

  it('should fetch current user', () => {
    const mockUser = { id: '1', email: 'test@test.com', firstName: 'Test', lastName: 'User', role: 'OWNER' };

    service.getCurrentUser().subscribe(user => {
      expect(user.email).toBe('test@test.com');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/me`);
    expect(req.request.method).toBe('GET');
    req.flush(mockUser);
  });
});
