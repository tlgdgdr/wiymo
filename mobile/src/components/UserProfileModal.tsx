import { useMutation, useQuery } from '@tanstack/react-query';
import { router } from 'expo-router';
import React, { useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import { getAvatarAssets } from '@/api/avatar';
import { ApiRequestError } from '@/api/client';
import { requestConnection } from '@/api/connections';
import { inviteToGame } from '@/api/games';
import { blockUser, reportUser } from '@/api/moderation';
import type { ReportReason } from '@/api/types';
import { getPublicProfile } from '@/api/users';
import { Avatar } from '@/components/Avatar';
import { intentionEmoji, intentionLabel } from '@/constants/intentions';
import { colors, spacing } from '@/theme';

interface Props {
  userId: string | null;
  onClose: () => void;
}

/**
 * Bottom-sheet style public profile. Chat/Gift/Block/Report buttons appear
 * here as the corresponding phases land.
 */
export function UserProfileModal({ userId, onClose }: Props) {
  const { data: assets } = useQuery({ queryKey: ['avatarAssets'], queryFn: getAvatarAssets });
  const [connectState, setConnectState] = useState<string | null>(null);
  const [reportOpen, setReportOpen] = useState(false);
  const [moderationState, setModerationState] = useState<string | null>(null);
  const [gameState, setGameState] = useState<string | null>(null);

  const gameMutation = useMutation({
    mutationFn: (opponentId: string) => inviteToGame(opponentId),
    onSuccess: () => {
      onClose();
      router.push('/(app)/game');
    },
    onError: (e) =>
      setGameState(e instanceof ApiRequestError ? e.message : 'Could not start a game.'),
  });

  const blockMutation = useMutation({
    mutationFn: blockUser,
    onSuccess: () => setModerationState('User blocked. They can no longer reach you.'),
    onError: () => setModerationState('Could not block. Try again.'),
  });
  const reportMutation = useMutation({
    mutationFn: (reason: ReportReason) => reportUser(userId as string, reason),
    onSuccess: () => {
      setReportOpen(false);
      setModerationState('Report sent. Thank you.');
    },
    onError: (e) =>
      setModerationState(e instanceof ApiRequestError ? e.message : 'Could not send report.'),
  });

  const REPORT_REASONS: { value: ReportReason; label: string }[] = [
    { value: 'HARASSMENT', label: 'Harassment' },
    { value: 'SEXUAL_CONTENT', label: 'Sexual content' },
    { value: 'HATE_SPEECH', label: 'Hate speech' },
    { value: 'SPAM', label: 'Spam' },
    { value: 'FAKE_PROFILE', label: 'Fake profile' },
    { value: 'UNDERAGE', label: 'Underage' },
    { value: 'THREATS', label: 'Threats' },
    { value: 'OTHER', label: 'Other' },
  ];
  const connectMutation = useMutation({
    mutationFn: requestConnection,
    onSuccess: () => setConnectState('Request sent ✓'),
    onError: (e) =>
      setConnectState(e instanceof ApiRequestError ? e.message : 'Could not send request.'),
  });
  const { data: profile } = useQuery({
    queryKey: ['publicProfile', userId],
    queryFn: () => getPublicProfile(userId as string),
    enabled: !!userId,
  });

  return (
    <Modal visible={!!userId} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose}>
        <Pressable style={styles.sheet} onPress={() => undefined}>
          <View style={styles.grabber} />
          <View style={styles.avatarWrap}>
            <Avatar
              avatar={profile?.avatar}
              assets={assets}
              size={110}
              fallbackInitial={profile?.username}
            />
          </View>
          <Text style={styles.username}>
            {profile ? `${profile.username}, ${profile.age}` : '...'}
          </Text>
          <Text style={styles.meta}>
            {profile?.countryCode ? `${profile.countryCode} · ` : ''}
            {intentionEmoji(profile?.currentIntention)} {intentionLabel(profile?.currentIntention)}
            {profile?.online ? ' · online' : ''}
          </Text>

          {profile?.bio ? <Text style={styles.bio}>{profile.bio}</Text> : null}

          {profile?.languages.length ? (
            <View style={styles.languages}>
              {profile.languages.map((lang) => (
                <View key={lang.id} style={styles.languageChip}>
                  <Text style={styles.languageChipText}>
                    {lang.languageCode.toUpperCase()} {lang.level}
                  </Text>
                </View>
              ))}
            </View>
          ) : null}

          <View style={styles.actions}>
            <Pressable
              style={styles.action}
              onPress={() => {
                if (!userId) return;
                onClose();
                router.push(`/(app)/chat/${userId}`);
              }}
            >
              <Text style={styles.actionText}>💬 Chat</Text>
            </Pressable>
            <Pressable
              style={styles.action}
              onPress={() => {
                if (!userId) return;
                onClose();
                router.push(`/(app)/chat/${userId}`);
              }}
            >
              <Text style={styles.actionText}>🎁 Gift</Text>
            </Pressable>
          </View>

          <View style={styles.actions}>
            <Pressable
              style={styles.action}
              onPress={() => userId && !connectMutation.isPending && connectMutation.mutate(userId)}
            >
              <Text style={styles.actionText}>🤝 Connect</Text>
              {connectState ? <Text style={styles.actionSoon}>{connectState}</Text> : null}
            </Pressable>
            <Pressable
              style={styles.action}
              onPress={() => userId && !gameMutation.isPending && gameMutation.mutate(userId)}
            >
              <Text style={styles.actionText}>🎲 Play</Text>
              {gameState ? <Text style={styles.actionSoon}>{gameState}</Text> : null}
            </Pressable>
          </View>

          {reportOpen ? (
            <View style={styles.reportBox}>
              <Text style={styles.reportTitle}>Why are you reporting this user?</Text>
              <View style={styles.reportReasons}>
                {REPORT_REASONS.map((r) => (
                  <Pressable
                    key={r.value}
                    style={styles.reasonChip}
                    onPress={() => !reportMutation.isPending && reportMutation.mutate(r.value)}
                  >
                    <Text style={styles.reasonText}>{r.label}</Text>
                  </Pressable>
                ))}
              </View>
            </View>
          ) : null}

          <View style={styles.moderationRow}>
            <Pressable
              style={styles.moderationButton}
              onPress={() => userId && !blockMutation.isPending && blockMutation.mutate(userId)}
            >
              <Text style={styles.moderationText}>Block</Text>
            </Pressable>
            <Pressable
              style={styles.moderationButton}
              onPress={() => setReportOpen((v) => !v)}
            >
              <Text style={styles.moderationText}>Report</Text>
            </Pressable>
          </View>
          {moderationState ? <Text style={styles.moderationState}>{moderationState}</Text> : null}

          <Pressable onPress={onClose} style={styles.closeButton}>
            <Text style={styles.closeText}>Close</Text>
          </Pressable>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.55)',
    justifyContent: 'flex-end',
  },
  sheet: {
    backgroundColor: colors.surface,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: spacing.lg,
    paddingBottom: spacing.xl,
  },
  grabber: {
    width: 40,
    height: 4,
    borderRadius: 2,
    backgroundColor: colors.surfaceLight,
    alignSelf: 'center',
    marginBottom: spacing.md,
  },
  avatarWrap: { alignSelf: 'center' },
  username: {
    color: colors.text,
    fontSize: 22,
    fontWeight: '800',
    textAlign: 'center',
    marginTop: spacing.sm,
  },
  meta: { color: colors.textMuted, fontSize: 13, textAlign: 'center', marginTop: 2 },
  bio: { color: colors.text, fontSize: 14, textAlign: 'center', marginTop: spacing.md },
  languages: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: spacing.xs,
    marginTop: spacing.md,
  },
  languageChip: {
    backgroundColor: colors.surfaceLight,
    borderRadius: 999,
    paddingHorizontal: spacing.sm,
    paddingVertical: 4,
  },
  languageChipText: { color: colors.textMuted, fontSize: 11, fontWeight: '700' },
  actions: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.lg },
  connectAction: { marginTop: spacing.sm },
  reportBox: {
    backgroundColor: colors.surfaceLight,
    borderRadius: 14,
    padding: spacing.md,
    marginTop: spacing.sm,
  },
  reportTitle: { color: colors.text, fontSize: 13, fontWeight: '700', marginBottom: spacing.sm },
  reportReasons: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xs },
  reasonChip: {
    backgroundColor: colors.surface,
    borderRadius: 999,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
  },
  reasonText: { color: colors.textMuted, fontSize: 11.5, fontWeight: '700' },
  moderationRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: spacing.lg,
    marginTop: spacing.md,
  },
  moderationButton: { padding: spacing.xs },
  moderationText: { color: colors.error, fontSize: 12.5, fontWeight: '700' },
  moderationState: {
    color: colors.textMuted,
    fontSize: 11.5,
    textAlign: 'center',
    marginTop: spacing.xs,
  },
  action: {
    flex: 1,
    backgroundColor: colors.surfaceLight,
    borderRadius: 14,
    paddingVertical: spacing.md,
    alignItems: 'center',
  },
  actionDisabled: { opacity: 0.55 },
  actionText: { color: colors.text, fontSize: 15, fontWeight: '700' },
  actionSoon: { color: colors.textMuted, fontSize: 10, marginTop: 2 },
  closeButton: { alignItems: 'center', marginTop: spacing.md, padding: spacing.sm },
  closeText: { color: colors.primary, fontWeight: '700' },
});
