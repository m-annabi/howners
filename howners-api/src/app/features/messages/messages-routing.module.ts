import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ConversationListComponent } from './conversation-list/conversation-list.component';

// Vue scindée : la page liste héberge aussi le fil actif
// (userId en paramètre, ou recipientId en query pour /new)
const routes: Routes = [
  { path: '', component: ConversationListComponent },
  { path: 'new', component: ConversationListComponent },
  { path: ':userId', component: ConversationListComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MessagesRoutingModule { }
