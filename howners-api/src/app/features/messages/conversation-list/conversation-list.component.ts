import { Component, OnInit } from '@angular/core';
import { MessageService } from '../../../core/services/message.service';
import { Conversation } from '../../../core/models/message.model';

@Component({
  selector: 'app-conversation-list',
  templateUrl: './conversation-list.component.html'
})
export class ConversationListComponent implements OnInit {
  conversations: Conversation[] = [];
  loading = false;

  constructor(private messageService: MessageService) {}

  ngOnInit(): void {
    this.loadConversations();
  }

  loadConversations(): void {
    this.loading = true;
    this.messageService.getConversations().subscribe({
      next: (conversations) => {
        this.conversations = conversations;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }
}
