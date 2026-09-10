import Constants from 'expo-constants';
import { router } from 'expo-router';
import React, { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { deleteAccount } from '@/api/users';
import { ApiRequestError } from '@/api/client';
import { API_URL } from '@/config';
import { useAuthStore } from '@/store/auth';
import { colors, spacing } from '@/theme';

export default function SettingsScreen() {
  const clearSession = useAuthStore((s) => s.clearSession);

  const logout = async () => {
    await clearSession();
    router.replace('/(auth)/login');
  };

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deletePassword, setDeletePassword] = useState('');
  const [deleteState, setDeleteState] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const confirmDelete = async () => {
    if (!deletePassword || deleting) return;
    setDeleting(true);
    setDeleteState(null);
    try {
      await deleteAccount(deletePassword);
      await clearSession();
      router.replace('/(auth)/login');
    } catch (e) {
      setDeleteState(
        e instanceof ApiRequestError ? e.message : 'Could not delete the account.',
      );
    } finally {
      setDeleting(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={8}>
          <Text style={styles.back}>‹ Back</Text>
        </Pressable>
        <Text style={styles.title}>Settings</Text>
        <View style={styles.headerSpacer} />
      </View>

      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.sectionLabel}>Account</Text>
        <View style={styles.card}>
          <Pressable style={styles.row} onPress={() => router.push('/(app)/edit-profile')}>
            <Text style={styles.rowText}>Edit profile</Text>
            <Text style={styles.chevron}>›</Text>
          </Pressable>
          <View style={styles.divider} />
          <Pressable style={styles.row} onPress={() => router.push('/(app)/avatar-creator')}>
            <Text style={styles.rowText}>Edit avatar</Text>
            <Text style={styles.chevron}>›</Text>
          </Pressable>
          <View style={styles.divider} />
          <Pressable style={styles.row} onPress={() => router.push('/(app)/languages')}>
            <Text style={styles.rowText}>My languages</Text>
            <Text style={styles.chevron}>›</Text>
          </Pressable>
        </View>

        <Text style={styles.sectionLabel}>Safety</Text>
        <View style={styles.card}>
          <View style={styles.row}>
            <Text style={styles.rowText}>Blocking & reporting</Text>
          </View>
          <Text style={styles.hint}>
            Open any profile and use Block or Report. Blocked users cannot
            message you, send gifts, see your profile or find you anywhere.
          </Text>
        </View>

        <Text style={styles.sectionLabel}>About</Text>
        <View style={styles.card}>
          <View style={styles.row}>
            <Text style={styles.rowText}>Version</Text>
            <Text style={styles.rowValue}>
              {Constants.expoConfig?.version ?? '0.1.0'}
            </Text>
          </View>
          <View style={styles.divider} />
          <View style={styles.row}>
            <Text style={styles.rowText}>Server</Text>
            <Text style={styles.rowValue} numberOfLines={1}>
              {API_URL}
            </Text>
          </View>
        </View>

        <Pressable style={styles.logout} onPress={() => void logout()}>
          <Text style={styles.logoutText}>Log out</Text>
        </Pressable>

        <Text style={styles.sectionLabel}>Danger zone</Text>
        <View style={styles.card}>
          <Pressable style={styles.row} onPress={() => setDeleteOpen((v) => !v)}>
            <Text style={[styles.rowText, styles.dangerText]}>Delete my account</Text>
            <Text style={styles.chevron}>{deleteOpen ? '▾' : '›'}</Text>
          </Pressable>
          {deleteOpen ? (
            <View style={styles.deleteBox}>
              <Text style={styles.hint}>
                This is permanent. Your profile, avatar and languages are removed
                and your account is anonymized. Enter your password to confirm.
              </Text>
              <TextInput
                style={styles.passwordInput}
                value={deletePassword}
                onChangeText={setDeletePassword}
                placeholder="Password"
                placeholderTextColor={colors.textMuted}
                secureTextEntry
              />
              {deleteState ? <Text style={styles.deleteError}>{deleteState}</Text> : null}
              <Pressable
                style={[styles.deleteButton, (!deletePassword || deleting) && styles.deleteDisabled]}
                disabled={!deletePassword || deleting}
                onPress={() => void confirmDelete()}
              >
                <Text style={styles.deleteButtonText}>
                  {deleting ? 'Deleting…' : 'Delete forever'}
                </Text>
              </Pressable>
            </View>
          ) : null}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  back: { color: colors.primary, fontSize: 15, fontWeight: '700' },
  title: { color: colors.text, fontSize: 17, fontWeight: '800' },
  headerSpacer: { width: 48 },
  container: { padding: spacing.md, paddingBottom: spacing.xl },
  sectionLabel: {
    color: colors.textMuted,
    fontSize: 12,
    fontWeight: '800',
    textTransform: 'uppercase',
    letterSpacing: 0.6,
    marginTop: spacing.md,
    marginBottom: spacing.xs,
  },
  card: { backgroundColor: colors.surface, borderRadius: 16, overflow: 'hidden' },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: spacing.md,
  },
  rowText: { color: colors.text, fontSize: 15, fontWeight: '600' },
  rowValue: { color: colors.textMuted, fontSize: 13, maxWidth: '60%' },
  chevron: { color: colors.textMuted, fontSize: 18 },
  divider: { height: 1, backgroundColor: colors.surfaceLight, marginLeft: spacing.md },
  hint: {
    color: colors.textMuted,
    fontSize: 12.5,
    lineHeight: 18,
    paddingHorizontal: spacing.md,
    paddingBottom: spacing.md,
  },
  logout: {
    marginTop: spacing.xl,
    backgroundColor: colors.surface,
    borderRadius: 16,
    padding: spacing.md,
    alignItems: 'center',
  },
  logoutText: { color: colors.error, fontSize: 15, fontWeight: '700' },
  dangerText: { color: colors.error },
  deleteBox: { paddingHorizontal: spacing.md, paddingBottom: spacing.md, gap: spacing.sm },
  passwordInput: {
    backgroundColor: colors.surfaceLight,
    borderRadius: 12,
    paddingHorizontal: spacing.md,
    paddingVertical: 10,
    color: colors.text,
    fontSize: 15,
  },
  deleteError: { color: colors.error, fontSize: 12.5 },
  deleteButton: {
    backgroundColor: colors.error,
    borderRadius: 12,
    padding: spacing.md,
    alignItems: 'center',
  },
  deleteDisabled: { opacity: 0.5 },
  deleteButtonText: { color: '#1b1526', fontWeight: '800', fontSize: 14 },
});
