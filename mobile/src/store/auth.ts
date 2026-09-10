import * as SecureStore from 'expo-secure-store';
import { create } from 'zustand';

import type { AuthResponse } from '@/api/types';

const ACCESS_TOKEN_KEY = 'sw.accessToken';
const REFRESH_TOKEN_KEY = 'sw.refreshToken';
const USER_ID_KEY = 'sw.userId';
const USERNAME_KEY = 'sw.username';

interface AuthState {
  hydrated: boolean;
  accessToken: string | null;
  refreshToken: string | null;
  userId: string | null;
  username: string | null;
  hydrate: () => Promise<void>;
  setSession: (auth: AuthResponse) => Promise<void>;
  clearSession: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  hydrated: false,
  accessToken: null,
  refreshToken: null,
  userId: null,
  username: null,

  hydrate: async () => {
    const [accessToken, refreshToken, userId, username] = await Promise.all([
      SecureStore.getItemAsync(ACCESS_TOKEN_KEY),
      SecureStore.getItemAsync(REFRESH_TOKEN_KEY),
      SecureStore.getItemAsync(USER_ID_KEY),
      SecureStore.getItemAsync(USERNAME_KEY),
    ]);
    set({ hydrated: true, accessToken, refreshToken, userId, username });
  },

  setSession: async (auth) => {
    await Promise.all([
      SecureStore.setItemAsync(ACCESS_TOKEN_KEY, auth.accessToken),
      SecureStore.setItemAsync(REFRESH_TOKEN_KEY, auth.refreshToken),
      SecureStore.setItemAsync(USER_ID_KEY, auth.userId),
      SecureStore.setItemAsync(USERNAME_KEY, auth.username),
    ]);
    set({
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      userId: auth.userId,
      username: auth.username,
    });
  },

  clearSession: async () => {
    await Promise.all([
      SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY),
      SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY),
      SecureStore.deleteItemAsync(USER_ID_KEY),
      SecureStore.deleteItemAsync(USERNAME_KEY),
    ]);
    set({ accessToken: null, refreshToken: null, userId: null, username: null });
  },
}));
