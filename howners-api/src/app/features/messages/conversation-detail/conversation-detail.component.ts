import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MessageService } from '../../../core/services/message.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Message, CreateMessageRequest } from '../../../core/models/message.model';

@Component({
  selector: 'app-conversation-detail',
  templateUrl: './conversation-detail.component.html'
})
export class ConversationDetailComponent implements OnInit, OnDestroy {
  messages: Message[] = [];
  loading = false;
  partnerId: string | null = null;
  currentUserId: string | null = null;
  newMessage = '';
  sending = false;
  listingId: string | null = null;
  private refreshInterval: any;

  constructor(
    private route: ActivatedRoute,
    private messageService: MessageService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUserId = user?.id || null;
    });

    this.partnerId = this.route.snapshot.paramMap.get('userId');
    this.listingId = this.route.snapshot.queryParamMap.get('listingId');

    // For /messages/new with recipientId query param
    if (!this.partnerId) {
      this.partnerId = this.route.snapshot.queryParamMap.get('recipientId');
    }

    if (this.partnerId) {
      this.loadMessages();
      this.refreshInterval = setInterval(() => this.loadMessages(), 10000);
    }
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  loadMessages(): void {
    if (!this.partnerId) return;
    this.loading = this.messages.length === 0;
    this.messageService.getConversation(this.partnerId).subscribe({
      next: (messages) => {
        this.messages = messages;
        this.loading = false;
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
      },
      error: () => this.sending = false
    });
  }

  isMyMessage(msg: Message): boolean {
    return msg.senderId === this.currentUserId;
  }

  private markUnreadAsRead(): void {
    this.messages
      .filter(m => !m.isRead && m.recipientId === this.currentUserId)
      .forEach(m => {
        this.messageService.markAsRead(m.id).subscribe(() => m.isRead = true);
      });
  }
}
