import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';
import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { ScrollView, StyleSheet, Text } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ApiRequestError } from '@/api/client';
import { getMe, updateProfile } from '@/api/users';
import { Button } from '@/components/Button';
import { FormTextField } from '@/components/FormTextField';
import { colors, spacing } from '@/theme';

interface EditProfileForm {
  bio: string;
  countryCode: string;
  gender: string;
}

export default function EditProfileScreen() {
  const queryClient = useQueryClient();
  const { data: me } = useQuery({ queryKey: ['me'], queryFn: getMe });

  const { control, handleSubmit, reset } = useForm<EditProfileForm>({
    defaultValues: { bio: '', countryCode: '', gender: '' },
  });

  useEffect(() => {
    if (me) {
      reset({
        bio: me.bio ?? '',
        countryCode: me.countryCode ?? '',
        gender: me.gender ?? '',
      });
    }
  }, [me, reset]);

  const mutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: (updated) => {
      queryClient.setQueryData(['me'], updated);
      router.back();
    },
  });

  const onSubmit = (values: EditProfileForm) => {
    mutation.mutate({
      bio: values.bio,
      countryCode: values.countryCode ? values.countryCode.toUpperCase() : undefined,
      gender: values.gender,
    });
  };

  const errorMessage =
    mutation.error instanceof ApiRequestError
      ? mutation.error.message
      : mutation.error
        ? 'Could not reach the server.'
        : null;

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>Edit profile</Text>

        <FormTextField
          control={control}
          name="bio"
          label="Bio"
          rules={{ maxLength: { value: 500, message: 'At most 500 characters' } }}
          placeholder="Say something about yourself"
          multiline
          numberOfLines={4}
        />
        <FormTextField
          control={control}
          name="countryCode"
          label="Country code"
          rules={{ pattern: { value: /^([a-zA-Z]{2})?$/, message: 'Two letters, e.g. TR' } }}
          placeholder="TR"
          autoCapitalize="characters"
          maxLength={2}
        />
        <FormTextField
          control={control}
          name="gender"
          label="Gender (optional)"
          rules={{ maxLength: { value: 30, message: 'At most 30 characters' } }}
          placeholder="e.g. woman, man, non-binary"
          autoCapitalize="none"
        />

        {errorMessage ? <Text style={styles.error}>{errorMessage}</Text> : null}

        <Button
          title="Save"
          loading={mutation.isPending}
          onPress={handleSubmit(onSubmit)}
        />
        <Button title="Cancel" variant="ghost" onPress={() => router.back()} />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { padding: spacing.md, gap: spacing.xs },
  title: { color: colors.text, fontSize: 24, fontWeight: '800', marginBottom: spacing.md },
  error: { color: colors.error, marginBottom: spacing.sm },
});
