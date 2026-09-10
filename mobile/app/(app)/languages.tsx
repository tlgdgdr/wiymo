import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import React, { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ApiRequestError } from '@/api/client';
import { addLanguage, listMyLanguages, removeLanguage } from '@/api/languages';
import type { LanguageLevel, LanguageType } from '@/api/types';
import { Button } from '@/components/Button';
import { colors, spacing } from '@/theme';

const TYPES: LanguageType[] = ['NATIVE', 'LEARNING', 'SPEAKING'];
const LEVELS: LanguageLevel[] = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'];

export default function LanguagesScreen() {
  const queryClient = useQueryClient();
  const { data: languages } = useQuery({ queryKey: ['myLanguages'], queryFn: listMyLanguages });

  const [code, setCode] = useState('');
  const [type, setType] = useState<LanguageType>('LEARNING');
  const [level, setLevel] = useState<LanguageLevel>('A2');
  const [error, setError] = useState<string | null>(null);

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['myLanguages'] });
    void queryClient.invalidateQueries({ queryKey: ['me'] });
  };

  const addMutation = useMutation({
    mutationFn: addLanguage,
    onSuccess: () => {
      setCode('');
      setError(null);
      invalidate();
    },
    onError: (e) =>
      setError(e instanceof ApiRequestError ? e.message : 'Could not reach the server.'),
  });

  const removeMutation = useMutation({
    mutationFn: removeLanguage,
    onSuccess: invalidate,
  });

  const submit = () => {
    if (!/^[a-zA-Z]{2,3}$/.test(code)) {
      setError('Language code must be 2-3 letters, e.g. "tr", "en".');
      return;
    }
    addMutation.mutate({
      languageCode: code.toLowerCase(),
      type,
      level: type === 'NATIVE' ? undefined : level,
    });
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>My languages</Text>

        {(languages ?? []).map((lang) => (
          <View key={lang.id} style={styles.row}>
            <Text style={styles.rowText}>
              {lang.languageCode.toUpperCase()} · {lang.type.toLowerCase()} · {lang.level}
            </Text>
            <Pressable onPress={() => removeMutation.mutate(lang.id)} hitSlop={8}>
              <Text style={styles.remove}>Remove</Text>
            </Pressable>
          </View>
        ))}
        {languages && languages.length === 0 ? (
          <Text style={styles.empty}>Nothing yet — add your first language below.</Text>
        ) : null}

        <Text style={styles.subtitle}>Add a language</Text>

        <TextInput
          style={styles.input}
          value={code}
          onChangeText={setCode}
          placeholder='Code, e.g. "pl"'
          placeholderTextColor={colors.textMuted}
          autoCapitalize="none"
          autoCorrect={false}
          maxLength={3}
        />

        <Text style={styles.pickerLabel}>Type</Text>
        <View style={styles.chips}>
          {TYPES.map((t) => (
            <Pressable
              key={t}
              onPress={() => setType(t)}
              style={[styles.chip, type === t && styles.chipActive]}
            >
              <Text style={[styles.chipText, type === t && styles.chipTextActive]}>{t}</Text>
            </Pressable>
          ))}
        </View>

        {type !== 'NATIVE' ? (
          <>
            <Text style={styles.pickerLabel}>Level (CEFR)</Text>
            <View style={styles.chips}>
              {LEVELS.map((l) => (
                <Pressable
                  key={l}
                  onPress={() => setLevel(l)}
                  style={[styles.chip, level === l && styles.chipActive]}
                >
                  <Text style={[styles.chipText, level === l && styles.chipTextActive]}>{l}</Text>
                </Pressable>
              ))}
            </View>
          </>
        ) : null}

        {error ? <Text style={styles.error}>{error}</Text> : null}

        <Button title="Add language" loading={addMutation.isPending} onPress={submit} />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { padding: spacing.md, paddingBottom: spacing.xl },
  title: { color: colors.text, fontSize: 24, fontWeight: '800', marginBottom: spacing.md },
  subtitle: {
    color: colors.text,
    fontSize: 17,
    fontWeight: '800',
    marginTop: spacing.lg,
    marginBottom: spacing.sm,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.surface,
    borderRadius: 12,
    padding: spacing.md,
    marginBottom: spacing.xs,
  },
  rowText: { color: colors.text, fontSize: 15 },
  remove: { color: colors.error, fontWeight: '700', fontSize: 13 },
  empty: { color: colors.textMuted, fontSize: 14 },
  input: {
    backgroundColor: colors.surface,
    borderRadius: 12,
    paddingHorizontal: spacing.md,
    paddingVertical: 12,
    color: colors.text,
    fontSize: 16,
    marginBottom: spacing.sm,
  },
  pickerLabel: {
    color: colors.textMuted,
    fontSize: 13,
    fontWeight: '600',
    marginBottom: spacing.xs,
    marginTop: spacing.xs,
  },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xs, marginBottom: spacing.sm },
  chip: {
    backgroundColor: colors.surface,
    borderRadius: 999,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  chipActive: { borderColor: colors.primary, backgroundColor: colors.surfaceLight },
  chipText: { color: colors.textMuted, fontSize: 13, fontWeight: '700' },
  chipTextActive: { color: colors.primary },
  error: { color: colors.error, marginBottom: spacing.sm },
});
