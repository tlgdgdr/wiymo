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
  { value: 'MEET_PEOPLE', emoji: '👥', label: 'Meet People', tagline: 'New faces' },
  { value: 'PLAY_GAMES', emoji: '🎲', label: 'Play Games', tagline: 'A quick round' },
  { value: 'CHILL', emoji: '🌿', label: 'Chill', tagline: 'Just hang out' },
];

/**
 * Moods the backend still understands but the app no longer offers. Kept only
 * so existing profiles and old rooms render a name instead of "Not set".
 */
const RETIRED_INTENTIONS: Record<string, { emoji: string; label: string }> = {
  LANGUAGE_EXCHANGE: { emoji: '🌍', label: 'Language Exchange' },
};

export function intentionLabel(value: Intention | null | undefined): string {
  return (
    INTENTIONS.find((i) => i.value === value)?.label ??
    (value ? RETIRED_INTENTIONS[value]?.label : undefined) ??
    'Not set'
  );
}

export function intentionEmoji(value: Intention | null | undefined): string {
  return (
    INTENTIONS.find((i) => i.value === value)?.emoji ??
    (value ? RETIRED_INTENTIONS[value]?.emoji : undefined) ??
    '✨'
  );
}
