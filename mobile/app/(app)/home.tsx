import { router } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Button } from '@/components/Button';
import { useAuthStore } from '@/store/auth';
import { colors, spacing } from '@/theme';

// Phase 1 placeholder: proves the auth loop works end-to-end.
// The real Home (intentions, rooms, discovery) arrives in later phases.
export default function HomeScreen() {
  const username = useAuthStore((s) => s.username);
  const clearSession = useAuthStore((s) => s.clearSession);

  const logout = async () => {
    await clearSession();
    router.replace('/(auth)/login');
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Hey, {username ?? 'friend'} 👋</Text>
      <Text style={styles.subtitle}>
        You are logged in. Intentions, rooms and avatars land in the next phases.
      </Text>
      <Button title="Log out" variant="ghost" onPress={() => void logout()} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.lg,
    gap: spacing.md,
  },
  title: {
    color: colors.text,
    fontSize: 26,
    fontWeight: '800',
  },
  subtitle: {
    color: colors.textMuted,
    fontSize: 15,
    textAlign: 'center',
  },
});
