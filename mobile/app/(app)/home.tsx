import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, router } from 'expo-router';
import React, { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getAvatarAssets } from '@/api/avatar';
import { discoverUsers } from '@/api/discovery';
import { listMyGames } from '@/api/games';
import { getMyWallet } from '@/api/gifts';
import { listRooms } from '@/api/rooms';
import type { Intention, Me } from '@/api/types';
import { getMe, updateIntention } from '@/api/users';
import { Avatar } from '@/components/Avatar';
import { UserProfileModal } from '@/components/UserProfileModal';
import { INTENTIONS } from '@/constants/intentions';
import { colors, spacing } from '@/theme';

export default function HomeScreen() {
  const queryClient = useQueryClient();
  const { data: me } = useQuery({ queryKey: ['me'], queryFn: getMe });
  const { data: assets } = useQuery({ queryKey: ['avatarAssets'], queryFn: getAvatarAssets });
  const { data: wallet } = useQuery({ queryKey: ['wallet'], queryFn: getMyWallet });
  const { data: games } = useQuery({ queryKey: ['games'], queryFn: listMyGames });
  const current = me?.currentIntention ?? null;
  const { data: recommendedRooms } = useQuery({
    queryKey: ['recommendations', 'rooms', current],
    queryFn: () => listRooms(current),
  });
  const { data: people } = useQuery({
    queryKey: ['recommendations', 'people', current],
    queryFn: () => discoverUsers({ intention: current ?? undefined }),
  });
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);

  const intentionMutation = useMutation({
    mutationFn: updateIntention,
    onSuccess: (updated: Me) => {
      queryClient.setQueryData(['me'], updated);
      // Recommendations (rooms, people) will refetch off this in later phases.
      void queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    },
  });

  const selectIntention = (value: Intention) => {
    if (value !== current && !intentionMutation.isPending) {
      intentionMutation.mutate(value);
    }
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.header}>
          <Avatar
            avatar={me?.avatar}
            assets={assets}
            size={44}
            fallbackInitial={me?.username}
          />
          <View style={styles.headerText}>
            <Text style={styles.username}>{me?.username ?? '...'}</Text>
            <Text style={styles.headerSub}>🪙 {wallet?.coinBalance ?? '...'}</Text>
          </View>
          <Link href="/(app)/game" style={styles.profileLink}>
            {games && games.length > 0 ? '🎲 •' : '🎲'}
          </Link>
          <Link href="/(app)/chats" style={styles.profileLink}>
            Chats
          </Link>
          <Link href="/(app)/profile" style={styles.profileLink}>
            Profile
          </Link>
        </View>

        <Text style={styles.question}>What are you in the mood for?</Text>

        <View style={styles.grid}>
          {INTENTIONS.map((item) => {
            const active = item.value === current;
            return (
              <Pressable
                key={item.value}
                onPress={() => selectIntention(item.value)}
                style={({ pressed }) => [
                  styles.card,
                  active && styles.cardActive,
                  pressed && styles.cardPressed,
                ]}
              >
                <Text style={styles.cardEmoji}>{item.emoji}</Text>
                <Text style={[styles.cardLabel, active && styles.cardLabelActive]}>
                  {item.label}
                </Text>
                <Text style={styles.cardTagline}>{item.tagline}</Text>
              </Pressable>
            );
          })}
        </View>

        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Recommended Rooms</Text>
          <Link href="/(app)/rooms" style={styles.sectionLink}>
            See all
          </Link>
        </View>
        {(recommendedRooms ?? []).slice(0, 3).map((room) => (
          <Pressable
            key={room.id}
            onPress={() => router.push(`/(app)/room/${room.id}`)}
            style={({ pressed }) => [styles.roomRow, pressed && styles.cardPressed]}
          >
            <View style={styles.roomRowText}>
              <Text style={styles.roomRowName}>{room.name}</Text>
              {room.description ? (
                <Text style={styles.roomRowDescription} numberOfLines={1}>
                  {room.description}
                </Text>
              ) : null}
            </View>
            <Text style={styles.roomRowPopulation}>👥 {room.population}</Text>
          </Pressable>
        ))}
        {recommendedRooms && recommendedRooms.length === 0 ? (
          <View style={styles.placeholderBox}>
            <Text style={styles.placeholderText}>No rooms for this mood yet.</Text>
          </View>
        ) : null}

        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>People For You</Text>
          <Link href="/(app)/connections" style={styles.sectionLink}>
            Connections
          </Link>
        </View>
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          <View style={styles.peopleRow}>
            {(people ?? []).map((person) => (
              <Pressable
                key={person.id}
                onPress={() => setSelectedUserId(person.id)}
                style={({ pressed }) => [styles.personCard, pressed && styles.cardPressed]}
              >
                <Avatar
                  avatar={person.avatar}
                  assets={assets}
                  size={56}
                  fallbackInitial={person.username}
                />
                {person.online ? <View style={styles.personOnlineDot} /> : null}
                <Text style={styles.personName} numberOfLines={1}>
                  {person.username}
                </Text>
                <Text style={styles.personAge}>{person.age}</Text>
              </Pressable>
            ))}
            {people && people.length === 0 ? (
              <View style={styles.placeholderBox}>
                <Text style={styles.placeholderText}>No one around right now.</Text>
              </View>
            ) : null}
          </View>
        </ScrollView>
      </ScrollView>

      <UserProfileModal userId={selectedUserId} onClose={() => setSelectedUserId(null)} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { padding: spacing.md, paddingBottom: spacing.xl },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: spacing.lg,
  },
  headerText: { flex: 1, marginLeft: spacing.sm },
  username: { color: colors.text, fontSize: 17, fontWeight: '700' },
  headerSub: { color: colors.textMuted, fontSize: 12 },
  profileLink: { color: colors.primary, fontWeight: '700', padding: spacing.sm },
  question: {
    color: colors.text,
    fontSize: 22,
    fontWeight: '800',
    marginBottom: spacing.md,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    padding: spacing.md,
    width: '48%',
    flexGrow: 1,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  cardActive: {
    borderColor: colors.primary,
    backgroundColor: colors.surfaceLight,
  },
  cardPressed: { opacity: 0.8 },
  cardEmoji: { fontSize: 26, marginBottom: spacing.xs },
  cardLabel: { color: colors.text, fontSize: 15, fontWeight: '700' },
  cardLabelActive: { color: colors.primary },
  cardTagline: { color: colors.textMuted, fontSize: 12, marginTop: 2 },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    marginTop: spacing.lg,
    marginBottom: spacing.sm,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 17,
    fontWeight: '800',
  },
  sectionLink: { color: colors.primary, fontSize: 13, fontWeight: '700' },
  roomRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: 14,
    padding: spacing.md,
    marginBottom: spacing.xs,
  },
  roomRowText: { flex: 1 },
  roomRowName: { color: colors.text, fontSize: 15, fontWeight: '700' },
  roomRowDescription: { color: colors.textMuted, fontSize: 12, marginTop: 1 },
  roomRowPopulation: { color: colors.textMuted, fontSize: 12, fontWeight: '700' },
  peopleRow: { flexDirection: 'row', gap: spacing.sm },
  personCard: {
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: 14,
    padding: spacing.sm,
    width: 88,
  },
  personOnlineDot: {
    position: 'absolute',
    top: spacing.sm,
    right: spacing.sm,
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: colors.success,
  },
  personName: { color: colors.text, fontSize: 12, fontWeight: '700', marginTop: spacing.xs },
  personAge: { color: colors.textMuted, fontSize: 11 },
  placeholderBox: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    padding: spacing.lg,
    alignItems: 'center',
  },
  placeholderText: { color: colors.textMuted, fontSize: 14 },
});
