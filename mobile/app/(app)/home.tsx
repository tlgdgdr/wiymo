import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'expo-router';
import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import type { Intention, Me } from '@/api/types';
import { getMe, updateIntention } from '@/api/users';
import { INTENTIONS } from '@/constants/intentions';
import { colors, spacing } from '@/theme';

export default function HomeScreen() {
  const queryClient = useQueryClient();
  const { data: me } = useQuery({ queryKey: ['me'], queryFn: getMe });

  const intentionMutation = useMutation({
    mutationFn: updateIntention,
    onSuccess: (updated: Me) => {
      queryClient.setQueryData(['me'], updated);
      // Recommendations (rooms, people) will refetch off this in later phases.
      void queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    },
  });

  const current = me?.currentIntention ?? null;

  const selectIntention = (value: Intention) => {
    if (value !== current && !intentionMutation.isPending) {
      intentionMutation.mutate(value);
    }
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.header}>
          <View style={styles.avatarCircle}>
            <Text style={styles.avatarInitial}>
              {(me?.username ?? '?').charAt(0).toUpperCase()}
            </Text>
          </View>
          <View style={styles.headerText}>
            <Text style={styles.username}>{me?.username ?? '...'}</Text>
            <Text style={styles.headerSub}>Ready to connect?</Text>
          </View>
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

        <Text style={styles.sectionTitle}>Recommended Rooms</Text>
        <View style={styles.placeholderBox}>
          <Text style={styles.placeholderText}>Rooms arrive in the next phase 🏠</Text>
        </View>

        <Text style={styles.sectionTitle}>Online People</Text>
        <View style={styles.placeholderBox}>
          <Text style={styles.placeholderText}>Discovery arrives soon 👋</Text>
        </View>
      </ScrollView>
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
  avatarCircle: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.primaryDark,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarInitial: { color: colors.text, fontSize: 18, fontWeight: '800' },
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
  sectionTitle: {
    color: colors.text,
    fontSize: 17,
    fontWeight: '800',
    marginTop: spacing.lg,
    marginBottom: spacing.sm,
  },
  placeholderBox: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    padding: spacing.lg,
    alignItems: 'center',
  },
  placeholderText: { color: colors.textMuted, fontSize: 14 },
});
