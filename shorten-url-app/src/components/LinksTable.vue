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
            @linkCreated="fetchLinks"
            @linksImported="fetchLinks"
            v-if="!hasLinks"
          />
          <v-card :loading="loading" class="w-full" v-else>
            <v-data-table
              hover
              class="elevation-0"
              :items-per-page="10"
              :headers="tableHeaders"
              :items="links"
            >
              <template v-slot:[`item.actions`]="{ item }">
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
                      @click="confirmDelete(item)"
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
              <div v-if="hasLinks">
                <div class="d-flex pa-2">
                  <v-btn
                    color="error"
                    variant="outlined"
                    prepend-icon="mdi-delete-sweep"
                    @click="confirmDeleteAll"
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
import { computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import { useLinks } from "@/composables/useLinks";
import { LABELS, LINKS_MAPPING } from "@/constants";

import AddLinkForm from "@/components/AddLinkForm.vue";
import AnimationGenerator from "@/components/AnimationGenerator.vue";

import loadingAnimation from "@/assets/animations/loading.json";

const route = useRoute();
const router = useRouter();

const {
  links,
  displayKeys,
  hasLinks,
  loading,
  error,
  fetch: fetchLinks,
  remove: removeLink,
  removeAll: removeAllLinks,
} = useLinks();

const tableHeaders = computed(() => [
  ...displayKeys.value.map((key) => ({
    title: (LINKS_MAPPING as Record<string, string>)[key] || key,
    key,
  })),
  { title: LABELS.ACTIONS, key: "actions", sortable: false },
]);

const viewDetails = (item: { id: number }) => {
  router.push({ path: `/links/${item.id}` });
};

const confirmDelete = (item: { id: number }) => {
  if (!confirm(LABELS.CONFIRM_DELETE_MESSAGE)) return;
  removeLink(item.id);
};

const confirmDeleteAll = () => {
  if (!confirm(LABELS.DELETE_ALL(links.value.length))) return;
  removeAllLinks();
};

watch(
  () => route.path,
  () => {
    fetchLinks();
  },
  { immediate: true },
);
</script>
