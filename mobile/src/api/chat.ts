import { apiFetch } from './client';
import type { Conversation, Message } from './types';

export function getConversations(): Promise<Conversation[]> {
  return apiFetch<Conversation[]>('/api/conversations');
}

export function getMessages(partnerId: string, before?: string): Promise<Message[]> {
  const query = before ? `?before=${encodeURIComponent(before)}` : '';
  return apiFetch<Message[]>(`/api/conversations/${partnerId}/messages${query}`);
}

/** REST fallback; the WebSocket is the primary send path. */
export function sendMessageRest(partnerId: string, content: string): Promise<Message> {
  return apiFetch<Message>(`/api/conversations/${partnerId}/messages`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  });
}
