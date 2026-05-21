import { createApiClient } from "./client";
import type { TokenProvider } from "./client";
import type { UrlResponse, CreateUrlRequest, UpdateUrlRequest } from "@/types";

const createLinksApi = (getToken: TokenProvider) => {
  const client = createApiClient(getToken);

  return {
    fetchAll: () => client.get<UrlResponse[]>("/api/links"),

    fetchById: (id: string | number) =>
      client.get<UrlResponse>(`/api/links/${id}`),

    fetchByShortCode: (shortCode: string) =>
      client.get<UrlResponse>(`/api/link/${shortCode}`),

    create: (data: CreateUrlRequest) =>
      client.post<UrlResponse>("/api/link", data),

    update: (id: string | number, data: UpdateUrlRequest) =>
      client.put<UrlResponse>(`/api/links/${id}`, data),

    delete: (id: string | number) =>
      client.delete<void>(`/api/links/${id}`),

    deleteAll: () => client.delete<void>("/api/links"),

    importLinks: (links: object) =>
      client.post<void>("/api/links/import", { links }),
  };
};

export { createLinksApi };
