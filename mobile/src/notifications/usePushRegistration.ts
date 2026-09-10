import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';
import { useEffect } from 'react';

import { registerPushToken } from '@/api/users';
import { useAuthStore } from '@/store/auth';

/**
 * Best-effort push registration after login. Silently no-ops where push
 * isn't available (simulators, Expo Go without a projectId, denied
 * permission) — the app never depends on it.
 */
export function usePushRegistration() {
  const accessToken = useAuthStore((s) => s.accessToken);

  useEffect(() => {
    if (!accessToken || !Device.isDevice) return;
    let cancelled = false;

    (async () => {
      try {
        const { status } = await Notifications.getPermissionsAsync();
        const granted =
          status === 'granted' ||
          (await Notifications.requestPermissionsAsync()).status === 'granted';
        if (!granted || cancelled) return;

        const token = (await Notifications.getExpoPushTokenAsync()).data;
        if (token && !cancelled) {
          await registerPushToken(token);
        }
      } catch {
        // Push is optional; never surface registration failures.
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [accessToken]);
}
