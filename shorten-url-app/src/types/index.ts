import type { User as Auth0User } from "@auth0/auth0-vue";

export type TUser = Auth0User;

export type {
  UserResponse,
  UrlResponse,
  AuthResponse,
  PageResponse,
  CursorPage,
} from "./response";

export type {
  CreateUrlRequest,
  UpdateUrlRequest,
  LoginRequest,
  RegisterRequest,
} from "./request";
