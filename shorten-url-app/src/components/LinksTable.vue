<template>
  <v-container class="links-table-container">
    <div
      class="w-[200px] flex items-center justify-center mx-auto h-full"
      v-if="loading"
    >
      <AnimationGenerator :jsonData="loadingAnimation" />
    </div>

    <div class="max-w-6xl mx-auto h-full" v-else>
      <div class="w-full h-full flex">
        <div
          v-if="error"
          class="h-full w-full flex items-center justify-center"
        >
          <div class="text-center max-w-md p-6">
            <v-icon size="120" color="error" class="mb-4">
              mdi-battery-outline
            </v-icon>
            <div class="text-sm">{{ error }}</div>
          </div>
        </div>
        <div v-else class="w-full">
          <h1 class="text-4xl font-bold text-center mb-8">
            {{ LABELS.LINKS_TABLE_TITLE }}
          </h1>
          <AddLinkForm
            @linkCreated="getLinks"
            @linksImported="getLinks"
            v-if="!isArrayNotEmpty(links)"
          />
          <v-card :loading="loading" class="w-full" v-else>
            <v-data-table
              hover
              class="elevation-0"
              :items-per-page="10"
              :headers="[
                ...linksIds.map((id) => ({
                  title: (LINKS_MAPPING as any)[id] || id,
                  key: id,
                })),
                ...(isArrayNotEmpty(links)
                  ? [{ title: LABELS.ACTIONS, key: 'actions', sortable: false }]
                  : []),
              ]"
              :items="links"
            >
              <template
                v-slot:[`item.actions`]="{ item }"
                v-if="isArrayNotEmpty(links)"
              >
                <div class="flex gap-2">
                  <v-btn-group variant="outlined" divided density="compact">
                    <v-btn
                      icon
                      color="primary"
                      @click="viewDetails(item)"
                      density="default"
                      size="small"
                    >
                      <v-icon>mdi-eye</v-icon>
                    </v-btn>
                    <v-btn
                      icon
                      color="error"
                      @click="deleteLink(item)"
                      density="default"
                      size="small"
                    >
                      <v-icon>mdi-delete</v-icon>
                    </v-btn>
                  </v-btn-group>
                </div>
              </template>
            </v-data-table>
            <v-card-actions>
              <div v-if="isArrayNotEmpty(links)">
                <div class="d-flex pa-2">
                  <v-btn
                    color="error"
                    variant="outlined"
                    prepend-icon="mdi-delete-sweep"
                    @click="deleteAllLinks"
                  >
                    {{ LABELS.DELETE_ALL_LINKS }}
                  </v-btn>
                </div>
              </div>
            </v-card-actions>
          </v-card>
        </div>
      </div>
    </div>
  </v-container>
</template>
<script setup lang="ts">
import { ref, watch } from "vue";
import { useAuth0 } from "@auth0/auth0-vue";

// state
import { useLinksStore } from "@/store/links";

// components
import AddLinkForm from "@/components/AddLinkForm.vue";
import AnimationGenerator from "@/components/AnimationGenerator.vue";

// json animations
import loadingAnimation from "@/assets/animations/loading.json";

// types
import type { TLink } from "@/types";

// constants
import { routes, LINKS_MAPPING, LABELS } from "@/constants";
import { isArrayNotEmpty } from "@/utils";

import { useRoute, useRouter } from "vue-router";

const links = ref<TLink[]>([]);
const linksIds = ref<Array<string>>([]);
const loading = ref(false);
const token = ref("");
const error = ref<Error | null>(null);

// state
const linksStore = useLinksStore();

// route
const route = useRoute();
const router = useRouter();

const viewDetails = (item: TLink) => {
  // redirect to details page or open modal
  router.push({ path: `/links/${item.id}` });
};

const deleteLink = (item: TLink) => {
  if (!confirm(LABELS.CONFIRM_DELETE_MESSAGE)) {
    return;
  }
  deleteLinkById(item.id as string);
};

const deleteAllLinks = async () => {
  if (!confirm(LABELS.DELETE_ALL(links.value.length))) {
    return;
  }
  loading.value = true;
  error.value = null;
  try {
    const response = await fetch(`${routes.api.deleteAllLinks.url}`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token.value}`,
      },
    });
    if (!response.ok) {
      error.value = new Error("Failed to delete all links");
      throw new Error("Failed to delete all links");
    }
    console.log("All links deleted successfully");
    await getLinks();
  } catch (err) {
    console.error("Failed to delete all links", err);
    error.value = err as Error;
  } finally {
    loading.value = false;
    error.value = null;
  }
};

const getLinks = async () => {
  // Fetch links from API using the token
  loading.value = true;
  linksStore.setLoading(true);
  error.value = null;
  try {
    const response = await fetch(routes.api.getAllLinks.url, {
      headers: {
        Authorization: `Bearer ${token.value}`,
      },
    });

    if (response.ok) {
      loading.value = false;
      linksStore.setLoading(false);
      error.value = null;
      const apiLinks: TLink[] = await response.json();
      links.value = apiLinks.map((link: TLink) => ({
        id: link._id,
        url: link.url,
        shortCode: link.shortCode,
        createdAt: new Date(link.createdAt).toLocaleString(),
        clicks: link.clicks,
      }));
      if (isArrayNotEmpty(links.value) && links.value[0]) {
        linksIds.value = Object.keys(links.value[0]);
      }
      linksStore.setLinks(links.value);
      linksStore.setLoading(false);
    } else {
      throw new Error("Failed to fetch links");
    }
  } catch (err: unknown) {
    error.value = err as Error;
    console.error("Error fetching links:", err);
  } finally {
    loading.value = false;
    linksStore.setLoading(false);
  }
};

const deleteLinkById = async (id: string) => {
  loading.value = true;
  error.value = null;
  try {
    const response = await fetch(`${routes.api.deleteLink.url(id)}`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token.value}`,
      },
    });

    if (response.ok) {
      await getLinks();
    } else {
      error.value = new Error("Failed to delete link");
      throw new Error("Failed to delete link");
    }
  } catch (err) {
    console.error("Failed to delete link", err);
    error.value = err as Error;
  } finally {
    loading.value = false;
    error.value = null;
  }
};

watch(
  () => route.path,
  async () => {
    console.log("Route changed, fetching links again");
    const { getAccessTokenSilently } = useAuth0();
    try {
      token.value = await getAccessTokenSilently();
      await getLinks();
    } catch (err) {
      console.error("Error getting access token:", err);
      error.value = err as Error;
    }
  },
  { immediate: true },
);
</script>
