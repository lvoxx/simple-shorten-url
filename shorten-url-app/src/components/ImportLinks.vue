<template>
  <v-card class="w-full" elevation="0">
    <v-card-text>
      <v-file-upload
        @update:modelValue="handleFile"
        accept=".json"
      ></v-file-upload>
    </v-card-text>
  </v-card>
</template>
<script setup lang="ts">
import { isNilOrEmpty, validateJson } from "@/utils";
import { pathOr } from "ramda";

// constants
import { EXTENSIONS, LABELS } from "@/constants";
const emits = defineEmits<{
  (e: "linksImported", data: object): void;
}>();

const handleFile = (file: File[] | File) => {
  // Simulate file processing and emit event
  if (!isNilOrEmpty(file)) {
    const type = pathOr("", ["type"], file);
    if (type !== EXTENSIONS.JSON.MIME_TYPE) {
      alert(LABELS.INVALID_FILE_TYPE);
      return;
    }
    const fileContent = new FileReader();
    fileContent.onload = (e) => {
      const content = e.target?.result;
      if (!isNilOrEmpty(content)) {
        let validJson: object | false = validateJson(content as string);
        if (!isNilOrEmpty(validJson) && validJson) {
          validJson = validJson as object;
          emits("linksImported", validJson);
        }
      }
    };
    fileContent.readAsText(file as File);
  }
};
</script>
