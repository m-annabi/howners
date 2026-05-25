import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { Client, IFrame, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Message } from '../models/message.model';

export type WsConnectionState = 'disconnected' | 'connecting' | 'connected';

@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {

  private client!: Client;
  private messageSubscription?: StompSubscription;

  private connectionState$ = new BehaviorSubject<WsConnectionState>('disconnected');
  private incomingMessages$ = new Subject<Message>();

  constructor(private ngZone: NgZone) {}

  get connected$(): Observable<WsConnectionState> {
    return this.connectionState$.asObservable();
  }

  get messages$(): Observable<Message> {
    return this.incomingMessages$.asObservable();
  }

  connect(token: string): void {
    if (this.client?.active) return;

    this.connectionState$.next('connecting');

    this.client = new Client({
      brokerURL: `${environment.wsUrl.replace('http', 'ws')}/ws`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        this.ngZone.run(() => {
          this.connectionState$.next('connected');
          this.subscribeToMessages();
        });
      },
      onDisconnect: () => {
        this.ngZone.run(() => this.connectionState$.next('disconnected'));
      },
      onStompError: (frame: IFrame) => {
        this.ngZone.run(() => {
          console.error('WebSocket STOMP error', frame);
          this.connectionState$.next('disconnected');
        });
      }
    });

    this.client.activate();
  }

  disconnect(): void {
    this.messageSubscription?.unsubscribe();
    this.client?.deactivate();
    this.connectionState$.next('disconnected');
  }

  private subscribeToMessages(): void {
    this.messageSubscription = this.client.subscribe('/user/queue/messages', (frame: IMessage) => {
      try {
        const message: Message = JSON.parse(frame.body);
        this.ngZone.run(() => this.incomingMessages$.next(message));
      } catch {
        console.error('Failed to parse incoming WebSocket message');
      }
    });
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
