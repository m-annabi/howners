import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Message, Conversation, CreateMessageRequest } from '../models/message.model';

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private apiUrl = `${environment.apiUrl}/messages`;

  constructor(private http: HttpClient) {}

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
}
