import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ApiRequestError } from '@/api/client';
import { acceptGame, declineGame, forfeitGame, listMyGames, playMove } from '@/api/games';
import type { GameState } from '@/api/types';
import { useAuthStore } from '@/store/auth';
import { colors, spacing } from '@/theme';
import { useChatSocket } from '@/ws/useChatSocket';

const MARKS: Record<string, string> = { X: '✕', O: '◯', '.': '' };

/**
 * The one live game, whatever stage it is at: an invite to answer, a board to
 * play, or a result to read. The server owns every rule — this screen only
 * sends "I tap cell N" and renders whatever state comes back.
 */
export default function GameScreen() {
  const myUserId = useAuthStore((s) => s.userId);
  const queryClient = useQueryClient();
  const { sendMoveViaSocket } = useChatSocket();

  const { data: games } = useQuery({ queryKey: ['games'], queryFn: listMyGames });
  const game: GameState | undefined = games?.[0];

  // Only the REST path can fail visibly; socket moves come back as pushed state.
  const moveMutation = useMutation({
    mutationFn: ({ id, cell }: { id: string; cell: number }) => playMove(id, cell),
  });
  const respondMutation = useMutation({
    mutationFn: ({ id, accept }: { id: string; accept: boolean }) =>
      accept ? acceptGame(id) : declineGame(id),
  });
  const forfeitMutation = useMutation({ mutationFn: forfeitGame });

  const dismiss = (id: string) =>
    queryClient.setQueryData<GameState[]>(['games'], (old: GameState[] | undefined) =>
      (old ?? []).filter((g: GameState) => g.id !== id),
    );

  if (!game) {
    return (
      <SafeAreaView style={styles.safe} edges={['top']}>
        <View style={styles.centered}>
          <Text style={styles.emptyEmoji}>🎲</Text>
          <Text style={styles.emptyTitle}>No game right now</Text>
          <Text style={styles.emptyBody}>
            Tap someone in a room and challenge them to a round.
          </Text>
          <Pressable style={styles.secondaryButton} onPress={() => router.push('/(app)/rooms')}>
            <Text style={styles.secondaryText}>Find a room</Text>
          </Pressable>
        </View>
      </SafeAreaView>
    );
  }

  const iAmChallenger = game.challengerId === myUserId;
  const opponentName = iAmChallenger ? game.opponentUsername : game.challengerUsername;
  const myTurn = game.turnUserId === myUserId;
  const finished = game.status !== 'PENDING' && game.status !== 'ACTIVE';

  const play = (cell: number) => {
    if (game.status !== 'ACTIVE' || !myTurn || game.board[cell] !== '.') return;
    // Fall back to REST whenever the socket is not connected, so a flaky
    // network costs latency rather than the move.
    if (!sendMoveViaSocket(game.id, cell)) {
      moveMutation.mutate({ id: game.id, cell });
    }
  };

  let banner: string;
  if (game.status === 'PENDING') {
    banner = iAmChallenger ? `Waiting for ${opponentName} to accept…` : `${opponentName} challenged you!`;
  } else if (game.status === 'ACTIVE') {
    banner = myTurn ? 'Your turn' : `${opponentName}'s turn`;
  } else if (game.status === 'DECLINED') {
    banner = `${opponentName} declined.`;
  } else if (game.draw) {
    banner = "It's a draw.";
  } else if (game.winnerId === myUserId) {
    banner = game.status === 'ABANDONED' ? `${opponentName} left — you win.` : 'You win! 🎉';
  } else if (game.winnerId) {
    banner = game.status === 'ABANDONED' ? 'You left the game.' : `${opponentName} wins.`;
  } else {
    banner = 'Game cancelled.';
  }

  const error =
    moveMutation.error instanceof ApiRequestError ? moveMutation.error.message : null;

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <Text style={styles.title}>Tic-Tac-Toe</Text>
        <Text style={styles.versus}>
          you {iAmChallenger ? MARKS.X : MARKS.O} vs {opponentName} {iAmChallenger ? MARKS.O : MARKS.X}
        </Text>
      </View>

      <Text style={[styles.banner, myTurn && game.status === 'ACTIVE' && styles.bannerActive]}>
        {banner}
      </Text>

      <View style={styles.board}>
        {Array.from({ length: 9 }, (_, cell) => {
          const mark = game.board[cell];
          const playable = game.status === 'ACTIVE' && myTurn && mark === '.';
          return (
            <Pressable
              key={cell}
              onPress={() => play(cell)}
              disabled={!playable}
              style={({ pressed }) => [styles.cell, pressed && playable && styles.cellPressed]}
            >
              <Text style={[styles.mark, mark === 'X' ? styles.markX : styles.markO]}>
                {MARKS[mark] ?? ''}
              </Text>
            </Pressable>
          );
        })}
      </View>

      {error ? <Text style={styles.error}>{error}</Text> : null}

      <View style={styles.footer}>
        {game.status === 'PENDING' && !iAmChallenger ? (
          <View style={styles.row}>
            <Pressable
              style={[styles.primaryButton, styles.flex]}
              disabled={respondMutation.isPending}
              onPress={() => respondMutation.mutate({ id: game.id, accept: true })}
            >
              <Text style={styles.primaryText}>Accept</Text>
            </Pressable>
            <Pressable
              style={[styles.secondaryButton, styles.flex]}
              disabled={respondMutation.isPending}
              onPress={() => respondMutation.mutate({ id: game.id, accept: false })}
            >
              <Text style={styles.secondaryText}>Decline</Text>
            </Pressable>
          </View>
        ) : finished ? (
          <Pressable style={styles.primaryButton} onPress={() => dismiss(game.id)}>
            <Text style={styles.primaryText}>Done</Text>
          </Pressable>
        ) : (
          <Pressable
            style={styles.quitButton}
            disabled={forfeitMutation.isPending}
            onPress={() => forfeitMutation.mutate(game.id)}
          >
            <Text style={styles.quitText}>
              {game.status === 'PENDING' ? 'Cancel invite' : 'Quit (opponent wins)'}
            </Text>
          </Pressable>
        )}
        <Pressable onPress={() => router.back()} style={styles.backButton}>
          <Text style={styles.backText}>Back</Text>
        </Pressable>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  centered: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: spacing.lg },
  emptyEmoji: { fontSize: 48 },
  emptyTitle: { color: colors.text, fontSize: 20, fontWeight: '800', marginTop: spacing.sm },
  emptyBody: {
    color: colors.textMuted,
    fontSize: 14,
    textAlign: 'center',
    marginTop: spacing.xs,
    marginBottom: spacing.lg,
  },
  header: { alignItems: 'center', paddingTop: spacing.md },
  title: { color: colors.text, fontSize: 24, fontWeight: '800' },
  versus: { color: colors.textMuted, fontSize: 13, marginTop: 2 },
  banner: {
    color: colors.textMuted,
    fontSize: 16,
    fontWeight: '700',
    textAlign: 'center',
    marginTop: spacing.lg,
  },
  bannerActive: { color: colors.primary },
  board: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignSelf: 'center',
    width: 300,
    marginTop: spacing.lg,
    gap: spacing.xs,
  },
  cell: {
    width: 94,
    height: 94,
    borderRadius: 16,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cellPressed: { backgroundColor: colors.surfaceLight },
  mark: { fontSize: 44, fontWeight: '800' },
  markX: { color: colors.primary },
  markO: { color: colors.text },
  error: { color: colors.error, textAlign: 'center', marginTop: spacing.md },
  footer: { marginTop: 'auto', padding: spacing.md, gap: spacing.sm },
  row: { flexDirection: 'row', gap: spacing.sm },
  flex: { flex: 1 },
  primaryButton: {
    backgroundColor: colors.primary,
    borderRadius: 14,
    paddingVertical: spacing.md,
    alignItems: 'center',
  },
  primaryText: { color: '#1b1526', fontSize: 15, fontWeight: '800' },
  secondaryButton: {
    backgroundColor: colors.surface,
    borderRadius: 14,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.lg,
    alignItems: 'center',
  },
  secondaryText: { color: colors.text, fontSize: 15, fontWeight: '700' },
  quitButton: { alignItems: 'center', padding: spacing.sm },
  quitText: { color: colors.error, fontSize: 13, fontWeight: '700' },
  backButton: { alignItems: 'center', padding: spacing.xs },
  backText: { color: colors.primary, fontWeight: '700' },
});
