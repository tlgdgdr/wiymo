import { apiFetch } from './client';
import type { AvatarAsset, UserAvatar } from './types';

export function getAvatarAssets(): Promise<AvatarAsset[]> {
  return apiFetch<AvatarAsset[]>('/api/avatar/assets');
}

export function getMyAvatar(): Promise<UserAvatar> {
  return apiFetch<UserAvatar>('/api/users/me/avatar');
}

export function updateMyAvatar(avatar: UserAvatar): Promise<UserAvatar> {
  return apiFetch<UserAvatar>('/api/users/me/avatar', {
    method: 'PUT',
    body: JSON.stringify(avatar),
  });
}
