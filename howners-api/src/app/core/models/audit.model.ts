export enum AuditAction {
  CREATE = 'CREATE',
  READ = 'READ',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE',
  LOGIN = 'LOGIN',
  LOGOUT = 'LOGOUT',
  PAYMENT_CREATED = 'PAYMENT_CREATED',
  PAYMENT_CONFIRMED = 'PAYMENT_CONFIRMED',
  CONTRACT_SIGNED = 'CONTRACT_SIGNED',
  DATA_EXPORT = 'DATA_EXPORT',
  DATA_ERASURE = 'DATA_ERASURE'
}

export interface AuditLog {
  id: string;
  userId: string;
  userName: string;
  entityType: string;
  entityId: string;
  action: AuditAction;
  changes: string;
  ipAddress: string;
  userAgent: string;
  createdAt: string;
}

export interface AuditLogPage {
  content: AuditLog[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const AUDIT_ACTION_LABELS: { [key in AuditAction]: string } = {
  [AuditAction.CREATE]: 'Création',
  [AuditAction.READ]: 'Lecture',
  [AuditAction.UPDATE]: 'Modification',
  [AuditAction.DELETE]: 'Suppression',
  [AuditAction.LOGIN]: 'Connexion',
  [AuditAction.LOGOUT]: 'Déconnexion',
  [AuditAction.PAYMENT_CREATED]: 'Paiement créé',
  [AuditAction.PAYMENT_CONFIRMED]: 'Paiement confirmé',
  [AuditAction.CONTRACT_SIGNED]: 'Contrat signé',
  [AuditAction.DATA_EXPORT]: 'Export données',
  [AuditAction.DATA_ERASURE]: 'Effacement données'
};
