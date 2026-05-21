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

// api & services
import { createLinksApi } from "@/api/links";
import { linkService } from "@/services/linkService";
import type { LinkDisplayItem } from "@/services/linkService";

// constants
import { LINKS_MAPPING, LABELS } from "@/constants";
import { isArrayNotEmpty } from "@/utils";

import { useRoute, useRouter } from "vue-router";

const links = ref<LinkDisplayItem[]>([]);
const linksIds = ref<Array<string>>([]);
const loading = ref(false);
const error = ref<Error | null>(null);

// state
const linksStore = useLinksStore();

// route
const route = useRoute();
const router = useRouter();

const { getAccessTokenSilently } = useAuth0();
const getLinksApi = () => createLinksApi(getAccessTokenSilently);

const viewDetails = (item: LinkDisplayItem) => {
  router.push({ path: `/links/${item.id}` });
};

const deleteLink = (item: LinkDisplayItem) => {
  if (!confirm(LABELS.CONFIRM_DELETE_MESSAGE)) {
    return;
  }
  deleteLinkById(item.id);
};

const getLinks = async () => {
  loading.value = true;
  linksStore.setLoading(true);
  error.value = null;
  try {
    const apiLinks = await getLinksApi().fetchAll();
    linksStore.setLinks(apiLinks);
    links.value = linkService.transformAllForDisplay(apiLinks);
    linksIds.value = linkService.getDisplayKeys(links.value);
    linksStore.setLoading(false);
  } catch (err) {
    error.value = err as Error;
    console.error("Error fetching links:", err);
  } finally {
    loading.value = false;
    linksStore.setLoading(false);
  }
};

const deleteLinkById = async (id: number) => {
  loading.value = true;
  error.value = null;
  try {
    await getLinksApi().delete(id);
    await getLinks();
  } catch (err) {
    console.error("Failed to delete link", err);
    error.value = err as Error;
  } finally {
    loading.value = false;
    error.value = null;
  }
};

const deleteAllLinks = async () => {
  if (!confirm(LABELS.DELETE_ALL(links.value.length))) {
    return;
  }
  loading.value = true;
  error.value = null;
  try {
    await getLinksApi().deleteAll();
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

watch(
  () => route.path,
  async () => {
    console.log("Route changed, fetching links again");
    try {
      await getLinks();
    } catch (err) {
      console.error("Error getting access token:", err);
      error.value = err as Error;
    }
  },
  { immediate: true },
);
</script>
