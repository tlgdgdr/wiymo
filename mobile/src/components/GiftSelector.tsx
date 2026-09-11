import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import React, { useState } from 'react';
import { Image, Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import { ApiRequestError } from '@/api/client';
import { getMyWallet, listGifts, sendGift } from '@/api/gifts';
import type { Message } from '@/api/types';
import { resolveAssetUrl } from '@/config';
import { colors, spacing } from '@/theme';

interface Props {
  visible: boolean;
  receiverId: string;
  onClose: () => void;
}

/** Bottom sheet with the gift catalog; deducts coins and drops a GIFT message. */
export function GiftSelector({ visible, receiverId, onClose }: Props) {
  const queryClient = useQueryClient();
  const [selectedGiftId, setSelectedGiftId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const { data: gifts } = useQuery({ queryKey: ['gifts'], queryFn: listGifts, enabled: visible });
  const { data: wallet } = useQuery({ queryKey: ['wallet'], queryFn: getMyWallet, enabled: visible });

  const mutation = useMutation({
    mutationFn: (giftId: string) => sendGift(receiverId, giftId),
    onSuccess: (response) => {
      queryClient.setQueryData(['wallet'], { coinBalance: response.newBalance });
      queryClient.setQueryData<Message[]>(['messages', receiverId], (old: Message[] | undefined) => {
        if (!old) return [response.message];
        if (old.some((m: Message) => m.id === response.message.id)) return old;
        return [response.message, ...old];
      });
      void queryClient.invalidateQueries({ queryKey: ['conversations'] });
      setError(null);
      setSelectedGiftId(null);
      onClose();
    },
    onError: (e) =>
      setError(e instanceof ApiRequestError ? e.message : 'Could not reach the server.'),
  });

  const selected = (gifts ?? []).find((g) => g.id === selectedGiftId) ?? null;
  const canAfford = selected && wallet ? wallet.coinBalance >= selected.coinPrice : true;

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose}>
        <Pressable style={styles.sheet} onPress={() => undefined}>
          <View style={styles.header}>
            <Text style={styles.title}>Send a gift</Text>
            <Text style={styles.balance}>🪙 {wallet?.coinBalance ?? '...'}</Text>
          </View>

          <View style={styles.grid}>
            {(gifts ?? []).map((gift) => {
              const isSelected = gift.id === selectedGiftId;
              return (
                <Pressable
                  key={gift.id}
                  onPress={() => setSelectedGiftId(isSelected ? null : gift.id)}
                  style={[styles.giftCard, isSelected && styles.giftCardSelected]}
                >
                  <Image source={{ uri: resolveAssetUrl(gift.iconUrl) }} style={styles.giftIcon} />
                  <Text style={styles.giftName}>{gift.name}</Text>
                  <Text style={styles.giftPrice}>🪙 {gift.coinPrice}</Text>
                </Pressable>
              );
            })}
          </View>

          {error ? <Text style={styles.error}>{error}</Text> : null}
          {selected && !canAfford ? (
            <Text style={styles.error}>Not enough coins for {selected.name}.</Text>
          ) : null}

          <Pressable
            style={[styles.sendButton, (!selected || !canAfford || mutation.isPending) && styles.sendDisabled]}
            disabled={!selected || !canAfford || mutation.isPending}
            onPress={() => selected && mutation.mutate(selected.id)}
          >
            <Text style={styles.sendText}>
              {selected ? `Send ${selected.name} · 🪙 ${selected.coinPrice}` : 'Pick a gift'}
            </Text>
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
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: spacing.md,
  },
  title: { color: colors.text, fontSize: 19, fontWeight: '800' },
  balance: { color: colors.primary, fontSize: 15, fontWeight: '800' },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  giftCard: {
    width: '30%',
    flexGrow: 1,
    maxWidth: '32%',
    backgroundColor: colors.surfaceLight,
    borderRadius: 14,
    padding: spacing.sm,
    alignItems: 'center',
    borderWidth: 2,
    borderColor: 'transparent',
  },
  giftCardSelected: { borderColor: colors.primary },
  giftIcon: { width: 44, height: 44, borderRadius: 10 },
  giftName: { color: colors.text, fontSize: 12, fontWeight: '700', marginTop: spacing.xs },
  giftPrice: { color: colors.textMuted, fontSize: 11, marginTop: 1 },
  error: { color: colors.error, marginTop: spacing.sm },
  sendButton: {
    backgroundColor: colors.primary,
    borderRadius: 14,
    paddingVertical: spacing.md,
    alignItems: 'center',
    marginTop: spacing.md,
  },
  sendDisabled: { opacity: 0.5 },
  sendText: { color: '#1b1526', fontSize: 15, fontWeight: '800' },
});
