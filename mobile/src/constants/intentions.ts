import type { Intention } from '@/api/types';

export interface IntentionCard {
  value: Intention;
  emoji: string;
  label: string;
  tagline: string;
}

export const INTENTIONS: IntentionCard[] = [
  { value: 'CASUAL_CHAT', emoji: '💬', label: 'Casual Chat', tagline: 'Easy, everyday talk' },
  { value: 'DEEP_TALK', emoji: '🌙', label: 'Deep Talk', tagline: 'Real conversations' },
  { value: 'FLIRT', emoji: '💕', label: 'Flirt', tagline: 'A little spark' },
  { value: 'LANGUAGE_EXCHANGE', emoji: '🌍', label: 'Language Exchange', tagline: 'Practice together' },
  { value: 'MEET_PEOPLE', emoji: '👥', label: 'Meet People', tagline: 'New faces' },
  { value: 'CHILL', emoji: '🎮', label: 'Chill', tagline: 'Just hang out' },
];

export function intentionLabel(value: Intention | null | undefined): string {
  return INTENTIONS.find((i) => i.value === value)?.label ?? 'Not set';
}

export function intentionEmoji(value: Intention | null | undefined): string {
  return INTENTIONS.find((i) => i.value === value)?.emoji ?? '✨';
}
