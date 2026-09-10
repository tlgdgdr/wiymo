import { useQuery } from '@tanstack/react-query';
import { router } from 'expo-router';
import React from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getAvatarAssets } from '@/api/avatar';
import { getConversations } from '@/api/chat';
import { Avatar } from '@/components/Avatar';
import { useChatSocket } from '@/ws/useChatSocket';
import { colors, spacing } from '@/theme';

function timeLabel(iso: string): string {
  const date = new Date(iso);
  const now = new Date();
  const sameDay = date.toDateString() === now.toDateString();
  return sameDay
    ? date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : date.toLocaleDateString([], { day: 'numeric', month: 'short' });
}

export default function ChatListScreen() {
  useChatSocket();
  const { data: assets } = useQuery({ queryKey: ['avatarAssets'], queryFn: getAvatarAssets });
  const { data: conversations } = useQuery({
    queryKey: ['conversations'],
    queryFn: getConversations,
    refetchInterval: 20_000,
  });

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <Text style={styles.title}>Chats</Text>
      <FlatList
        data={conversations ?? []}
        keyExtractor={(c) => c.partnerId}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <Pressable
            onPress={() => router.push(`/(app)/chat/${item.partnerId}`)}
            style={({ pressed }) => [styles.row, pressed && styles.rowPressed]}
          >
            <View>
              <Avatar
                avatar={item.partnerAvatar}
                assets={assets}
                size={48}
                fallbackInitial={item.partnerUsername}
              />
              {item.partnerOnline ? <View style={styles.onlineDot} /> : null}
            </View>
            <View style={styles.rowText}>
              <Text style={styles.username}>{item.partnerUsername}</Text>
              <Text style={styles.preview} numberOfLines={1}>
                {item.lastMessage.messageType === 'GIFT' ? '🎁 Gift' : item.lastMessage.content}
              </Text>
            </View>
            <View style={styles.rowMeta}>
              <Text style={styles.time}>{timeLabel(item.lastMessage.createdAt)}</Text>
              {item.unreadCount > 0 ? (
                <View style={styles.badge}>
                  <Text style={styles.badgeText}>{item.unreadCount}</Text>
                </View>
              ) : null}
            </View>
          </Pressable>
        )}
        ListEmptyComponent={
          <Text style={styles.empty}>
            No conversations yet. Tap someone in a room to say hi 👋
          </Text>
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  title: {
    color: colors.text,
    fontSize: 24,
    fontWeight: '800',
    padding: spacing.md,
  },
  list: { paddingHorizontal: spacing.md, paddingBottom: spacing.xl },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing.sm,
    gap: spacing.sm,
  },
  rowPressed: { opacity: 0.7 },
  onlineDot: {
    position: 'absolute',
    right: 0,
    bottom: 0,
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: colors.success,
    borderWidth: 2,
    borderColor: colors.background,
  },
  rowText: { flex: 1 },
  username: { color: colors.text, fontSize: 16, fontWeight: '700' },
  preview: { color: colors.textMuted, fontSize: 13, marginTop: 1 },
  rowMeta: { alignItems: 'flex-end', gap: 4 },
  time: { color: colors.textMuted, fontSize: 11 },
  badge: {
    backgroundColor: colors.primary,
    borderRadius: 999,
    minWidth: 20,
    height: 20,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 5,
  },
  badgeText: { color: '#1b1526', fontSize: 11, fontWeight: '800' },
  empty: { color: colors.textMuted, textAlign: 'center', marginTop: spacing.xl },
});
