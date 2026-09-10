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
  avatar: UserAvatar;
}

export interface PublicProfile {
  id: string;
  username: string;
  age: number;
  countryCode: string | null;
  bio: string | null;
  currentIntention: Intention | null;
  languages: UserLanguage[];
  avatar: UserAvatar;
  online: boolean;
  lastSeenAt: string | null;
}

export interface UpdateProfileRequest {
  bio?: string;
  countryCode?: string;
  gender?: string;
}

export type AvatarCategory =
  | 'BODY'
  | 'BOTTOM'
  | 'SHOES'
  | 'TOP'
  | 'FACE'
  | 'EYES'
  | 'HAIR'
  | 'ACCESSORY';

export interface AvatarAsset {
  id: string;
  category: AvatarCategory;
  assetKey: string;
  displayName: string;
  imageUrl: string;
  premium: boolean;
  coinPrice: number;
  sortOrder: number;
}

/** Asset keys per layer; null layers are unset. */
export interface UserAvatar {
  bodyId: string;
  faceId: string | null;
  eyesId: string | null;
  hairId: string | null;
  topId: string | null;
  bottomId: string | null;
  shoesId: string | null;
  accessoryId: string | null;
}

export type RoomTheme = 'CAFE' | 'LOUNGE' | 'NIGHT' | 'LANGUAGE' | 'CASUAL';

export interface Room {
  id: string;
  name: string;
  description: string | null;
  theme: RoomTheme;
  intention: Intention | null;
  backgroundImageUrl: string;
  maxUsers: number;
  population: number;
}

export interface RoomSlot {
  slotIndex: number;
  xPercent: number;
  yPercent: number;
  scale: number;
}

export interface RoomDetail {
  room: Room;
  slots: RoomSlot[];
}

export interface RoomUser {
  userId: string;
  username: string;
  slotIndex: number;
  currentIntention: Intention | null;
  avatar: UserAvatar;
}

export interface JoinRoomResponse {
  roomId: string;
  slotIndex: number;
}

export type MessageType = 'TEXT' | 'GIFT' | 'SYSTEM';

export interface Message {
  id: string;
  senderId: string;
  receiverId: string;
  content: string;
  messageType: MessageType;
  createdAt: string;
  readAt: string | null;
}

export interface Conversation {
  partnerId: string;
  partnerUsername: string;
  partnerOnline: boolean;
  partnerAvatar: UserAvatar;
  lastMessage: Message;
  unreadCount: number;
}

export type GiftCategory = 'CLASSIC' | 'ROMANTIC' | 'FUN' | 'LUXURY';

export interface Gift {
  id: string;
  name: string;
  iconUrl: string;
  animationUrl: string | null;
  coinPrice: number;
  category: GiftCategory;
}

export interface Wallet {
  coinBalance: number;
}

export interface SendGiftResponse {
  transactionId: string;
  giftId: string;
  giftName: string;
  coinAmount: number;
  newBalance: number;
  message: Message;
}

export interface AddLanguageRequest {
  languageCode: string;
  type: LanguageType;
  level?: LanguageLevel;
}
