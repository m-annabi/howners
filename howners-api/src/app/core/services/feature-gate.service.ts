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
}
