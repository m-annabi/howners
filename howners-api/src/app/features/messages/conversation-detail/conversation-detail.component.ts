import { Component, OnInit, OnDestroy, OnChanges, SimpleChanges, Input, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { MessageService } from '../../../core/services/message.service';
import { AuthService } from '../../../core/auth/auth.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { Message, CreateMessageRequest, avatarColor } from '../../../core/models/message.model';

/**
 * Fil de conversation, embarqué dans la vue scindée (partnerId fourni en Input).
 */
@Component({
  selector: 'app-conversation-detail',
  templateUrl: './conversation-detail.component.html',
  styleUrls: ['./conversation-detail.component.scss']
})
export class ConversationDetailComponent implements OnInit, OnChanges, OnDestroy, AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  @Input() partnerId: string | null = null;
  @Input() partnerName: string | null = null;

  messages: Message[] = [];
  loading = false;
  currentUserId: string | null = null;
  newMessage = '';
  sending = false;
  listingId: string | null = null;

  readonly avatarColor = avatarColor;

  private userSub?: Subscription;
  private wsSub?: Subscription;
  private shouldScrollBottom = false;

  constructor(
    private route: ActivatedRoute,
    private messageService: MessageService,
    private authService: AuthService,
    private webSocketService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.userSub = this.authService.currentUser$.subscribe(user => {
      this.currentUserId = user?.id || null;
    });

    this.listingId = this.route.snapshot.queryParamMap.get('listingId');

    if (this.partnerId) {
      this.loadMessages();
    }
    this.subscribeToWebSocket();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['partnerId'] && !changes['partnerId'].firstChange) {
      this.messages = [];
      this.newMessage = '';
      if (this.partnerId) this.loadMessages();
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollBottom) {
      this.scrollToBottom();
      this.shouldScrollBottom = false;
    }
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
    this.wsSub?.unsubscribe();
  }

  get displayName(): string {
    if (this.partnerName) return this.partnerName;
    const first = this.messages[0];
    if (!first) return 'Conversation';
    return this.isMyMessage(first) ? first.recipientName : first.senderName;
  }

  loadMessages(): void {
    if (!this.partnerId) return;
    this.loading = this.messages.length === 0;
    this.messageService.getConversation(this.partnerId).subscribe({
      next: (messages) => {
        this.messages = messages;
        this.loading = false;
        this.shouldScrollBottom = true;
        this.markUnreadAsRead();
      },
      error: () => this.loading = false
    });
  }

  send(): void {
    if (!this.newMessage.trim() || !this.partnerId) return;

    this.sending = true;
    const request: CreateMessageRequest = {
      recipientId: this.partnerId,
      body: this.newMessage.trim()
    };

    if (this.listingId) {
      request.listingId = this.listingId;
    }

    this.messageService.send(request).subscribe({
      next: (msg) => {
        this.messages.push(msg);
        this.newMessage = '';
        this.sending = false;
        this.listingId = null;
        this.shouldScrollBottom = true;
      },
      error: () => this.sending = false
    });
  }

  isMyMessage(msg: Message): boolean {
    return msg.senderId === this.currentUserId;
  }

  /** Sépare les messages par jour : vrai si le message ouvre une nouvelle journée. */
  isNewDay(index: number): boolean {
    if (index === 0) return true;
    return new Date(this.messages[index].createdAt).toDateString()
      !== new Date(this.messages[index - 1].createdAt).toDateString();
  }

  dayLabel(msg: Message): string {
    const date = new Date(msg.createdAt);
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);
    if (date.toDateString() === today.toDateString()) return "Aujourd'hui";
    if (date.toDateString() === yesterday.toDateString()) return 'Hier';
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  private subscribeToWebSocket(): void {
    this.wsSub = this.webSocketService.messages$.subscribe(msg => {
      // Only show messages belonging to this conversation
      if (msg.senderId === this.partnerId || msg.recipientId === this.partnerId) {
        const alreadyPresent = this.messages.some(m => m.id === msg.id);
        if (!alreadyPresent) {
          this.messages.push(msg);
          this.shouldScrollBottom = true;
          if (msg.recipientId === this.currentUserId) {
            this.messageService.markAsRead(msg.id).subscribe({ error: () => {} });
          }
        }
      }
    });
  }

  private markUnreadAsRead(): void {
    this.messages
      .filter(m => !m.isRead && m.recipientId === this.currentUserId)
      .forEach(m => {
        this.messageService.markAsRead(m.id).subscribe({
          next: () => m.isRead = true,
          error: () => {}
        });
      });
  }

  private scrollToBottom(): void {
    try {
      this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
    } catch {}
  }
}
