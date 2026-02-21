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
import { Router } from '@angular/router';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(
    private notificationService: NotificationService,
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
              errorMessage = 'Session expirée. Veuillez vous reconnecter.';
              this.router.navigate(['/auth/login']);
              break;
            case 403:
              errorMessage = error.error?.message || "Vous n'avez pas les droits nécessaires pour effectuer cette action";
              break;
            case 404:
              // Skip notification for optional resource endpoints (e.g. primary photo)
              if (request.url.includes('/photos/primary')) {
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
