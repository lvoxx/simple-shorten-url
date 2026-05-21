import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth0 } from "@auth0/auth0-vue";

import { useLinksStore } from "@/stores/links";
import { LABELS } from "@/constants";
import type { UrlResponse } from "@/types";

export function useLinkAnalytics() {
  const route = useRoute();
  const router = useRouter();
  const { getAccessTokenSilently } = useAuth0();
  const store = useLinksStore();

  const link = ref<UrlResponse | null>(null);
  const loading = computed(() => store.loading);
  const error = computed(() => store.error);

  const linkId = computed(() => route.params.linkId as string);

  const shortUrl = computed(() =>
    link.value
      ? `${window.location.origin}${import.meta.env.BASE_URL}${link.value.shortCode}`
      : "",
  );

  const loadLink = async () => {
    error.value = null;

    const cached = store.findLinkById(linkId.value);
    if (cached) {
      link.value = cached;
      return;
    }

    try {
      link.value = await store.fetchLinkById(linkId.value, getAccessTokenSilently);
    } catch {
      error.value = LABELS.FAILED_LOAD_LINK;
    }
  };

  const deleteLink = async () => {
    if (!confirm(LABELS.CONFIRM_DELETE_LINK)) return;

    try {
      await store.deleteLink(linkId.value, getAccessTokenSilently);
      alert(LABELS.LINK_DELETED);
      router.push({ path: "/" });
    } catch {
      alert(LABELS.ERROR_DELETING_LINK);
    }
  };

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(shortUrl.value);
    } catch {
      alert(LABELS.FAILED_COPY_URL);
    }
  };

  const openInNewWindow = (url: string) => {
    window.open(url, "_blank");
  };

  const goBack = () => {
    router.push({ path: "/" });
  };

  onMounted(() => {
    loadLink();
  });

  return {
    link,
    loading,
    error,
    shortUrl,
    deleteLink,
    copyToClipboard,
    openInNewWindow,
    goBack,
  };
}
