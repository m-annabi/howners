import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ConversationListComponent } from './conversation-list/conversation-list.component';
import { ConversationDetailComponent } from './conversation-detail/conversation-detail.component';

const routes: Routes = [
  { path: '', component: ConversationListComponent },
  { path: 'new', component: ConversationDetailComponent },
  { path: ':userId', component: ConversationDetailComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MessagesRoutingModule { }
