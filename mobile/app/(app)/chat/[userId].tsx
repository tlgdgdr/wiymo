import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { router, useLocalSearchParams } from 'expo-router';
import React, { useState } from 'react';
import {
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getAvatarAssets } from '@/api/avatar';
import { getMessages, sendMessageRest } from '@/api/chat';
import type { Message } from '@/api/types';
import { getPublicProfile } from '@/api/users';
import { Avatar } from '@/components/Avatar';
import { useAuthStore } from '@/store/auth';
import { useChatSocket } from '@/ws/useChatSocket';
import { colors, spacing } from '@/theme';

export default function PrivateChatScreen() {
  const { userId } = useLocalSearchParams<{ userId: string }>();
  const partnerId = userId as string;
  const myUserId = useAuthStore((s) => s.userId);
  const queryClient = useQueryClient();
  const { sendViaSocket } = useChatSocket();

  const [draft, setDraft] = useState('');

  const { data: assets } = useQuery({ queryKey: ['avatarAssets'], queryFn: getAvatarAssets });
  const { data: partner } = useQuery({
    queryKey: ['publicProfile', partnerId],
    queryFn: () => getPublicProfile(partnerId),
    enabled: !!partnerId,
  });
  const { data: messages } = useQuery({
    queryKey: ['messages', partnerId],
    queryFn: () => getMessages(partnerId),
    enabled: !!partnerId,
    // The socket handles live updates; this is a safety net.
    refetchInterval: 30_000,
  });

  const restSend = useMutation({
    mutationFn: (content: string) => sendMessageRest(partnerId, content),
    onSuccess: (message) => {
      queryClient.setQueryData<Message[]>(['messages', partnerId], (old: Message[] | undefined) => {
        if (!old) return [message];
        if (old.some((m: Message) => m.id === message.id)) return old;
        return [message, ...old];
      });
    },
  });

  const send = () => {
    const content = draft.trim();
    if (!content) return;
    setDraft('');
    // Prefer the socket; fall back to REST when it's not connected.
    if (!sendViaSocket(partnerId, content)) {
      restSend.mutate(content);
    }
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={8}>
          <Text style={styles.back}>‹</Text>
        </Pressable>
        <Avatar
          avatar={partner?.avatar}
          assets={assets}
          size={36}
          fallbackInitial={partner?.username}
        />
        <View style={styles.headerText}>
          <Text style={styles.headerName}>{partner?.username ?? '...'}</Text>
          <Text style={styles.headerStatus}>
            {partner?.online ? 'online' : 'offline'}
          </Text>
        </View>
      </View>

      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={8}
      >
        <FlatList
          data={messages ?? []}
          inverted
          keyExtractor={(m) => m.id}
          contentContainerStyle={styles.messageList}
          renderItem={({ item }) => {
            const mine = item.senderId === myUserId;
            return (
              <View style={[styles.bubble, mine ? styles.bubbleMine : styles.bubbleTheirs]}>
                <Text style={mine ? styles.bubbleTextMine : styles.bubbleText}>
                  {item.messageType === 'GIFT' ? `🎁 ${item.content}` : item.content}
                </Text>
              </View>
            );
          }}
        />

        <View style={styles.footer}>
          <Pressable style={styles.giftButton}>
            <Text style={styles.giftIcon}>🎁</Text>
          </Pressable>
          <TextInput
            style={styles.input}
            value={draft}
            onChangeText={setDraft}
            placeholder="Message..."
            placeholderTextColor={colors.textMuted}
            multiline
            maxLength={1000}
          />
          <Pressable onPress={send} style={styles.sendButton} disabled={!draft.trim()}>
            <Text style={styles.sendText}>➤</Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  flex: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.surface,
  },
  back: { color: colors.primary, fontSize: 28, fontWeight: '700', marginRight: spacing.xs },
  headerText: { flex: 1 },
  headerName: { color: colors.text, fontSize: 16, fontWeight: '700' },
  headerStatus: { color: colors.textMuted, fontSize: 11 },
  messageList: { padding: spacing.md, gap: spacing.xs },
  bubble: {
    maxWidth: '78%',
    borderRadius: 16,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  bubbleMine: {
    alignSelf: 'flex-end',
    backgroundColor: colors.primary,
    borderBottomRightRadius: 4,
  },
  bubbleTheirs: {
    alignSelf: 'flex-start',
    backgroundColor: colors.surface,
    borderBottomLeftRadius: 4,
  },
  bubbleText: { color: colors.text, fontSize: 15 },
  bubbleTextMine: { color: '#1b1526', fontSize: 15, fontWeight: '500' },
  footer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: spacing.xs,
    padding: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.surface,
  },
  giftButton: {
    padding: spacing.sm,
    borderRadius: 12,
    backgroundColor: colors.surface,
  },
  giftIcon: { fontSize: 18 },
  input: {
    flex: 1,
    backgroundColor: colors.surface,
    borderRadius: 18,
    paddingHorizontal: spacing.md,
    paddingVertical: 10,
    color: colors.text,
    fontSize: 15,
    maxHeight: 110,
  },
  sendButton: {
    backgroundColor: colors.primary,
    borderRadius: 18,
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sendText: { color: '#1b1526', fontSize: 16, fontWeight: '800' },
});
