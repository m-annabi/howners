import { Injectable } from '@angular/core';
import { Observable, of, map, catchError } from 'rxjs';
import { SubscriptionService } from './subscription.service';

@Injectable({
  providedIn: 'root'
})
export class FeatureGateService {

  constructor(private subscriptionService: SubscriptionService) {}

  canCreateProperty(): Observable<boolean> {
    return this.subscriptionService.getUsageLimits().pipe(
      map(limits => limits.canCreateProperty),
      catchError(() => of(true))
    );
  }

  canCreateContract(): Observable<boolean> {
    return this.subscriptionService.getUsageLimits().pipe(
      map(limits => limits.canCreateContract),
      catchError(() => of(true))
    );
  }

  canCreateRental(): Observable<boolean> {
    return this.subscriptionService.getUsageLimits().pipe(
      map(limits => limits.canCreateRental),
      catchError(() => of(true))
    );
  }

  canCreateListing(): Observable<boolean> {
    return this.subscriptionService.getUsageLimits().pipe(
      map(limits => limits.canCreateListing),
      catchError(() => of(true))
    );
  }

  hasESignature(): Observable<boolean> {
    return this.subscriptionService.getUsageLimits().pipe(
      map(limits => limits.hasESignature),
      catchError(() => of(false))
    );
  }

  hasTenantScoring(): Observable<boolean> {
    return this.subscriptionService.getUsageLimits().pipe(
      map(limits => limits.hasTenantScoring),
      catchError(() => of(false))
    );
  }

  hasDocumentEncryption(): Observable<boolean> {
    return this.subscriptionService.getUsageLimits().pipe(
      map(limits => limits.hasDocumentEncryption),
      catchError(() => of(false))
    );
  }
}
