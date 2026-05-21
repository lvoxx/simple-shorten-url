export interface UserResponse {
  id: number;
  username: string;
  email: string;
  role: string;
  isActive: boolean;
  createdAt: string;
}

export interface UrlResponse {
  id: number;
  shortCode: string;
  shortUrl: string;
  originalUrl: string;
  title: string;
  isActive: boolean;
  clickCount: number;
  expireAt: string | null;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface PageResponse<T> {
  content: T[];
  total: number;
  page: number;
  size: number;
}

export interface CursorPage<T> {
  content: T[];
  nextCursor: number | null;
  hasNext: boolean;
}
