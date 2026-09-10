import { useMutation } from '@tanstack/react-query';
import { Link, router } from 'expo-router';
import React from 'react';
import { useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';

import { register } from '@/api/auth';
import { ApiRequestError } from '@/api/client';
import { Button } from '@/components/Button';
import { FormTextField } from '@/components/FormTextField';
import { useAuthStore } from '@/store/auth';
import { colors, spacing } from '@/theme';

interface RegisterForm {
  username: string;
  email: string;
  password: string;
  birthDate: string;
  countryCode: string;
}

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

function isAdult(birthDate: string): boolean {
  const date = new Date(`${birthDate}T00:00:00Z`);
  if (Number.isNaN(date.getTime())) return false;
  const cutoff = new Date();
  cutoff.setFullYear(cutoff.getFullYear() - 18);
  return date <= cutoff;
}

export default function RegisterScreen() {
  const setSession = useAuthStore((s) => s.setSession);
  const { control, handleSubmit } = useForm<RegisterForm>({
    defaultValues: { username: '', email: '', password: '', birthDate: '', countryCode: '' },
  });

  const mutation = useMutation({
    mutationFn: register,
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

  const onSubmit = (values: RegisterForm) => {
    mutation.mutate({
      username: values.username,
      email: values.email,
      password: values.password,
      birthDate: values.birthDate,
      countryCode: values.countryCode ? values.countryCode.toUpperCase() : undefined,
    });
  };

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>Create your account</Text>
        <Text style={styles.subtitle}>You must be 18 or older to join.</Text>

        <FormTextField
          control={control}
          name="username"
          label="Username"
          rules={{
            required: 'Required',
            minLength: { value: 3, message: 'At least 3 characters' },
            maxLength: { value: 30, message: 'At most 30 characters' },
            pattern: {
              value: /^[a-zA-Z0-9_.]+$/,
              message: 'Only letters, digits, "_" and "."',
            },
          }}
          autoCapitalize="none"
          autoCorrect={false}
        />
        <FormTextField
          control={control}
          name="email"
          label="Email"
          rules={{
            required: 'Required',
            pattern: { value: /.+@.+\..+/, message: 'Enter a valid email' },
          }}
          keyboardType="email-address"
          autoCapitalize="none"
          autoCorrect={false}
        />
        <FormTextField
          control={control}
          name="password"
          label="Password"
          rules={{
            required: 'Required',
            minLength: { value: 8, message: 'At least 8 characters' },
          }}
          secureTextEntry
        />
        <FormTextField
          control={control}
          name="birthDate"
          label="Birth date (YYYY-MM-DD)"
          rules={{
            required: 'Required',
            pattern: { value: DATE_PATTERN, message: 'Use YYYY-MM-DD' },
            validate: (v: string) => isAdult(v) || 'You must be at least 18',
          }}
          placeholder="1998-04-12"
          autoCapitalize="none"
        />
        <FormTextField
          control={control}
          name="countryCode"
          label="Country code (optional)"
          rules={{
            pattern: { value: /^[a-zA-Z]{2}$/, message: 'Two letters, e.g. TR' },
          }}
          placeholder="TR"
          autoCapitalize="characters"
          maxLength={2}
        />

        {errorMessage ? <Text style={styles.error}>{errorMessage}</Text> : null}

        <Button
          title="Sign up"
          loading={mutation.isPending}
          onPress={handleSubmit(onSubmit)}
        />

        <View style={styles.footer}>
          <Text style={styles.footerText}>Already have an account? </Text>
          <Link href="/(auth)/login" style={styles.link}>
            Log in
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
