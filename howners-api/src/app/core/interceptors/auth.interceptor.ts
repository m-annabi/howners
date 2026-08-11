import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { StorageService } from '../services/storage.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private storageService: StorageService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.storageService.getItem('access_token');

    if (token && !request.url.includes('/auth/login') && !request.url.includes('/auth/register')) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    // La gestion des 401 (session expirée) est centralisée dans ErrorInterceptor :
    // la dupliquer ici provoquait une seconde navigation par requête en échec.
    return next.handle(request);
  }
}
