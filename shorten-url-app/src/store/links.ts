import { ref } from "vue";
import { defineStore } from "pinia";

// types
import type { TLink } from "@/types";

export const useLinksStore = defineStore("links", () => {
  const links = ref<TLink[]>([]);
  const loading = ref(false);

  const setLinks = (newLinks: typeof links.value) => {
    links.value = newLinks;
  };

  const setLoading = (value: boolean) => {
    loading.value = value;
  };

  return { links, setLinks, loading, setLoading };
});
