import { apiFetch } from './client';
import type { GameState, GameType } from './types';

/** Pending invites and games in progress, for both roles. */
export function listMyGames(): Promise<GameState[]> {
  return apiFetch<GameState[]>('/api/games');
}

export function inviteToGame(
  opponentId: string,
  gameType: GameType = 'TIC_TAC_TOE',
  roomId?: string,
): Promise<GameState> {
  return apiFetch<GameState>('/api/games', {
    method: 'POST',
    body: JSON.stringify({ opponentId, gameType, roomId: roomId ?? null }),
  });
}

export function acceptGame(id: string): Promise<GameState> {
  return apiFetch<GameState>(`/api/games/${id}/accept`, { method: 'POST' });
}

export function declineGame(id: string): Promise<GameState> {
  return apiFetch<GameState>(`/api/games/${id}/decline`, { method: 'POST' });
}

/** REST fallback; the WebSocket is the primary move path. */
export function playMove(id: string, cell: number): Promise<GameState> {
  return apiFetch<GameState>(`/api/games/${id}/moves`, {
    method: 'POST',
    body: JSON.stringify({ cell }),
  });
}

export function forfeitGame(id: string): Promise<GameState> {
  return apiFetch<GameState>(`/api/games/${id}/forfeit`, { method: 'POST' });
}
