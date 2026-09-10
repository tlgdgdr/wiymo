import { apiFetch } from './client';
import type { Intention, Me, PublicProfile, UpdateProfileRequest } from './types';

export function getMe(): Promise<Me> {
  return apiFetch<Me>('/api/users/me');
}

export function updateProfile(request: UpdateProfileRequest): Promise<Me> {
  return apiFetch<Me>('/api/users/me', {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

export function updateIntention(intention: Intention): Promise<Me> {
  return apiFetch<Me>('/api/users/me/intention', {
    method: 'PUT',
    body: JSON.stringify({ intention }),
  });
}

export function getPublicProfile(userId: string): Promise<PublicProfile> {
  return apiFetch<PublicProfile>(`/api/users/${userId}`);
}

export function registerPushToken(token: string): Promise<void> {
  return apiFetch<void>('/api/users/me/push-token', {
    method: 'PUT',
    body: JSON.stringify({ token }),
  });
}

export function deleteAccount(password: string): Promise<void> {
  return apiFetch<void>('/api/users/me', {
    method: 'DELETE',
    body: JSON.stringify({ password }),
  });
}
