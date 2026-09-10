import { apiFetch } from './client';
import type { Connection } from './types';

export function listConnections(): Promise<Connection[]> {
  return apiFetch<Connection[]>('/api/connections');
}

export function requestConnection(userId: string): Promise<Connection> {
  return apiFetch<Connection>(`/api/connections/${userId}`, { method: 'POST' });
}

export function acceptConnection(id: string): Promise<Connection> {
  return apiFetch<Connection>(`/api/connections/${id}/accept`, { method: 'POST' });
}

export function rejectConnection(id: string): Promise<Connection> {
  return apiFetch<Connection>(`/api/connections/${id}/reject`, { method: 'POST' });
}
