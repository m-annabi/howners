export type InAppNotificationType =
  | 'message'
  | 'payment'
  | 'contract'
  | 'application'
  | 'inventory'
  | 'system';

export interface InAppNotification {
  id: string;
  type: InAppNotificationType;
  title: string;
  body: string;
  route?: string;
  icon: string;
  isRead: boolean;
  createdAt: Date;
}
