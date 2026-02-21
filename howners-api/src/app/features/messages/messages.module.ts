import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { MessagesRoutingModule } from './messages-routing.module';
import { ConversationListComponent } from './conversation-list/conversation-list.component';
import { ConversationDetailComponent } from './conversation-detail/conversation-detail.component';

@NgModule({
  declarations: [
    ConversationListComponent,
    ConversationDetailComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    MessagesRoutingModule
  ]
})
export class MessagesModule { }
