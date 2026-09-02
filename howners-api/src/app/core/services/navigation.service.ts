import { Injectable } from '@angular/core';
import { Location } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

/**
 * Retour « page précédente » cohérent dans toute l'application.
 *
 * Les boutons Retour renvoyaient chacun vers la liste parente, ce qui perdait le contexte
 * (ex. : bail → paiement → Retour ramenait à la liste des paiements, pas au bail).
 * On revient dans l'historique de navigation interne quand il existe, sinon vers la
 * destination de repli (arrivée directe par URL, lien externe, rechargement).
 */
@Injectable({ providedIn: 'root' })
export class NavigationService {
  private readonly history: string[] = [];

  constructor(private router: Router, private location: Location) {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => {
        this.history.push(e.urlAfterRedirects);
        if (this.history.length > 50) this.history.shift();
      });
  }

  /** Revient à la page précédente de l'application, ou vers `fallback` s'il n'y en a pas. */
  back(fallback: any[]): void {
    if (this.history.length > 1 || this.hasEarlierInAppNavigation()) {
      this.history.pop();
      this.location.back();
    } else {
      this.router.navigate(fallback);
    }
  }

  /**
   * Le routeur Angular numérote ses navigations dans history.state.navigationId : un id > 1
   * signifie qu'une navigation interne a précédé celle-ci, même si ce service a été instancié
   * tard (module chargé à la demande).
   */
  private hasEarlierInAppNavigation(): boolean {
    if (typeof window === 'undefined') return false;
    const id = window.history.state?.navigationId;
    return typeof id === 'number' && id > 1;
  }
}
