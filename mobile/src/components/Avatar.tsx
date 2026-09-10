import React, { useMemo } from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';

import type { AvatarAsset, UserAvatar } from '@/api/types';
import { colors } from '@/theme';

/** Layer render order, bottom to top. */
const LAYER_ORDER: (keyof UserAvatar)[] = [
  'bodyId',
  'bottomId',
  'shoesId',
  'topId',
  'faceId',
  'eyesId',
  'hairId',
  'accessoryId',
];

interface Props {
  avatar: UserAvatar | null | undefined;
  /** Full asset catalog (from GET /api/avatar/assets) used to resolve keys to image URLs. */
  assets: AvatarAsset[] | undefined;
  size: number;
  /** Shown when the avatar or assets are not loaded yet. */
  fallbackInitial?: string;
}

/**
 * Layered 2D avatar: transparent PNGs (same canvas per layer) stacked with
 * absolute positioning. All layers share the full component bounds, so no
 * per-layer coordinates are needed.
 */
export function Avatar({ avatar, assets, size, fallbackInitial }: Props) {
  const urlByKey = useMemo(() => {
    const map = new Map<string, string>();
    for (const asset of assets ?? []) {
      map.set(asset.assetKey, asset.imageUrl);
    }
    return map;
  }, [assets]);

  const layers = avatar
    ? LAYER_ORDER.map((layer) => {
        const key = avatar[layer];
        return key ? urlByKey.get(key) : undefined;
      }).filter((url): url is string => !!url)
    : [];

  if (layers.length === 0) {
    return (
      <View style={[styles.fallback, { width: size, height: size, borderRadius: size / 2 }]}>
        <Text style={[styles.fallbackText, { fontSize: size * 0.4 }]}>
          {(fallbackInitial ?? '?').charAt(0).toUpperCase()}
        </Text>
      </View>
    );
  }

  return (
    <View style={{ width: size, height: size }}>
      {layers.map((url, index) => (
        <Image
          key={`${url}-${index}`}
          source={{ uri: url }}
          style={[StyleSheet.absoluteFillObject, { width: size, height: size }]}
          resizeMode="contain"
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  fallback: {
    backgroundColor: colors.primaryDark,
    alignItems: 'center',
    justifyContent: 'center',
  },
  fallbackText: {
    color: colors.text,
    fontWeight: '800',
  },
});
