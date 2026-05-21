export interface CreateUrlRequest {
  originalUrl: string;
  title?: string;
  expireAt?: string;
}

export interface UpdateUrlRequest {
  title?: string;
  expireAt?: string;
  isActive?: boolean;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}
