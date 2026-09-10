import { useQuery } from '@tanstack/react-query';
import { router } from 'expo-router';
import React, { useState } from 'react';
import { FlatList, ImageBackground, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { listRooms } from '@/api/rooms';
import { getMe } from '@/api/users';
import { intentionEmoji, intentionLabel } from '@/constants/intentions';
import { colors, spacing } from '@/theme';

export default function RoomListScreen() {
  const { data: me } = useQuery({ queryKey: ['me'], queryFn: getMe });
  const [onlyMyMood, setOnlyMyMood] = useState(false);

  const intentionFilter = onlyMyMood ? me?.currentIntention ?? null : null;
  const { data: rooms } = useQuery({
    queryKey: ['rooms', intentionFilter],
    queryFn: () => listRooms(intentionFilter),
    refetchInterval: 20_000,
  });

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <Text style={styles.title}>Rooms</Text>
        {me?.currentIntention ? (
          <Pressable
            onPress={() => setOnlyMyMood((v) => !v)}
            style={[styles.filterChip, onlyMyMood && styles.filterChipActive]}
          >
            <Text style={[styles.filterText, onlyMyMood && styles.filterTextActive]}>
              {intentionEmoji(me.currentIntention)} My mood
            </Text>
          </Pressable>
        ) : null}
      </View>

      <FlatList
        data={rooms ?? []}
        keyExtractor={(room) => room.id}
        contentContainerStyle={styles.list}
        renderItem={({ item: room }) => (
          <Pressable onPress={() => router.push(`/(app)/room/${room.id}`)}>
            <ImageBackground
              source={{ uri: room.backgroundImageUrl }}
              style={styles.card}
              imageStyle={styles.cardImage}
            >
              <View style={styles.cardOverlay}>
                <Text style={styles.roomName}>{room.name}</Text>
                {room.description ? (
                  <Text style={styles.roomDescription}>{room.description}</Text>
                ) : null}
                <View style={styles.cardFooter}>
                  <Text style={styles.population}>
                    👥 {room.population}/{room.maxUsers}
                  </Text>
                  {room.intention ? (
                    <Text style={styles.roomMood}>
                      {intentionEmoji(room.intention)} {intentionLabel(room.intention)}
                    </Text>
                  ) : null}
                </View>
              </View>
            </ImageBackground>
          </Pressable>
        )}
        ListEmptyComponent={
          <Text style={styles.empty}>No rooms for this mood right now.</Text>
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: spacing.md,
  },
  title: { color: colors.text, fontSize: 24, fontWeight: '800' },
  filterChip: {
    backgroundColor: colors.surface,
    borderRadius: 999,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  filterChipActive: { borderColor: colors.primary, backgroundColor: colors.surfaceLight },
  filterText: { color: colors.textMuted, fontSize: 13, fontWeight: '700' },
  filterTextActive: { color: colors.primary },
  list: { padding: spacing.md, gap: spacing.sm, paddingBottom: spacing.xl },
  card: { height: 132, borderRadius: 18, overflow: 'hidden' },
  cardImage: { borderRadius: 18 },
  cardOverlay: {
    flex: 1,
    backgroundColor: 'rgba(10,6,18,0.45)',
    padding: spacing.md,
    justifyContent: 'flex-end',
  },
  roomName: { color: colors.text, fontSize: 19, fontWeight: '800' },
  roomDescription: { color: 'rgba(244,241,250,0.8)', fontSize: 12, marginTop: 2 },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: spacing.sm,
  },
  population: { color: colors.text, fontSize: 12, fontWeight: '700' },
  roomMood: { color: 'rgba(244,241,250,0.85)', fontSize: 12 },
  empty: { color: colors.textMuted, textAlign: 'center', marginTop: spacing.xl },
});
