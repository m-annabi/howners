import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PropertyDetailComponent } from './property-detail.component';
import { PropertyService } from '../property.service';
import { RentalService } from '../../rentals/rental.service';
import { NotificationService } from '../../../core/services/notification.service';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

describe('PropertyDetailComponent', () => {
  let component: PropertyDetailComponent;
  let fixture: ComponentFixture<PropertyDetailComponent>;
  let propertyService: jasmine.SpyObj<PropertyService>;
  let rentalService: jasmine.SpyObj<RentalService>;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let router: Router;

  const mockProperty = {
    id: 'prop-1',
    name: 'Apt Test',
    propertyType: 'APARTMENT',
    address: {
      addressLine1: '10 Rue Test',
      addressLine2: null,
      city: 'Paris',
      postalCode: '75001',
      department: '75',
      country: 'FR'
    },
    surfaceArea: 65,
    bedrooms: 2,
    bathrooms: 1,
    description: 'Nice apartment',
    createdAt: '2024-01-01T00:00:00',
    updatedAt: '2024-01-15T00:00:00',
    dpeRating: null,
    gesRating: null,
    constructionYear: null,
    floorNumber: null,
    totalFloors: null,
    heatingType: null,
    hasParking: null,
    hasElevator: null,
    isFurnished: null,
    propertyCondition: null
  };

  const mockRentals = [
    {
      id: 'rental-1',
      propertyId: 'prop-1',
      status: 'ACTIVE',
      monthlyRent: 1200,
      tenantName: 'Jean Dupont',
      tenantEmail: 'jean@test.com',
      startDate: '2024-01-01',
      endDate: null,
      depositAmount: 1200
    }
  ];

  beforeEach(async () => {
    const propSpy = jasmine.createSpyObj('PropertyService', ['getProperty', 'deleteProperty']);
    const rentalSpy = jasmine.createSpyObj('RentalService', ['getRentals']);
    const notifSpy = jasmine.createSpyObj('NotificationService', ['info', 'error', 'success']);

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule],
      declarations: [PropertyDetailComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [
        { provide: PropertyService, useValue: propSpy },
        { provide: RentalService, useValue: rentalSpy },
        { provide: NotificationService, useValue: notifSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'prop-1' } } }
        }
      ]
    }).compileComponents();

    propertyService = TestBed.inject(PropertyService) as jasmine.SpyObj<PropertyService>;
    rentalService = TestBed.inject(RentalService) as jasmine.SpyObj<RentalService>;
    notificationService = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;
    router = TestBed.inject(Router);
  });

  beforeEach(() => {
    propertyService.getProperty.and.returnValue(of(mockProperty as any));
    rentalService.getRentals.and.returnValue(of(mockRentals as any));

    fixture = TestBed.createComponent(PropertyDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load property on init', () => {
    expect(component.property).toBeTruthy();
    expect(component.property!.name).toBe('Apt Test');
    expect(component.loading).toBeFalse();
  });

  it('should find active rental for property', () => {
    expect(component.activeRental).toBeTruthy();
    expect(component.activeRental!.id).toBe('rental-1');
    expect(component.monthlyRent).toBe(1200);
  });

  it('should handle no active rental', () => {
    rentalService.getRentals.and.returnValue(of([]));
    component.loadPropertyAndRentals('prop-1');

    expect(component.activeRental).toBeNull();
    expect(component.monthlyRent).toBe(0);
  });

  it('should handle error on load', () => {
    propertyService.getProperty.and.returnValue(throwError(() => ({ error: { message: 'Not found' } })));
    rentalService.getRentals.and.returnValue(of([]));

    component.loadPropertyAndRentals('prop-1');

    expect(component.error).toBe('Not found');
    expect(component.loading).toBeFalse();
  });

  it('should navigate to edit', () => {
    spyOn(router, 'navigate');
    component.editProperty();
    expect(router.navigate).toHaveBeenCalledWith(['/properties', 'prop-1', 'edit']);
  });

  it('should navigate to rentals', () => {
    spyOn(router, 'navigate');
    component.viewRentals();
    expect(router.navigate).toHaveBeenCalledWith(['/rentals'], { queryParams: { propertyId: 'prop-1' } });
  });

  it('should navigate to financial stats', () => {
    spyOn(router, 'navigate');
    component.viewStats();
    expect(router.navigate).toHaveBeenCalledWith(['/financial'], { queryParams: { propertyId: 'prop-1' } });
  });

  it('should scroll to documents section', () => {
    const mockElement = jasmine.createSpyObj('Element', ['scrollIntoView']);
    spyOn(document, 'querySelector').and.returnValue(mockElement);

    component.viewDocuments();

    expect(document.querySelector).toHaveBeenCalledWith('app-document-list');
    expect(mockElement.scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' });
  });

  it('should navigate to rental detail', () => {
    spyOn(router, 'navigate');
    component.viewRentalDetail();
    expect(router.navigate).toHaveBeenCalledWith(['/rentals', 'rental-1']);
  });

  it('should delete property after confirmation', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(router, 'navigate');
    propertyService.deleteProperty.and.returnValue(of(void 0));

    component.deleteProperty();

    expect(propertyService.deleteProperty).toHaveBeenCalledWith('prop-1');
    expect(router.navigate).toHaveBeenCalledWith(['/properties']);
  });

  it('should not delete property if not confirmed', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.deleteProperty();

    expect(propertyService.deleteProperty).not.toHaveBeenCalled();
  });

  it('should show error on delete failure', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    propertyService.deleteProperty.and.returnValue(
      throwError(() => ({ error: { message: 'Cannot delete property' } }))
    );

    component.deleteProperty();

    expect(notificationService.error).toHaveBeenCalledWith('Cannot delete property');
  });
});
