import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { filter, map, take, timeout, catchError } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean | UrlTree> {
    const requiredRoles = route.data['roles'] as string[];

    if (!requiredRoles || requiredRoles.length === 0) {
      return of(true);
    }

    // Attend que le profil soit chargé (important sur refresh : le HTTP /auth/me
    // est asynchrone, currentUser$ vaut null jusqu'à sa résolution)
    return this.authService.currentUser$.pipe(
      filter(user => user !== null),
      take(1),
      timeout(5000),
      map(user => {
        const hasRole = requiredRoles.some(role => user?.role === role);
        return hasRole ? true : this.router.createUrlTree(['/dashboard']);
      }),
      catchError(() => of(this.router.createUrlTree(['/auth/login'])))
    );
  }
}
