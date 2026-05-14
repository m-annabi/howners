import { Component, OnInit } from '@angular/core';
import { InvitationService } from '../../../core/services/invitation.service';
import {
  Invitation,
  INVITATION_STATUS_LABELS,
  INVITATION_STATUS_COLORS
} from '../../../core/models/invitation.model';

@Component({
  selector: 'app-sent-invitations',
  templateUrl: './sent-invitations.component.html',
  styleUrls: ['./sent-invitations.component.scss']
})
export class SentInvitationsComponent implements OnInit {
  invitations: Invitation[] = [];
  loading = false;
  statusLabels = INVITATION_STATUS_LABELS;
  statusColors = INVITATION_STATUS_COLORS;

  constructor(private invitationService: InvitationService) {}

  ngOnInit(): void {
    this.loadInvitations();
  }

  loadInvitations(): void {
    this.loading = true;
    this.invitationService.getSentInvitations().subscribe({
      next: (invitations) => {
        this.invitations = invitations;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }
}
