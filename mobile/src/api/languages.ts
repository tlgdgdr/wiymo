import { apiFetch } from './client';
import type { AddLanguageRequest, UserLanguage } from './types';

export function listMyLanguages(): Promise<UserLanguage[]> {
  return apiFetch<UserLanguage[]>('/api/users/me/languages');
}

export function addLanguage(request: AddLanguageRequest): Promise<UserLanguage> {
  return apiFetch<UserLanguage>('/api/users/me/languages', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function removeLanguage(id: string): Promise<void> {
  return apiFetch<void>(`/api/users/me/languages/${id}`, { method: 'DELETE' });
}
