import { useMutation } from '@tanstack/react-query';
import { Link, router } from 'expo-router';
import React from 'react';
import { useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';

import { login } from '@/api/auth';
import { ApiRequestError } from '@/api/client';
import { Button } from '@/components/Button';
import { FormTextField } from '@/components/FormTextField';
import { useAuthStore } from '@/store/auth';
import { colors, spacing } from '@/theme';

interface LoginForm {
  identifier: string;
  password: string;
}

export default function LoginScreen() {
  const setSession = useAuthStore((s) => s.setSession);
  const { control, handleSubmit } = useForm<LoginForm>({
    defaultValues: { identifier: '', password: '' },
  });

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: async (auth) => {
      await setSession(auth);
      router.replace('/(app)/home');
    },
  });

  const errorMessage =
    mutation.error instanceof ApiRequestError
      ? mutation.error.message
      : mutation.error
        ? 'Could not reach the server.'
        : null;

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>Welcome back</Text>
        <Text style={styles.subtitle}>Log in to keep your connections going.</Text>

        <FormTextField
          control={control}
          name="identifier"
          label="Username or email"
          rules={{ required: 'Required' }}
          autoCapitalize="none"
          autoCorrect={false}
        />
        <FormTextField
          control={control}
          name="password"
          label="Password"
          rules={{ required: 'Required' }}
          secureTextEntry
        />

        {errorMessage ? <Text style={styles.error}>{errorMessage}</Text> : null}

        <Button
          title="Log in"
          loading={mutation.isPending}
          onPress={handleSubmit((values) => mutation.mutate(values))}
        />

        <View style={styles.footer}>
          <Text style={styles.footerText}>New here? </Text>
          <Link href="/(auth)/register" style={styles.link}>
            Create an account
          </Link>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.background },
  container: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: spacing.lg,
  },
  title: {
    color: colors.text,
    fontSize: 28,
    fontWeight: '800',
    marginBottom: spacing.xs,
  },
  subtitle: {
    color: colors.textMuted,
    fontSize: 15,
    marginBottom: spacing.xl,
  },
  error: {
    color: colors.error,
    marginBottom: spacing.md,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginTop: spacing.lg,
  },
  footerText: { color: colors.textMuted },
  link: { color: colors.primary, fontWeight: '700' },
});
