/** Couleur d'avatar stable dérivée du nom (palette de la charte). */
const AVATAR_COLORS = ['#1E3A5F', '#2C5478', '#B69248', '#2E6B45', '#245F8F', '#8E6F30', '#557B9C', '#A6332E'];

export function avatarColor(name: string): string {
  let hash = 0;
  for (let i = 0; i < (name || '').length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) >>> 0;
  }
  return AVATAR_COLORS[hash % AVATAR_COLORS.length];
}

export interface Message {
  id: string;
  senderId: string;
  senderName: string;
  recipientId: string;
  recipientName: string;
  listingId: string | null;
  applicationId: string | null;
  parentId: string | null;
  subject: string | null;
  body: string;
  isRead: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface Conversation {
  partnerId: string;
  partnerName: string;
  lastMessageBody: string;
  lastMessageRead: boolean;
  lastMessageAt: string;
  unreadCount: number;
}

export interface CreateMessageRequest {
  recipientId: string;
  subject?: string;
  body: string;
  listingId?: string;
  applicationId?: string;
  parentId?: string;
}
