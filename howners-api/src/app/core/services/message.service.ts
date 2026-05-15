import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Message, Conversation, CreateMessageRequest } from '../models/message.model';
import { WebSocketService } from './websocket.service';

@Injectable({ providedIn: 'root' })
export class MessageService implements OnDestroy {
  private apiUrl = `${environment.apiUrl}/messages`;

  private unreadCountSubject = new BehaviorSubject<number>(0);
  readonly unreadCount$ = this.unreadCountSubject.asObservable();

  private wsSub?: Subscription;

  constructor(
    private http: HttpClient,
    private webSocketService: WebSocketService
  ) {
    this.wsSub = this.webSocketService.messages$.subscribe(() => {
      this.unreadCountSubject.next(this.unreadCountSubject.value + 1);
    });
  }

  refreshUnreadCount(): void {
    this.getUnreadCount().subscribe({
      next: res => this.unreadCountSubject.next(res.count),
      error: () => {}
    });
  }

  resetUnreadCount(): void {
    this.unreadCountSubject.next(0);
  }

  send(request: CreateMessageRequest): Observable<Message> {
    return this.http.post<Message>(this.apiUrl, request);
  }

  getConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(`${this.apiUrl}/conversations`);
  }

  getConversation(userId: string): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.apiUrl}/conversation/${userId}`);
  }

  markAsRead(messageId: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${messageId}/read`, {});
  }

  getUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/unread-count`);
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }
}
