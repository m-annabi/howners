import { Component, Input, OnChanges } from '@angular/core';

interface StatusConfig {
  label: string;
  icon: string;
  color: 'success' | 'warning' | 'danger' | 'neutral' | 'info' | 'primary';
}

const STATUS_MAP: Record<string, Record<string, StatusConfig>> = {
  payment: {
    PAID: { label: 'Payé', icon: 'bi-check-circle-fill', color: 'success' },
    PENDING: { label: 'En attente', icon: 'bi-clock', color: 'warning' },
    LATE: { label: 'En retard', icon: 'bi-exclamation-triangle-fill', color: 'danger' },
    OVERDUE: { label: 'Impayé', icon: 'bi-x-circle-fill', color: 'danger' },
    FAILED: { label: 'Échoué', icon: 'bi-x-circle-fill', color: 'danger' },
    CANCELLED: { label: 'Annulé', icon: 'bi-dash-circle', color: 'neutral' },
    PARTIAL: { label: 'Partiel', icon: 'bi-circle-half', color: 'info' },
  },
  contract: {
    ACTIVE: { label: 'Actif', icon: 'bi-check-circle-fill', color: 'success' },
    SIGNED: { label: 'Signé', icon: 'bi-pen-fill', color: 'success' },
    DRAFT: { label: 'Brouillon', icon: 'bi-pencil', color: 'warning' },
    PENDING: { label: 'En attente', icon: 'bi-clock', color: 'warning' },
    PENDING_SIGNATURE: { label: 'Signature en attente', icon: 'bi-clock', color: 'warning' },
    EXPIRED: { label: 'Expiré', icon: 'bi-calendar-x', color: 'neutral' },
    CANCELLED: { label: 'Annulé', icon: 'bi-x-circle', color: 'neutral' },
    TERMINATED: { label: 'Résilié', icon: 'bi-x-circle-fill', color: 'danger' },
  },
  rental: {
    ACTIVE: { label: 'Active', icon: 'bi-check-circle-fill', color: 'success' },
    PENDING: { label: 'En attente', icon: 'bi-clock', color: 'warning' },
    ENDED: { label: 'Terminée', icon: 'bi-calendar-x', color: 'neutral' },
    CANCELLED: { label: 'Annulée', icon: 'bi-x-circle', color: 'neutral' },
  },
  invoice: {
    PAID: { label: 'Payée', icon: 'bi-check-circle-fill', color: 'success' },
    PENDING: { label: 'En attente', icon: 'bi-clock', color: 'warning' },
    SENT: { label: 'Envoyée', icon: 'bi-send', color: 'info' },
    OVERDUE: { label: 'En retard', icon: 'bi-exclamation-triangle-fill', color: 'danger' },
    DRAFT: { label: 'Brouillon', icon: 'bi-pencil', color: 'neutral' },
    CANCELLED: { label: 'Annulée', icon: 'bi-x-circle', color: 'neutral' },
  },
  application: {
    PENDING: { label: 'En attente', icon: 'bi-clock', color: 'warning' },
    UNDER_REVIEW: { label: 'En cours d\'examen', icon: 'bi-eye', color: 'info' },
    ACCEPTED: { label: 'Acceptée', icon: 'bi-check-circle-fill', color: 'success' },
    REJECTED: { label: 'Refusée', icon: 'bi-x-circle-fill', color: 'danger' },
    CANCELLED: { label: 'Annulée', icon: 'bi-dash-circle', color: 'neutral' },
  },
  listing: {
    ACTIVE: { label: 'Active', icon: 'bi-check-circle-fill', color: 'success' },
    DRAFT: { label: 'Brouillon', icon: 'bi-pencil', color: 'warning' },
    CLOSED: { label: 'Fermée', icon: 'bi-x-circle', color: 'neutral' },
    RENTED: { label: 'Louée', icon: 'bi-house-check-fill', color: 'info' },
  },
  inventory: {
    DRAFT: { label: 'Brouillon', icon: 'bi-pencil', color: 'warning' },
    COMPLETED: { label: 'Terminé', icon: 'bi-check-circle-fill', color: 'success' },
    SIGNED: { label: 'Signé', icon: 'bi-pen-fill', color: 'success' },
    DISPUTED: { label: 'Contesté', icon: 'bi-exclamation-triangle-fill', color: 'danger' },
  },
  expense: {
    PENDING: { label: 'En attente', icon: 'bi-clock', color: 'warning' },
    APPROVED: { label: 'Approuvée', icon: 'bi-check-circle-fill', color: 'success' },
    PAID: { label: 'Payée', icon: 'bi-check-circle-fill', color: 'success' },
    REJECTED: { label: 'Refusée', icon: 'bi-x-circle-fill', color: 'danger' },
  },
};

@Component({
  selector: 'app-status-badge',
  templateUrl: './status-badge.component.html',
  styleUrls: ['./status-badge.component.scss']
})
export class StatusBadgeComponent implements OnChanges {
  @Input() status = '';
  @Input() type: string = 'payment';

  config: StatusConfig = { label: '', icon: 'bi-circle', color: 'neutral' };

  ngOnChanges(): void {
    const typeMap = STATUS_MAP[this.type] || {};
    this.config = typeMap[this.status] || {
      label: this.status,
      icon: 'bi-circle',
      color: 'neutral' as const
    };
  }
}
