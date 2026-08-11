import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { NotificationService } from '../services/notification.service';
import { UpgradeModalService } from '../services/upgrade-modal.service';
import { StorageService } from '../services/storage.service';
import { Router } from '@angular/router';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(
    private notificationService: NotificationService,
    private upgradeModalService: UpgradeModalService,
    private storageService: StorageService,
    private router: Router
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        let errorMessage = 'Une erreur est survenue';

        if (error.error instanceof ErrorEvent) {
          // Erreur côté client
          errorMessage = `Erreur: ${error.error.message}`;
        } else {
          // Erreur côté serveur
          if (error.error && error.error.message) {
            errorMessage = error.error.message;
          } else if (error.message) {
            errorMessage = error.message;
          }

          // Gestion spécifique selon le code d'erreur
          switch (error.status) {
            case 401:
              // Échec de connexion/inscription : le formulaire affiche lui-même
              // le message du serveur — pas de toast global (et surtout pas
              // « Session expirée » pour un mauvais mot de passe).
              if (request.url.includes('/auth/login') || request.url.includes('/auth/register')) {
                return throwError(() => error);
              }
              // Session expirée : traitée UNE seule fois par rafale. La première
              // 401 purge le jeton ; les 401 concurrentes (polling, requêtes
              // parallèles du dashboard) ne trouvent plus de jeton et se taisent.
              if (this.storageService.getItem('access_token')) {
                this.storageService.removeItem('access_token');
                this.notificationService.error('Session expirée. Veuillez vous reconnecter.');
                this.router.navigate(['/auth/login']);
              }
              return throwError(() => error);
            case 402:
              errorMessage = error.error?.message || 'Cette fonctionnalité nécessite un plan supérieur';
              this.upgradeModalService.show(errorMessage);
              return throwError(() => error);
            case 403:
              errorMessage = error.error?.message || "Vous n'avez pas les droits nécessaires pour effectuer cette action";
              break;
            case 404:
              // Skip notification for optional resource endpoints where a 404 is
              // an expected "not set yet" case, handled locally by the component
              // (primary photo, contract with no e-signature request yet…).
              if (request.url.includes('/photos/primary') ||
                  request.url.includes('/esignature/status')) {
                return throwError(() => error);
              }
              errorMessage = error.error?.message || 'Ressource non trouvée';
              break;
            case 409:
              errorMessage = error.error?.message || 'Conflit détecté';
              break;
            case 500:
              errorMessage = error.error?.message || 'Erreur serveur. Veuillez réessayer plus tard.';
              break;
            case 0:
              errorMessage = 'Impossible de contacter le serveur. Vérifiez votre connexion.';
              break;
          }
        }

        // Afficher la notification d'erreur
        this.notificationService.error(errorMessage);

        return throwError(() => error);
      })
    );
  }
}
