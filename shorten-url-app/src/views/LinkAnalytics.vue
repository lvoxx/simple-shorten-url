<script setup lang="ts">
import { useLinkAnalytics } from "@/composables/useLinkAnalytics";
import { LABELS } from "@/constants";

import loadingAnimation from "@/assets/animations/loading.json";

import AnimationGenerator from "@/components/AnimationGenerator.vue";

const {
  link,
  loading,
  error,
  shortUrl,
  deleteLink,
  copyToClipboard,
  openInNewWindow,
  goBack,
} = useLinkAnalytics();
</script>

<template>
  <v-container class="link-analytics-container h-full">
    <div
      class="w-[200px] flex items-center justify-center mx-auto h-full"
      v-if="loading"
    >
      <AnimationGenerator :jsonData="loadingAnimation" />
    </div>

    <div class="max-w-6xl mx-auto" v-else>
      <h1 class="text-4xl font-bold text-center mb-4">
        {{ LABELS.LINK_ANALYTICS }}
      </h1>

      <div class="link-analytics pa-6">
        <v-btn
          variant="text"
          prepend-icon="mdi-arrow-left"
          @click="goBack"
          class="mb-4"
        >
          {{ LABELS.BACK_TO_DASHBOARD }}
        </v-btn>

        <v-alert v-if="error" type="error" class="mb-4">
          {{ error }}
        </v-alert>

        <div v-else-if="link">
          <v-row>
            <v-col cols="12">
              <v-card class="pa-4">
                <v-card-title class="d-flex justify-space-between align-center">
                  <span>{{ LABELS.LINK_DETAILS }}</span>
                  <v-btn
                    color="error"
                    variant="outlined"
                    size="small"
                    prepend-icon="mdi-delete"
                    @click="deleteLink"
                  >
                    {{ LABELS.DELETE }}
                  </v-btn>
                </v-card-title>
                <v-card-text>
                  <v-list lines="two">
                    <v-list-item>
                      <v-list-item-title class="font-weight-bold">
                        {{ LABELS.ORIGINAL_URL }}
                      </v-list-item-title>
                      <v-list-item-subtitle class="text-wrap">
                        <a
                          :href="link.originalUrl"
                          target="_blank"
                          class="text-primary"
                        >
                          {{ link.originalUrl }}
                        </a>
                      </v-list-item-subtitle>
                    </v-list-item>

                    <v-list-item>
                      <v-list-item-title class="font-weight-bold">
                        {{ LABELS.SHORT_URL }}
                      </v-list-item-title>
                      <v-list-item-subtitle class="d-flex align-center gap-2">
                        <a
                          :href="shortUrl"
                          target="_blank"
                          class="text-primary"
                        >
                          {{ shortUrl }}
                        </a>
                        <div>
                          <v-btn
                            icon="mdi-content-copy"
                            size="x-small"
                            variant="text"
                            @click="copyToClipboard"
                          ></v-btn>
                          <v-btn
                            icon="mdi-open-in-new"
                            size="x-small"
                            variant="text"
                            @click="openInNewWindow(shortUrl)"
                          ></v-btn>
                        </div>
                      </v-list-item-subtitle>
                    </v-list-item>

                    <v-list-item>
                      <v-list-item-title class="font-weight-bold">
                        {{ LABELS.SHORT_CODE_LABEL }}
                      </v-list-item-title>
                      <v-list-item-subtitle>
                        {{ link.shortCode }}
                      </v-list-item-subtitle>
                    </v-list-item>

                    <v-list-item>
                      <v-list-item-title class="font-weight-bold">
                        {{ LABELS.CREATED_AT }}
                      </v-list-item-title>
                      <v-list-item-subtitle>
                        {{ new Date(link.createdAt).toLocaleString() }}
                      </v-list-item-subtitle>
                    </v-list-item>

                    <v-list-item>
                      <v-list-item-title class="font-weight-bold">
                        {{ LABELS.TOTAL_CLICKS }}
                      </v-list-item-title>
                      <v-list-item-subtitle>
                        <span class="text-h5 text-primary">{{
                          link.clickCount
                        }}</span>
                      </v-list-item-subtitle>
                    </v-list-item>
                  </v-list>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>
      </div>
    </div>
  </v-container>
</template>
