import { Component, OnInit } from '@angular/core';
import { InvitationService } from '../../../core/services/invitation.service';
import { Invitation, InvitationStatus, INVITATION_STATUS_LABELS, INVITATION_STATUS_COLORS } from '../../../core/models/invitation.model';

@Component({
  selector: 'app-my-invitations',
  templateUrl: './my-invitations.component.html',
  styleUrls: ['./my-invitations.component.scss']
})
export class MyInvitationsComponent implements OnInit {
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
    this.invitationService.getReceivedInvitations().subscribe({
      next: (invitations) => {
        this.invitations = invitations;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  decline(invitation: Invitation): void {
    this.invitationService.updateStatus(invitation.id, InvitationStatus.DECLINED).subscribe({
      next: (updated) => {
        const idx = this.invitations.findIndex(i => i.id === updated.id);
        if (idx >= 0) this.invitations[idx] = updated;
      },
      error: () => {} // silent — status unchanged in UI
    });
  }

  markViewed(invitation: Invitation): void {
    if (invitation.status === InvitationStatus.PENDING) {
      this.invitationService.updateStatus(invitation.id, InvitationStatus.VIEWED).subscribe({
        next: (updated) => {
          const idx = this.invitations.findIndex(i => i.id === updated.id);
          if (idx >= 0) this.invitations[idx] = updated;
        },
        error: () => {} // silent — best effort
      });
    }
  }
}
