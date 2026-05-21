<template>
  <v-container class="add-link-container">
    <div class="max-w-6xl mx-auto">
      <h1 class="text-4xl font-bold text-center mb-4">
        {{ LABELS.ADD_LINK_TITLE }}
      </h1>

      <v-btn
        variant="text"
        prepend-icon="mdi-arrow-left"
        @click="goBack"
        class="mb-4"
        :disabled="loading"
      >
        {{ LABELS.BACK_TO_DASHBOARD }}
      </v-btn>

      <v-sheet elevation="4">
        <v-tabs color="primary" v-model="tabRef">
          <v-tab :value="ADD_LINK_TABS.ADD_LINK">{{ LABELS.ADD_LINK }}</v-tab>
          <v-tab :value="ADD_LINK_TABS.IMPORT_LINKS">{{
            LABELS.IMPORT_LINKS
          }}</v-tab>
        </v-tabs>

        <v-divider></v-divider>

        <v-tabs-window v-model="tabRef">
          <v-tabs-window-item :value="ADD_LINK_TABS.ADD_LINK">
            <v-sheet class="pa-5">
              <v-card class="w-full" :loading="loading" elevation="0">
                <v-card-text>
                  <v-form
                    ref="formRef"
                    v-model="valid"
                    @submit.prevent="handleSubmit"
                  >
                    <v-text-field
                      v-model="formData.url"
                      :label="LABELS.URL_LABEL"
                      :rules="urlRules"
                      :placeholder="LABELS.URL_PLACEHOLDER"
                      prepend-inner-icon="mdi-link"
                      variant="outlined"
                      required
                      class="mb-4"
                    ></v-text-field>

                    <v-text-field
                      v-model="formData.shortCode"
                      :label="LABELS.SHORT_CODE_LABEL"
                      :rules="shortCodeRules"
                      :placeholder="LABELS.SHORT_CODE_PLACEHOLDER"
                      prepend-inner-icon="mdi-link-variant"
                      variant="outlined"
                      :hint="LABELS.SHORT_CODE_HINT"
                      persistent-hint
                    ></v-text-field>
                  </v-form>
                </v-card-text>

                <v-card-actions class="pa-4 flex flex-col gap-4 w-full">
                  <div class="flex flex-row justify-end w-full">
                    <v-btn
                      variant="outlined"
                      @click="handleReset"
                      :disabled="loading"
                    >
                      {{ LABELS.CANCEL }}
                    </v-btn>
                    <v-btn
                      class="ml-4"
                      :loading="loading"
                      :disabled="!canSubmit"
                      @click="handleSubmit"
                      prepend-icon="mdi-plus"
                    >
                      {{ LABELS.ADD_LINK }}
                    </v-btn>
                  </div>
                </v-card-actions>
              </v-card>
              <div
                v-if="creationStatus.display"
                class="w-full pa-4"
              >
                <v-alert
                  :icon="
                    creationStatus.success
                      ? 'mdi-check-circle'
                      : 'mdi-alert-circle-outline'
                  "
                  :text="creationStatus.message"
                  :type="creationStatus.success ? 'success' : 'error'"
                />
              </div>
            </v-sheet>
          </v-tabs-window-item>
          <v-tabs-window-item :value="ADD_LINK_TABS.IMPORT_LINKS">
            <v-sheet class="pa-5">
              <v-card class="w-full" :loading="loading" elevation="0">
                <v-card-text>
                  <p class="text-center mb-4">
                    {{ LABELS.IMPORT_LINKS_INSTRUCTIONS }}
                  </p>
                  <ImportLinks @linksImported="handleImport" />
                </v-card-text>
              </v-card>
              <div
                v-if="importStatus.display"
                class="w-full px-8 pb-4"
              >
                <v-alert
                  :icon="
                    importStatus.success
                      ? 'mdi-check-circle'
                      : 'mdi-alert-circle-outline'
                  "
                  :text="importStatus.message"
                  :type="importStatus.success ? 'success' : 'error'"
                />
              </div>
            </v-sheet>
          </v-tabs-window-item>
        </v-tabs-window>
      </v-sheet>
    </div>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";

import { useLinks } from "@/composables/useLinks";
import { useLinkForm } from "@/composables/useLinkForm";
import { LABELS, ADD_LINK_TABS } from "@/constants";

import ImportLinks from "@/components/ImportLinks.vue";

const router = useRouter();

const {
  loading,
  create: createLink,
  importLinks: importLinksAction,
} = useLinks();

const {
  formData,
  valid,
  formRef,
  urlRules,
  shortCodeRules,
  creationStatus,
  importStatus,
  canSubmit,
  reset,
  setCreationSuccess,
  setCreationError,
  setImportSuccess,
  setImportError,
  buildCreateRequest,
} = useLinkForm();

const tabRef = ref(ADD_LINK_TABS.ADD_LINK as string);

const emit = defineEmits<{
  linkCreated: [];
  linksImported: [];
}>();

const handleSubmit = async () => {
  if (!canSubmit.value) return;

  try {
    await createLink(buildCreateRequest());
    setCreationSuccess(LABELS.LINK_CREATED);
    emit("linkCreated");
    handleReset();
  } catch (err) {
    const message = err instanceof Error ? err.message : LABELS.FAILED_CREATE_LINK;
    setCreationError(message);
    alert(message);
  }
};

const handleImport = async (linksContent: object) => {
  if (!linksContent) return;

  try {
    await importLinksAction(linksContent);
    const totalLinks = Object.keys(linksContent).length;
    setImportSuccess(LABELS.IMPORT_SUCCESS(totalLinks));
    emit("linksImported");
    handleReset();
  } catch (err) {
    const message = err instanceof Error ? err.message : LABELS.IMPORT_FAILED;
    setImportError(message);
    alert(message);
  }
};

const goBack = () => {
  router.push({ path: "/" });
};

const handleReset = () => {
  reset();
};
</script>
