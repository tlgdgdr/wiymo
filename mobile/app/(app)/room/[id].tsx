import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { router, useLocalSearchParams } from 'expo-router';
import React, { useEffect, useState } from 'react';
import {
  ImageBackground,
  LayoutChangeEvent,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getAvatarAssets } from '@/api/avatar';
import { getRoom, getRoomUsers, joinRoom, leaveRoom } from '@/api/rooms';
import { Avatar } from '@/components/Avatar';
import { UserProfileModal } from '@/components/UserProfileModal';
import { resolveAssetUrl } from '@/config';
import { useAuthStore } from '@/store/auth';
import { colors, spacing } from '@/theme';

const BASE_AVATAR_SIZE = 64;

export default function RoomScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const roomId = id as string;
  const queryClient = useQueryClient();
  const myUserId = useAuthStore((s) => s.userId);

  const [stage, setStage] = useState({ width: 0, height: 0 });
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [joinError, setJoinError] = useState<string | null>(null);

  const { data: assets } = useQuery({ queryKey: ['avatarAssets'], queryFn: getAvatarAssets });
  const { data: detail } = useQuery({
    queryKey: ['room', roomId],
    queryFn: () => getRoom(roomId),
    enabled: !!roomId,
  });

  const joinMutation = useMutation({
    mutationFn: joinRoom,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['roomUsers', roomId] });
    },
    onError: (e) => setJoinError(e instanceof Error ? e.message : 'Could not join the room.'),
  });

  // Join on mount, leave on unmount.
  useEffect(() => {
    if (!roomId) return;
    joinMutation.mutate(roomId);
    return () => {
      leaveRoom(roomId).catch(() => undefined);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId]);

  const { data: users } = useQuery({
    queryKey: ['roomUsers', roomId],
    queryFn: () => getRoomUsers(roomId),
    enabled: !!roomId && joinMutation.isSuccess,
    refetchInterval: 10_000,
  });

  const onStageLayout = (event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    setStage({ width, height });
  };

  const slotByIndex = new Map(detail?.slots.map((s) => [s.slotIndex, s]) ?? []);

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={8}>
          <Text style={styles.back}>‹ Leave</Text>
        </Pressable>
        <Text style={styles.roomName}>{detail?.room.name ?? '...'}</Text>
        <Text style={styles.population}>
          {users ? `${users.length} here` : ''}
        </Text>
      </View>

      <ImageBackground
        source={{ uri: resolveAssetUrl(detail?.room.backgroundImageUrl) }}
        style={styles.stage}
        onLayout={onStageLayout}
      >
        {joinError ? (
          <View style={styles.errorBox}>
            <Text style={styles.errorText}>{joinError}</Text>
          </View>
        ) : null}

        {stage.width > 0 &&
          (users ?? []).map((user) => {
            const slot = slotByIndex.get(user.slotIndex);
            if (!slot) return null;
            const size = BASE_AVATAR_SIZE * (slot.scale || 1);
            const left = (slot.xPercent / 100) * stage.width - size / 2;
            const top = (slot.yPercent / 100) * stage.height - size / 2;
            const isMe = user.userId === myUserId;
            return (
              <Pressable
                key={user.userId}
                onPress={() => !isMe && setSelectedUserId(user.userId)}
                style={[styles.seat, { left, top, width: size }]}
              >
                <Avatar
                  avatar={user.avatar}
                  assets={assets}
                  size={size}
                  fallbackInitial={user.username}
                />
                <Text style={[styles.seatName, isMe && styles.seatNameMe]} numberOfLines={1}>
                  {isMe ? 'You' : user.username}
                </Text>
              </Pressable>
            );
          })}
      </ImageBackground>

      <UserProfileModal userId={selectedUserId} onClose={() => setSelectedUserId(null)} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  back: { color: colors.primary, fontSize: 15, fontWeight: '700' },
  roomName: { color: colors.text, fontSize: 17, fontWeight: '800' },
  population: { color: colors.textMuted, fontSize: 12, minWidth: 54, textAlign: 'right' },
  stage: { flex: 1 },
  seat: { position: 'absolute', alignItems: 'center' },
  seatName: {
    color: colors.text,
    fontSize: 11,
    fontWeight: '700',
    marginTop: 2,
    textShadowColor: 'rgba(0,0,0,0.8)',
    textShadowRadius: 4,
    maxWidth: 80,
  },
  seatNameMe: { color: colors.primary },
  errorBox: {
    margin: spacing.md,
    backgroundColor: 'rgba(20,10,10,0.85)',
    borderRadius: 12,
    padding: spacing.md,
  },
  errorText: { color: colors.error, fontSize: 13 },
});
