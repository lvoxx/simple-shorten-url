import { computed } from "vue";
import { useAuth0 } from "@auth0/auth0-vue";

import { useLinksStore } from "@/stores/links";
import type { CreateUrlRequest, UpdateUrlRequest } from "@/types";

export function useLinks() {
  const store = useLinksStore();
  const { getAccessTokenSilently } = useAuth0();

  const getToken = () => getAccessTokenSilently();

  const links = computed(() => store.displayLinks());
  const displayKeys = computed(() => store.displayKeys());
  const hasLinks = computed(() => store.hasLinks());
  const loading = computed(() => store.loading);
  const error = computed(() => store.error);

  const fetch = async () => store.fetchLinks(getToken);

  const create = async (data: CreateUrlRequest) => store.createLink(data, getToken);

  const update = async (id: string | number, data: UpdateUrlRequest) =>
    store.updateLink(id, data, getToken);

  const remove = async (id: string | number) => store.deleteLink(id, getToken);

  const removeAll = async () => store.deleteAllLinks(getToken);

  const importLinks = async (linksData: object) => store.importLinks(linksData, getToken);

  const clearError = () => store.clearError();

  return {
    links,
    displayKeys,
    hasLinks,
    loading,
    error,
    fetch,
    create,
    update,
    remove,
    removeAll,
    importLinks,
    clearError,
  };
}
