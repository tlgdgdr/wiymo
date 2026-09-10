import { Redirect } from 'expo-router';
import React from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';

import { useAuthStore } from '@/store/auth';
import { colors, spacing } from '@/theme';

// Splash: waits for the persisted session to load, then routes.
export default function SplashScreen() {
  const hydrated = useAuthStore((s) => s.hydrated);
  const accessToken = useAuthStore((s) => s.accessToken);

  if (!hydrated) {
    return (
      <View style={styles.container}>
        <Text style={styles.logo}>SocialWorld</Text>
        <ActivityIndicator color={colors.primary} />
      </View>
    );
  }

  return <Redirect href={accessToken ? '/(app)/home' : '/(auth)/login'} />;
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.lg,
  },
  logo: {
    color: colors.text,
    fontSize: 32,
    fontWeight: '800',
    letterSpacing: 1,
  },
});
