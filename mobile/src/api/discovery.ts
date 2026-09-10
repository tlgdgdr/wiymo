import { apiFetch } from './client';
import type { DiscoveredUser, Intention } from './types';

export interface DiscoveryFilters {
  intention?: Intention;
  languageCode?: string;
  countryCode?: string;
  onlineOnly?: boolean;
  ageMin?: number;
  ageMax?: number;
}

export function discoverUsers(filters: DiscoveryFilters = {}): Promise<DiscoveredUser[]> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== null && value !== '') {
      params.set(key, String(value));
    }
  }
  const query = params.toString();
  return apiFetch<DiscoveredUser[]>(`/api/discovery/users${query ? `?${query}` : ''}`);
}
