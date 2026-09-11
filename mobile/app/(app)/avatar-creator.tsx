import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';
import React, { useEffect, useState } from 'react';
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getAvatarAssets, getMyAvatar, updateMyAvatar } from '@/api/avatar';
import { ApiRequestError } from '@/api/client';
import type { AvatarCategory, UserAvatar } from '@/api/types';
import { resolveAssetUrl } from '@/config';
import { Avatar } from '@/components/Avatar';
import { Button } from '@/components/Button';
import { colors, spacing } from '@/theme';

interface Tab {
  label: string;
  categories: AvatarCategory[];
}

const TABS: Tab[] = [
  { label: 'Body', categories: ['BODY'] },
  { label: 'Hair', categories: ['HAIR'] },
  { label: 'Face', categories: ['FACE', 'EYES'] },
  { label: 'Clothes', categories: ['TOP', 'BOTTOM', 'SHOES'] },
  { label: 'Extras', categories: ['ACCESSORY'] },
];

const CATEGORY_TO_FIELD: Record<AvatarCategory, keyof UserAvatar> = {
  BODY: 'bodyId',
  FACE: 'faceId',
  EYES: 'eyesId',
  HAIR: 'hairId',
  TOP: 'topId',
  BOTTOM: 'bottomId',
  SHOES: 'shoesId',
  ACCESSORY: 'accessoryId',
};

export default function AvatarCreatorScreen() {
  const queryClient = useQueryClient();
  const { data: assets } = useQuery({ queryKey: ['avatarAssets'], queryFn: getAvatarAssets });
  const { data: savedAvatar } = useQuery({ queryKey: ['myAvatar'], queryFn: getMyAvatar });

  const [draft, setDraft] = useState<UserAvatar | null>(null);
  const [tabIndex, setTabIndex] = useState(0);

  useEffect(() => {
    if (savedAvatar && !draft) {
      setDraft(savedAvatar);
    }
  }, [savedAvatar, draft]);

  const saveMutation = useMutation({
    mutationFn: updateMyAvatar,
    onSuccess: (updated) => {
      queryClient.setQueryData(['myAvatar'], updated);
      void queryClient.invalidateQueries({ queryKey: ['me'] });
      router.back();
    },
  });

  const toggleLayer = (category: AvatarCategory, assetKey: string) => {
    if (!draft) return;
    const field = CATEGORY_TO_FIELD[category];
    const isSelected = draft[field] === assetKey;
    // Body cannot be cleared; other layers toggle off when re-tapped.
    if (field === 'bodyId') {
      setDraft({ ...draft, bodyId: assetKey });
    } else {
      setDraft({ ...draft, [field]: isSelected ? null : assetKey });
    }
  };

  const errorMessage =
    saveMutation.error instanceof ApiRequestError
      ? saveMutation.error.message
      : saveMutation.error
        ? 'Could not reach the server.'
        : null;

  const tab = TABS[tabIndex];

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.preview}>
        <Avatar avatar={draft} assets={assets} size={160} />
      </View>

      <View style={styles.tabs}>
        {TABS.map((t, i) => (
          <Pressable key={t.label} onPress={() => setTabIndex(i)} style={styles.tab}>
            <Text style={[styles.tabText, i === tabIndex && styles.tabTextActive]}>{t.label}</Text>
            {i === tabIndex ? <View style={styles.tabUnderline} /> : null}
          </Pressable>
        ))}
      </View>

      <ScrollView contentContainerStyle={styles.assetList}>
        {tab.categories.map((category) => {
          const categoryAssets = (assets ?? []).filter((a) => a.category === category);
          const field = CATEGORY_TO_FIELD[category];
          return (
            <View key={category}>
              {tab.categories.length > 1 ? (
                <Text style={styles.categoryTitle}>{category}</Text>
              ) : null}
              <View style={styles.assetGrid}>
                {categoryAssets.map((asset) => {
                  const selected = draft?.[field] === asset.assetKey;
                  return (
                    <Pressable
                      key={asset.assetKey}
                      onPress={() => toggleLayer(category, asset.assetKey)}
                      style={[styles.assetCard, selected && styles.assetCardSelected]}
                    >
                      <Image source={{ uri: resolveAssetUrl(asset.imageUrl) }} style={styles.assetImage} />
                      <Text style={styles.assetName} numberOfLines={1}>
                        {asset.displayName}
                      </Text>
                      {asset.premium ? (
                        <Text style={styles.premiumBadge}>★ {asset.coinPrice}</Text>
                      ) : null}
                    </Pressable>
                  );
                })}
              </View>
            </View>
          );
        })}
      </ScrollView>

      <View style={styles.footer}>
        {errorMessage ? <Text style={styles.error}>{errorMessage}</Text> : null}
        <Button
          title="Save avatar"
          loading={saveMutation.isPending}
          disabled={!draft}
          onPress={() => draft && saveMutation.mutate(draft)}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  preview: {
    alignItems: 'center',
    paddingVertical: spacing.md,
    backgroundColor: colors.surface,
    borderBottomLeftRadius: 24,
    borderBottomRightRadius: 24,
  },
  tabs: {
    flexDirection: 'row',
    paddingHorizontal: spacing.sm,
    marginTop: spacing.sm,
  },
  tab: { flex: 1, alignItems: 'center', paddingVertical: spacing.sm },
  tabText: { color: colors.textMuted, fontSize: 13, fontWeight: '700' },
  tabTextActive: { color: colors.primary },
  tabUnderline: {
    height: 3,
    alignSelf: 'stretch',
    marginHorizontal: spacing.md,
    marginTop: spacing.xs,
    borderRadius: 2,
    backgroundColor: colors.primary,
  },
  assetList: { padding: spacing.md, paddingBottom: spacing.xl },
  categoryTitle: {
    color: colors.textMuted,
    fontSize: 12,
    fontWeight: '800',
    marginTop: spacing.sm,
    marginBottom: spacing.xs,
  },
  assetGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  assetCard: {
    width: '30%',
    flexGrow: 1,
    maxWidth: '32%',
    backgroundColor: colors.surface,
    borderRadius: 14,
    padding: spacing.sm,
    alignItems: 'center',
    borderWidth: 2,
    borderColor: 'transparent',
  },
  assetCardSelected: { borderColor: colors.primary, backgroundColor: colors.surfaceLight },
  assetImage: { width: 64, height: 64, borderRadius: 8 },
  assetName: { color: colors.text, fontSize: 11, marginTop: spacing.xs },
  premiumBadge: { color: colors.primary, fontSize: 10, fontWeight: '800', marginTop: 2 },
  footer: { padding: spacing.md, gap: spacing.xs },
  error: { color: colors.error },
});
