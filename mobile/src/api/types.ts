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
