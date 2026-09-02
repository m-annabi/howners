import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../auth/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    if (this.authService.isAuthenticated()) {
      return true;
    }

    // Conserve la destination : après connexion, l'utilisateur revient là où il
    // voulait aller (ex. « Candidater » sur une annonce) au lieu du tableau de bord.
    return this.router.createUrlTree(['/auth/login'], {
      queryParams: state.url && state.url !== '/' ? { returnUrl: state.url } : {}
    });
  }
}
