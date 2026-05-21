import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth0 } from "@auth0/auth0-vue";

import { useLinksStore } from "@/stores/links";

export function useRedirectLink() {
  const route = useRoute();
  const router = useRouter();
  const { getAccessTokenSilently } = useAuth0();
  const store = useLinksStore();

  const linkId = computed(() => route.params.linkId as string);
  const error = ref<string | null>(null);

  const redirect = async () => {
    error.value = null;

    try {
      const linkData = await store.fetchLinkById(linkId.value, getAccessTokenSilently);
      window.location.href = linkData.originalUrl;
    } catch {
      router.replace({ name: "not-found" });
    }
  };

  onMounted(() => {
    redirect();
  });

  return {
    error,
  };
}
