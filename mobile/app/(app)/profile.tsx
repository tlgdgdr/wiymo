import { useQuery } from '@tanstack/react-query';
import { Link, router } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '@/components/Button';
import { getMe } from '@/api/users';
import { intentionEmoji, intentionLabel } from '@/constants/intentions';
import { useAuthStore } from '@/store/auth';
import { colors, spacing } from '@/theme';

export default function ProfileScreen() {
  const { data: me } = useQuery({ queryKey: ['me'], queryFn: getMe });
  const clearSession = useAuthStore((s) => s.clearSession);

  const logout = async () => {
    await clearSession();
    router.replace('/(auth)/login');
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.avatarCircle}>
          <Text style={styles.avatarInitial}>
            {(me?.username ?? '?').charAt(0).toUpperCase()}
          </Text>
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

        <View style={styles.card}>
          <Text style={styles.cardTitle}>Languages</Text>
          {me?.languages.length ? (
            me.languages.map((lang) => (
              <Text key={lang.id} style={styles.cardBody}>
                {lang.languageCode.toUpperCase()} · {lang.type.toLowerCase()} · {lang.level}
              </Text>
            ))
          ) : (
            <Text style={styles.cardBody}>No languages added.</Text>
          )}
          <Link href="/(app)/languages" style={styles.link}>
            Manage languages
          </Link>
        </View>

        <View style={styles.actions}>
          <Button title="Edit profile" onPress={() => router.push('/(app)/edit-profile')} />
          <Button title="Log out" variant="ghost" onPress={() => void logout()} />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { padding: spacing.md, alignItems: 'stretch' },
  avatarCircle: {
    width: 88,
    height: 88,
    borderRadius: 44,
    backgroundColor: colors.primaryDark,
    alignItems: 'center',
    justifyContent: 'center',
    alignSelf: 'center',
    marginTop: spacing.lg,
  },
  avatarInitial: { color: colors.text, fontSize: 36, fontWeight: '800' },
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
  link: { color: colors.primary, fontWeight: '700', marginTop: spacing.sm },
  actions: { marginTop: spacing.md, gap: spacing.sm },
});
