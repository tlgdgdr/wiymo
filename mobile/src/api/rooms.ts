import { apiFetch } from './client';
import type { Intention, JoinRoomResponse, Room, RoomDetail, RoomUser } from './types';

export function listRooms(intention?: Intention | null): Promise<Room[]> {
  const query = intention ? `?intention=${intention}` : '';
  return apiFetch<Room[]>(`/api/rooms${query}`);
}

export function getRoom(roomId: string): Promise<RoomDetail> {
  return apiFetch<RoomDetail>(`/api/rooms/${roomId}`);
}

export function joinRoom(roomId: string): Promise<JoinRoomResponse> {
  return apiFetch<JoinRoomResponse>(`/api/rooms/${roomId}/join`, { method: 'POST' });
}

export function leaveRoom(roomId: string): Promise<void> {
  return apiFetch<void>(`/api/rooms/${roomId}/leave`, { method: 'POST' });
}

export function getRoomUsers(roomId: string): Promise<RoomUser[]> {
  return apiFetch<RoomUser[]>(`/api/rooms/${roomId}/users`);
}
