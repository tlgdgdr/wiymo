import { apiFetch } from './client';
import type { Gift, SendGiftResponse, Wallet } from './types';

export function listGifts(): Promise<Gift[]> {
  return apiFetch<Gift[]>('/api/gifts');
}

export function getMyWallet(): Promise<Wallet> {
  return apiFetch<Wallet>('/api/wallets/me');
}

export function sendGift(receiverId: string, giftId: string): Promise<SendGiftResponse> {
  return apiFetch<SendGiftResponse>('/api/gifts/send', {
    method: 'POST',
    body: JSON.stringify({ receiverId, giftId }),
  });
}
