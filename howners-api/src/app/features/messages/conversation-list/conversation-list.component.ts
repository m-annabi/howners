import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { MessageService } from '../../../core/services/message.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { Conversation } from '../../../core/models/message.model';

@Component({
  selector: 'app-conversation-list',
  templateUrl: './conversation-list.component.html'
})
export class ConversationListComponent implements OnInit, OnDestroy {
  conversations: Conversation[] = [];
  loading = false;
  private wsSub?: Subscription;

  constructor(
    private messageService: MessageService,
    private webSocketService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.loadConversations();
    this.wsSub = this.webSocketService.messages$.subscribe(() => {
      this.loadConversations();
    });
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }

  loadConversations(): void {
    this.loading = this.conversations.length === 0;
    this.messageService.getConversations().subscribe({
      next: (conversations) => {
        this.conversations = conversations;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }
}
