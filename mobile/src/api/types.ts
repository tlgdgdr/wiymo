export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  username: string;
}

export interface ApiError {
  code: string;
  message: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  birthDate: string; // ISO yyyy-MM-dd
  countryCode?: string;
}

export interface LoginRequest {
  identifier: string;
  password: string;
}

export type Intention =
  | 'CASUAL_CHAT'
  | 'DEEP_TALK'
  | 'FLIRT'
  | 'MEET_PEOPLE'
  | 'LANGUAGE_EXCHANGE'
  | 'CHILL';

export type LanguageType = 'NATIVE' | 'LEARNING' | 'SPEAKING';

export type LanguageLevel = 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2' | 'NATIVE';

export interface UserLanguage {
  id: string;
  languageCode: string;
  type: LanguageType;
  level: LanguageLevel;
}

export interface Me {
  id: string;
  username: string;
  email: string;
  birthDate: string;
  countryCode: string | null;
  bio: string | null;
  gender: string | null;
  currentIntention: Intention | null;
  languages: UserLanguage[];
}

export interface PublicProfile {
  id: string;
  username: string;
  age: number;
  countryCode: string | null;
  bio: string | null;
  currentIntention: Intention | null;
  languages: UserLanguage[];
  online: boolean;
  lastSeenAt: string | null;
}

export interface UpdateProfileRequest {
  bio?: string;
  countryCode?: string;
  gender?: string;
}

export interface AddLanguageRequest {
  languageCode: string;
  type: LanguageType;
  level?: LanguageLevel;
}
