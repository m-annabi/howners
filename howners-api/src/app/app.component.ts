import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { SwUpdate, VersionReadyEvent } from '@angular/service-worker';
import { filter } from 'rxjs/operators';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  title = 'howners-api';
  isAuthenticated = false;
  sidebarOpen = false;
  /** Barre de navigation invité (pages publiques hors landing/auth/signature). */
  showGuestNav = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private swUpdate: SwUpdate
  ) {}

  ngOnInit(): void {
    // Applique immédiatement toute nouvelle version déployée : sans ça, le
    // service worker ressert l'ancien bundle jusqu'à un rechargement forcé
    // manuel (symptôme : des bugs déjà corrigés qui « persistent »).
    if (this.swUpdate.isEnabled) {
      this.swUpdate.versionUpdates.pipe(
        filter((e): e is VersionReadyEvent => e.type === 'VERSION_READY')
      ).subscribe(() => document.location.reload());
      this.swUpdate.unrecoverable.subscribe(() => document.location.reload());
    }

    this.authService.isAuthenticated$.subscribe(
      isAuth => this.isAuthenticated = isAuth
    );

    this.showGuestNav = this.computeGuestNav(this.router.url);
    this.router.events.pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(e => this.showGuestNav = this.computeGuestNav((e as NavigationEnd).urlAfterRedirects));
  }

  /**
   * La landing porte déjà sa propre navigation ; les parcours auth et signature
   * tokenisée gèrent leur propre mise en page. Partout ailleurs (annonces, 404…),
   * un visiteur doit pouvoir revenir au menu principal.
   */
  private computeGuestNav(url: string): boolean {
    const path = (url.split('?')[0].split('#')[0]) || '/';
    if (path === '/' || path === '') return false;
    if (path.startsWith('/auth')) return false;
    if (path.startsWith('/sign')) return false;
    return true;
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }
}
