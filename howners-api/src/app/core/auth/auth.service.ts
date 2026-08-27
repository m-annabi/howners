import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, tap } from 'rxjs';
import { catchError, filter, map, take, timeout } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/user.model';
import { StorageService } from '../services/storage.service';
import { WebSocketService } from '../services/websocket.service';
import { MessageService } from '../services/message.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(
    private http: HttpClient,
    private storageService: StorageService,
    private router: Router,
    private webSocketService: WebSocketService,
    private messageService: MessageService
  ) {
    this.loadUserFromStorage();
  }

  /**
   * L'inscription n'auto-connecte plus : le compte doit être activé via le lien reçu par e-mail.
   * La réponse est un simple message générique (aucun jeton, aucune info sur l'existence du compte).
   */
  register(request: RegisterRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.API_URL}/register`, request);
  }

  /** Confirme l'adresse à partir du token du lien e-mail. */
  verifyEmail(token: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.API_URL}/verify-email`, { token });
  }

  /** Renvoie un lien de vérification (réponse générique). */
  resendVerification(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.API_URL}/resend-verification`, { email });
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, request)
      .pipe(
        tap(response => this.handleAuthResponse(response))
      );
  }

  logout(): void {
    this.webSocketService.disconnect();
    this.storageService.removeItem('access_token');
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/auth/login']);
  }

  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.API_URL}/me`);
  }

  updateCurrentUser(request: { firstName: string; lastName: string; email: string; phone?: string }): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/me`, request).pipe(
      tap(user => this.currentUserSubject.next(user))
    );
  }

  isAuthenticated(): boolean {
    return !!this.storageService.getItem('access_token');
  }

  hasRole(role: string): boolean {
    const user = this.currentUserSubject.value;
    return user?.role === role;
  }

  /**
   * Accueil selon le rôle : un locataire atterrit dans SON espace (jamais sur le
   * tableau de bord propriétaire) ; propriétaire, admin et concierge sur /dashboard.
   */
  homePathFor(user: User | null): string {
    return user?.role === 'TENANT' ? '/tenant/dashboard' : '/dashboard';
  }

  /**
   * Résout l'accueil du compte courant en attendant le chargement du profil
   * (sur un refresh, /auth/me est asynchrone : le rôle n'est pas connu tout de suite).
   */
  resolveHomePath(): Observable<string> {
    return this.currentUser$.pipe(
      filter((user): user is User => user !== null),
      take(1),
      timeout(5000),
      map(user => this.homePathFor(user)),
      catchError(() => of('/auth/login'))
    );
  }

  private handleAuthResponse(response: AuthResponse): void {
    this.storageService.setItem('access_token', response.accessToken);
    this.currentUserSubject.next(response.user);
    this.isAuthenticatedSubject.next(true);
    this.webSocketService.connect(response.accessToken);
    this.messageService.refreshUnreadCount();
  }

  private loadUserFromStorage(): void {
    const token = this.storageService.getItem('access_token');
    if (token) {
      this.isAuthenticatedSubject.next(true);
      this.getCurrentUser().subscribe({
        next: user => {
          this.currentUserSubject.next(user);
          this.webSocketService.connect(token);
          this.messageService.refreshUnreadCount();
        },
        error: () => this.logout()
      });
    } else {
      this.isAuthenticatedSubject.next(false);
    }
  }
}
