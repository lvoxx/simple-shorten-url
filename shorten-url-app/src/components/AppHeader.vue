<template>
  <v-app-bar v-if="!isNilOrEmpty(user)" elevation="2">
    <template v-slot:prepend>
      <v-app-bar-nav-icon>
        <router-link
          to="/"
          class="text-white no-underline cursor-pointer"
          :disabled="isDisabled"
        >
          <img
            src="/favicons/logo.png"
            :alt="APP.TITLE"
            width="32"
            height="32"
          />
        </router-link>
      </v-app-bar-nav-icon>
      <div class="flex gap-2 ml-2" v-if="!xs">
        <v-btn
          variant="text"
          prepend-icon="mdi-plus"
          color="white"
          to="/add-link"
          :disabled="isDisabled"
        >
          {{ LABELS.ADD_LINK }}
        </v-btn>
        <v-btn
          variant="text"
          prepend-icon="mdi-information"
          color="white"
          to="/about"
          :disabled="isDisabled"
        >
          {{ LABELS.ABOUT }}
        </v-btn>
      </div>
      <v-btn v-if="xs">
        <v-icon>mdi-dots-vertical</v-icon>
        <v-menu class="hidden-md-and-up" activator="parent">
          <v-list>
            <v-list-item>
              <v-btn
                variant="text"
                prepend-icon="mdi-plus"
                color="white"
                to="/add-link"
                :disabled="isDisabled"
                class="w-full"
              >
                {{ LABELS.ADD_LINK }}
              </v-btn>
            </v-list-item>
            <v-list-item>
              <v-btn
                variant="text"
                prepend-icon="mdi-information"
                color="white"
                to="/about"
                :disabled="isDisabled"
                class="w-full"
              >
                {{ LABELS.ABOUT }}
              </v-btn>
            </v-list-item>
          </v-list>
        </v-menu>
      </v-btn>
    </template>

    <template #append>
      <v-menu location="bottom">
        <template #activator="{ props }">
          <v-btn
            v-bind="props"
            variant="text"
            prepend-icon="mdi-account-circle"
            :disabled="isDisabled"
          >
            {{ user?.name || user?.email || LABELS.ACCOUNT }}
          </v-btn>
        </template>

        <v-card min-width="300">
          <v-card-text>
            <div class="flex flex-col gap-4 items-center text-center">
              <v-avatar size="64" :image="imageSrc"></v-avatar>
              <div>
                <p class="font-semibold">{{ user?.name }}</p>
                <p class="text-small">{{ user?.email }}</p>
              </div>
            </div>
          </v-card-text>

          <v-divider></v-divider>

          <v-card-actions>
            <v-btn
              block
              variant="text"
              prepend-icon="mdi-logout"
              @click="handleLogout"
            >
              {{ LABELS.LOGOUT }}
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-menu>
    </template>
  </v-app-bar>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type ComputedRef, type Ref } from "vue";
import { useDisplay } from "vuetify";

// utils
import { isNilOrEmpty, checkImage } from "@/utils";

// constants
import { APP, LABELS, PLACEHOLDER_IMAGE } from "@/constants";

// state
import { useLinksStore } from "@/store/links";

// types
import type { TUser } from "@/types";

// state
const linksStore = useLinksStore();
const isDisabled = computed(() => linksStore.loading);

const props = defineProps<{
  user?: TUser;
}>();

const emit = defineEmits<{
  (e: "logout"): void;
}>();
const { xs } = useDisplay();
const user: ComputedRef<TUser | undefined> = computed(() => props.user);
const imageSrc: Ref<string> = ref(PLACEHOLDER_IMAGE);

onMounted(() => {
  if (!isNilOrEmpty(user.value?.picture)) {
    const picture = user.value?.picture as string;
    checkImage(picture).then((validImage) => {
      console.log(validImage?.src);
      imageSrc.value =
        !isNilOrEmpty(picture) && validImage
          ? (validImage?.src as string)
          : PLACEHOLDER_IMAGE;
    });
  }
});

const handleLogout = () => {
  emit("logout");
};
</script>
