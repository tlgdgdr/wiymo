import { useQuery } from '@tanstack/react-query';
import { router } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getAvatarAssets } from '@/api/avatar';
import { getMe } from '@/api/users';
import { Avatar } from '@/components/Avatar';
import { Button } from '@/components/Button';
import { intentionEmoji, intentionLabel } from '@/constants/intentions';
import { colors, spacing } from '@/theme';

export default function ProfileScreen() {
  const { data: me } = useQuery({ queryKey: ['me'], queryFn: getMe });
  const { data: assets } = useQuery({ queryKey: ['avatarAssets'], queryFn: getAvatarAssets });
  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.avatarWrap}>
          <Avatar avatar={me?.avatar} assets={assets} size={120} fallbackInitial={me?.username} />
        </View>
        <Text style={styles.username}>{me?.username ?? '...'}</Text>
        <Text style={styles.mood}>
          {intentionEmoji(me?.currentIntention)} {intentionLabel(me?.currentIntention)}
        </Text>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>Bio</Text>
          <Text style={styles.cardBody}>{me?.bio ?? 'No bio yet.'}</Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>Country</Text>
          <Text style={styles.cardBody}>{me?.countryCode ?? 'Not set'}</Text>
        </View>

        {/*
          Languages are hidden for the MVP launch. The screen, the API and the
          data all still work — restoring the feature is putting this card and
          the Settings row back.
        */}

        <View style={styles.actions}>
          <Button title="Edit avatar" onPress={() => router.push('/(app)/avatar-creator')} />
          <Button title="Edit profile" onPress={() => router.push('/(app)/edit-profile')} />
          <Button title="Settings" variant="ghost" onPress={() => router.push('/(app)/settings')} />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { padding: spacing.md, alignItems: 'stretch' },
  avatarWrap: { alignSelf: 'center', marginTop: spacing.lg },
  username: {
    color: colors.text,
    fontSize: 24,
    fontWeight: '800',
    textAlign: 'center',
    marginTop: spacing.sm,
  },
  mood: {
    color: colors.textMuted,
    fontSize: 14,
    textAlign: 'center',
    marginBottom: spacing.lg,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  cardTitle: { color: colors.textMuted, fontSize: 12, fontWeight: '700', marginBottom: spacing.xs },
  cardBody: { color: colors.text, fontSize: 15, marginBottom: 2 },
  actions: { marginTop: spacing.md, gap: spacing.sm },
});
