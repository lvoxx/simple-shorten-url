import type { UrlResponse } from "@/types";

export interface LinkDisplayItem {
  id: number;
  url: string;
  shortCode: string;
  createdAt: string;
  clicks: number;
}

const transformForDisplay = (link: UrlResponse): LinkDisplayItem => ({
  id: link.id,
  url: link.originalUrl,
  shortCode: link.shortCode,
  createdAt: new Date(link.createdAt).toLocaleString(),
  clicks: link.clickCount,
});

const transformAllForDisplay = (links: UrlResponse[]): LinkDisplayItem[] =>
  links.map(transformForDisplay);

const getDisplayKeys = (items: LinkDisplayItem[]): string[] => {
  if (items.length === 0) return [];
  return Object.keys(items[0]);
};

const validateUrl = (url: string): boolean => {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
};

const SHORT_CODE_PATTERN = /^[a-zA-Z0-9-_]+$/;
const SHORT_CODE_MAX_LENGTH = 50;

const validateShortCode = (code: string): boolean =>
  code.length === 0 || (SHORT_CODE_PATTERN.test(code) && code.length <= SHORT_CODE_MAX_LENGTH);

export const linkService = {
  transformForDisplay,
  transformAllForDisplay,
  getDisplayKeys,
  validateUrl,
  validateShortCode,
};
