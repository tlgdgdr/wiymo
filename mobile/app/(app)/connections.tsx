import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';
import React from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getAvatarAssets } from '@/api/avatar';
import { acceptConnection, listConnections, rejectConnection } from '@/api/connections';
import type { Connection } from '@/api/types';
import { Avatar } from '@/components/Avatar';
import { colors, spacing } from '@/theme';

export default function ConnectionsScreen() {
  const queryClient = useQueryClient();
  const { data: assets } = useQuery({ queryKey: ['avatarAssets'], queryFn: getAvatarAssets });
  const { data: connections } = useQuery({ queryKey: ['connections'], queryFn: listConnections });

  const invalidate = () => void queryClient.invalidateQueries({ queryKey: ['connections'] });
  const acceptMutation = useMutation({ mutationFn: acceptConnection, onSuccess: invalidate });
  const rejectMutation = useMutation({ mutationFn: rejectConnection, onSuccess: invalidate });

  const pendingIncoming = (connections ?? []).filter(
    (c) => c.status === 'PENDING' && c.incoming,
  );
  const others = (connections ?? []).filter((c) => !(c.status === 'PENDING' && c.incoming));

  const renderRow = (item: Connection, actions: boolean) => (
    <Pressable
      style={styles.row}
      onPress={() => router.push(`/(app)/chat/${item.partnerId}`)}
    >
      <View>
        <Avatar
          avatar={item.partnerAvatar}
          assets={assets}
          size={44}
          fallbackInitial={item.partnerUsername}
        />
        {item.partnerOnline ? <View style={styles.onlineDot} /> : null}
      </View>
      <View style={styles.rowText}>
        <Text style={styles.username}>{item.partnerUsername}</Text>
        <Text style={styles.statusLabel}>
          {item.status === 'ACCEPTED'
            ? 'Connected'
            : item.incoming
              ? 'Wants to connect'
              : 'Request sent'}
        </Text>
      </View>
      {actions ? (
        <View style={styles.actions}>
          <Pressable
            style={[styles.actionButton, styles.acceptButton]}
            onPress={() => acceptMutation.mutate(item.id)}
          >
            <Text style={styles.acceptText}>Accept</Text>
          </Pressable>
          <Pressable
            style={styles.actionButton}
            onPress={() => rejectMutation.mutate(item.id)}
          >
            <Text style={styles.rejectText}>Decline</Text>
          </Pressable>
        </View>
      ) : null}
    </Pressable>
  );

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <Text style={styles.title}>Connections</Text>
      <FlatList
        data={[...pendingIncoming, ...others]}
        keyExtractor={(c) => c.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => renderRow(item, item.status === 'PENDING' && item.incoming)}
        ListEmptyComponent={
          <Text style={styles.empty}>
            No connections yet. Meet people in rooms and send a request 🤝
          </Text>
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  title: { color: colors.text, fontSize: 24, fontWeight: '800', padding: spacing.md },
  list: { paddingHorizontal: spacing.md, paddingBottom: spacing.xl },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing.sm,
    gap: spacing.sm,
  },
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
  statusLabel: { color: colors.textMuted, fontSize: 12, marginTop: 1 },
  actions: { flexDirection: 'row', gap: spacing.xs },
  actionButton: {
    borderRadius: 10,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    backgroundColor: colors.surface,
  },
  acceptButton: { backgroundColor: colors.primary },
  acceptText: { color: '#1b1526', fontWeight: '800', fontSize: 12 },
  rejectText: { color: colors.textMuted, fontWeight: '700', fontSize: 12 },
  empty: { color: colors.textMuted, textAlign: 'center', marginTop: spacing.xl },
});
