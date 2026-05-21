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
                      :disabled="!valid"
                      @click="handleSubmit"
                      prepend-icon="mdi-plus"
                    >
                      {{ LABELS.ADD_LINK }}
                    </v-btn>
                  </div>
                </v-card-actions>
              </v-card>
              <div
                v-if="
                  !isNilOrEmpty(linkCreationStatus) &&
                  linkCreationStatus.display
                "
                class="w-full pa-4"
              >
                <v-alert
                  :icon="
                    linkCreationStatus.success
                      ? 'mdi-check-circle'
                      : 'mdi-alert-circle-outline'
                  "
                  :text="linkCreationStatus.message as string"
                  :type="linkCreationStatus.success ? 'success' : 'error'"
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
                  <ImportLinks @linksImported="handleLinksImported" />
                </v-card-text>
              </v-card>
              <div
                v-if="
                  !isNilOrEmpty(linkImportStatus) && linkImportStatus.display
                "
                class="w-full px-8 pb-4"
              >
                <v-alert
                  :icon="
                    linkImportStatus.success
                      ? 'mdi-check-circle'
                      : 'mdi-alert-circle-outline'
                  "
                  :text="linkImportStatus.message as string"
                  :type="linkImportStatus.success ? 'success' : 'error'"
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
import { useAuth0 } from "@auth0/auth0-vue";
import { useRouter } from "vue-router";

// constants
import { LABELS, ADD_LINK_TABS } from "@/constants";

// api & services
import { createLinksApi } from "@/api/links";
import { linkService } from "@/services/linkService";

// Auth0
const { getAccessTokenSilently } = useAuth0();
const linksApi = () => createLinksApi(getAccessTokenSilently);

// Router
const router = useRouter();

// components
import ImportLinks from "@/components/ImportLinks.vue";

// ref
const tabRef = ref(ADD_LINK_TABS.ADD_LINK as string);

// Types
interface LinkFormData {
  url: string;
  shortCode: string;
}

// Emits
const emit = defineEmits<{
  linkCreated: [];
  linksImported: [];
}>();

// State
const formRef = ref();
const valid = ref(false);
const loading = ref(false);
const formData = ref<LinkFormData>({
  url: "",
  shortCode: "",
});
const linkCreationStatus = ref({
  success: false,
  message: null as string | null,
  display: false,
});
const linkImportStatus = ref({
  success: false,
  message: null as string | null,
  display: false,
});

// Validation rules
const urlRules = [
  (v: string) => !!v || LABELS.URL_REQUIRED,
  (v: string) => linkService.validateUrl(v) || LABELS.INVALID_URL,
];

const shortCodeRules = [
  (v: string) =>
    !v || linkService.validateShortCode(v) || LABELS.SHORT_CODE_PATTERN_ERROR,
  (v: string) => !v || v.length <= 50 || LABELS.SHORT_CODE_LENGTH_ERROR,
];

// Methods
const handleSubmit = async () => {
  if (!valid.value) return;

  loading.value = true;
  linkCreationStatus.value = {
    success: false,
    message: null,
    display: false,
  };
  try {
    await linksApi().create({
      originalUrl: formData.value.url,
    });
    console.log(LABELS.LINK_CREATED);
    emit("linkCreated");
    linkCreationStatus.value = {
      success: true,
      message: LABELS.LINK_CREATED,
      display: true,
    };
    handleReset();
  } catch (err) {
    const message = err instanceof Error ? err.message : LABELS.FAILED_CREATE_LINK;
    console.error(LABELS.FAILED_CREATE_LINK, err);
    alert(message);
    linkCreationStatus.value = {
      success: false,
      message,
      display: true,
    };
  } finally {
    loading.value = false;
  }
};

const handleLinksImported = async (linksContent: object) => {
  if (!linksContent) return;

  loading.value = true;
  linkImportStatus.value = {
    success: false,
    message: null,
    display: false,
  };
  try {
    await linksApi().importLinks(linksContent);
    const totalLinks = Object.keys(linksContent).length;
    console.log(LABELS.IMPORT_SUCCESS(totalLinks));
    emit("linksImported");
    linkImportStatus.value = {
      success: true,
      message: LABELS.IMPORT_SUCCESS(totalLinks),
      display: true,
    };
    handleReset();
  } catch (err) {
    const message = err instanceof Error ? err.message : LABELS.IMPORT_FAILED;
    console.error(LABELS.IMPORT_FAILED, err);
    alert(message);
    linkImportStatus.value = {
      success: false,
      message,
      display: true,
    };
  } finally {
    loading.value = false;
  }
};

const goBack = () => {
  router.push({ path: "/" });
};

const handleReset = () => {
  formData.value = {
    url: "",
    shortCode: "",
  };
  formRef.value?.reset();
};
</script>
