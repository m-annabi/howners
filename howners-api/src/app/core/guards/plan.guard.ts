import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, UrlTree } from '@angular/router';
import { Observable, catchError, map, of } from 'rxjs';
import { SubscriptionService } from '../services/subscription.service';

/**
 * Réserve une route aux plans payants (PRO et supérieurs).
 * Les utilisateurs FREE sont redirigés vers la page de présentation
 * de la fonctionnalité indiquée par route.data['proFeature'].
 */
@Injectable({
  providedIn: 'root'
})
export class PlanGuard implements CanActivate {

  constructor(
    private subscriptionService: SubscriptionService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean | UrlTree> {
    const feature = route.data['proFeature'];

    return this.subscriptionService.getUsageLimits().pipe(
      map(usage => {
        if ((usage.planName || 'FREE') !== 'FREE') {
          return true;
        }
        return this.router.createUrlTree(['/billing/upgrade', feature]);
      }),
      // En cas d'erreur on laisse passer : le backend applique de toute façon la restriction plan.
      catchError(() => of(true))
    );
  }
}
