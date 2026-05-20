import type { User } from "@auth0/auth0-vue";

type TUser = User;

type TLink = {
  id?: string;
  _id?: string;
  url: string;
  shortCode: string;
  createdAt: string;
  clicks: number;
};

export type { TLink, TUser };
