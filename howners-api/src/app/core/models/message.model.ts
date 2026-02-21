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
