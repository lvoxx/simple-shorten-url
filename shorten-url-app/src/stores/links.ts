import { defineStore } from "pinia";
import { ref } from "vue";

import { createLinksApi } from "@/api/links";
import { linkService } from "@/services/linkService";
import type { UrlResponse, CreateUrlRequest, UpdateUrlRequest } from "@/types";
import type { LinkDisplayItem } from "@/services/linkService";
import type { TokenProvider } from "@/api/client";

export const useLinksStore = defineStore("links", () => {
  const links = ref<UrlResponse[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const displayLinks = (): LinkDisplayItem[] =>
    linkService.transformAllForDisplay(links.value);

  const displayKeys = (): string[] =>
    linkService.getDisplayKeys(displayLinks());

  const hasLinks = (): boolean => links.value.length > 0;

  const findLinkById = (id: string | number): UrlResponse | undefined =>
    links.value.find((l) => l.id === Number(id));

  const fetchLinks = async (getToken: TokenProvider) => {
    loading.value = true;
    error.value = null;
    try {
      const api = createLinksApi(getToken);
      links.value = await api.fetchAll();
    } catch (err) {
      error.value = err instanceof Error ? err.message : "Failed to fetch links";
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const fetchLinkById = async (id: string | number, getToken: TokenProvider): Promise<UrlResponse> => {
    const cached = findLinkById(id);
    if (cached) return cached;

    loading.value = true;
    error.value = null;
    try {
      const api = createLinksApi(getToken);
      const link = await api.fetchById(id);
      return link;
    } catch (err) {
      error.value = err instanceof Error ? err.message : "Failed to fetch link";
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const createLink = async (data: CreateUrlRequest, getToken: TokenProvider) => {
    loading.value = true;
    error.value = null;
    try {
      const api = createLinksApi(getToken);
      const newLink = await api.create(data);
      links.value.push(newLink);
      return newLink;
    } catch (err) {
      error.value = err instanceof Error ? err.message : "Failed to create link";
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const updateLink = async (id: string | number, data: UpdateUrlRequest, getToken: TokenProvider) => {
    loading.value = true;
    error.value = null;
    try {
      const api = createLinksApi(getToken);
      const updated = await api.update(id, data);
      const index = links.value.findIndex((l) => l.id === Number(id));
      if (index !== -1) links.value[index] = updated;
      return updated;
    } catch (err) {
      error.value = err instanceof Error ? err.message : "Failed to update link";
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const deleteLink = async (id: string | number, getToken: TokenProvider) => {
    loading.value = true;
    error.value = null;
    try {
      const api = createLinksApi(getToken);
      await api.delete(id);
      links.value = links.value.filter((l) => l.id !== Number(id));
    } catch (err) {
      error.value = err instanceof Error ? err.message : "Failed to delete link";
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const deleteAllLinks = async (getToken: TokenProvider) => {
    loading.value = true;
    error.value = null;
    try {
      const api = createLinksApi(getToken);
      await api.deleteAll();
      links.value = [];
    } catch (err) {
      error.value = err instanceof Error ? err.message : "Failed to delete all links";
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const importLinks = async (linksData: object, getToken: TokenProvider) => {
    loading.value = true;
    error.value = null;
    try {
      const api = createLinksApi(getToken);
      await api.importLinks(linksData);
      await fetchLinks(getToken);
    } catch (err) {
      error.value = err instanceof Error ? err.message : "Failed to import links";
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const clearError = () => {
    error.value = null;
  };

  return {
    links,
    loading,
    error,
    displayLinks,
    displayKeys,
    hasLinks,
    findLinkById,
    fetchLinks,
    fetchLinkById,
    createLink,
    updateLink,
    deleteLink,
    deleteAllLinks,
    importLinks,
    clearError,
  };
});
