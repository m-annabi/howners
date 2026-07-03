import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription, combineLatest } from 'rxjs';
import { MessageService } from '../../../core/services/message.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { Conversation, avatarColor } from '../../../core/models/message.model';

/**
 * Page messagerie en vue scindée : liste des conversations à gauche,
 * fil actif à droite (app-conversation-detail embarqué).
 */
@Component({
  selector: 'app-conversation-list',
  templateUrl: './conversation-list.component.html',
  styleUrls: ['./conversation-list.component.scss']
})
export class ConversationListComponent implements OnInit, OnDestroy {
  conversations: Conversation[] = [];
  loading = false;
  searchTerm = '';
  activePartnerId: string | null = null;

  readonly avatarColor = avatarColor;

  private wsSub?: Subscription;
  private routeSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private messageService: MessageService,
    private webSocketService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.loadConversations();

    this.routeSub = combineLatest([this.route.paramMap, this.route.queryParamMap])
      .subscribe(([params, query]) => {
        this.activePartnerId = params.get('userId') ?? query.get('recipientId');
      });

    this.wsSub = this.webSocketService.messages$.subscribe(() => {
      this.loadConversations();
    });
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.routeSub?.unsubscribe();
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

  get filteredConversations(): Conversation[] {
    if (!this.searchTerm.trim()) return this.conversations;
    const term = this.searchTerm.toLowerCase();
    return this.conversations.filter(c =>
      c.partnerName.toLowerCase().includes(term) ||
      (c.lastMessageBody || '').toLowerCase().includes(term)
    );
  }

  get activePartnerName(): string | null {
    return this.conversations.find(c => c.partnerId === this.activePartnerId)?.partnerName ?? null;
  }

  trackByPartner(_: number, conv: Conversation): string { return conv.partnerId; }
}
