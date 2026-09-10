import { apiFetch } from './client';
import type { ReportReason } from './types';

export function blockUser(userId: string): Promise<void> {
  return apiFetch<void>(`/api/users/${userId}/block`, { method: 'POST' });
}

export function unblockUser(userId: string): Promise<void> {
  return apiFetch<void>(`/api/users/${userId}/block`, { method: 'DELETE' });
}

export function reportUser(
  reportedUserId: string,
  reason: ReportReason,
  description?: string,
): Promise<void> {
  return apiFetch<void>('/api/reports', {
    method: 'POST',
    body: JSON.stringify({ reportedUserId, reason, description }),
  });
}
