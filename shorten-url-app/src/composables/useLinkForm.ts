import { ref, computed } from "vue";

import { linkService } from "@/services/linkService";
import { LABELS } from "@/constants";
import type { CreateUrlRequest } from "@/types";

interface LinkFormData {
  url: string;
  shortCode: string;
}

interface FormStatus {
  success: boolean;
  message?: string;
  display: boolean;
}

export function useLinkForm() {
  const formData = ref<LinkFormData>({
    url: "",
    shortCode: "",
  });

  const valid = ref(false);
  const formRef = ref();

  const urlRules = [
    (v: string) => !!v || LABELS.URL_REQUIRED,
    (v: string) => linkService.validateUrl(v) || LABELS.INVALID_URL,
  ];

  const shortCodeRules = [
    (v: string) =>
      !v || linkService.validateShortCode(v) || LABELS.SHORT_CODE_PATTERN_ERROR,
    (v: string) => !v || v.length <= 50 || LABELS.SHORT_CODE_LENGTH_ERROR,
  ];

  const creationStatus = ref<FormStatus>({
    success: false,
    display: false,
  });

  const importStatus = ref<FormStatus>({
    success: false,
    display: false,
  });

  const canSubmit = computed(() => valid.value);

  const reset = () => {
    formData.value = { url: "", shortCode: "" };
    formRef.value?.reset();
    creationStatus.value = { success: false, display: false };
    importStatus.value = { success: false, display: false };
  };

  const setCreationSuccess = (message: string) => {
    creationStatus.value = { success: true, message, display: true };
  };

  const setCreationError = (message: string) => {
    creationStatus.value = { success: false, message, display: true };
  };

  const setImportSuccess = (message: string) => {
    importStatus.value = { success: true, message, display: true };
  };

  const setImportError = (message: string) => {
    importStatus.value = { success: false, message, display: true };
  };

  const buildCreateRequest = (): CreateUrlRequest => ({
    originalUrl: formData.value.url,
  });

  return {
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
  };
}
