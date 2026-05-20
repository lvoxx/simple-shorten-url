<template>
  <v-container class="about-container">
    <div class="max-w-6xl mx-auto">
      <h1 class="text-4xl font-bold text-center mb-8">
        {{ ABOUT_PAGE.TITLE }}
      </h1>

      <v-card class="mb-6">
        <v-card-text class="pa-6">
          <h2 class="text-2xl font-semibold mb-4">
            {{ ABOUT_PAGE.WHAT_IS.TITLE }}
          </h2>
          <p class="mb-4">
            {{ ABOUT_PAGE.WHAT_IS.DESCRIPTION_1 }}
          </p>
          <p>
            {{ ABOUT_PAGE.WHAT_IS.DESCRIPTION_2 }}
          </p>
        </v-card-text>
      </v-card>

      <v-card class="mb-6">
        <v-card-text class="pa-6">
          <h2 class="text-2xl font-semibold mb-4">
            {{ ABOUT_PAGE.FEATURES.TITLE }}
          </h2>
          <v-list class="bg-transparent">
            <v-list-item>
              <template v-slot:prepend>
                <v-icon color="primary">mdi-shield-lock</v-icon>
              </template>
              <v-list-item-title class="">
                <strong>{{ ABOUT_PAGE.FEATURES.SECURE_AUTH.TITLE }}:</strong>
                {{ ABOUT_PAGE.FEATURES.SECURE_AUTH.DESCRIPTION }}
              </v-list-item-title>
            </v-list-item>
            <v-list-item>
              <template v-slot:prepend>
                <v-icon color="primary">mdi-chart-bar</v-icon>
              </template>
              <v-list-item-title class="">
                <strong>{{ ABOUT_PAGE.FEATURES.ANALYTICS.TITLE }}:</strong>
                {{ ABOUT_PAGE.FEATURES.ANALYTICS.DESCRIPTION }}
              </v-list-item-title>
            </v-list-item>
            <v-list-item>
              <template v-slot:prepend>
                <v-icon color="primary">mdi-palette</v-icon>
              </template>
              <v-list-item-title class="">
                <strong>{{ ABOUT_PAGE.FEATURES.MODERN_UI.TITLE }}:</strong>
                {{ ABOUT_PAGE.FEATURES.MODERN_UI.DESCRIPTION }}
              </v-list-item-title>
            </v-list-item>
            <v-list-item>
              <template v-slot:prepend>
                <v-icon color="primary">mdi-devices</v-icon>
              </template>
              <v-list-item-title class="">
                <strong>{{ ABOUT_PAGE.FEATURES.RESPONSIVE.TITLE }}:</strong>
                {{ ABOUT_PAGE.FEATURES.RESPONSIVE.DESCRIPTION }}
              </v-list-item-title>
            </v-list-item>
            <v-list-item>
              <template v-slot:prepend>
                <v-icon color="primary">mdi-link-variant</v-icon>
              </template>
              <v-list-item-title class="">
                <strong>{{ ABOUT_PAGE.FEATURES.CUSTOM_CODES.TITLE }}:</strong>
                {{ ABOUT_PAGE.FEATURES.CUSTOM_CODES.DESCRIPTION }}
              </v-list-item-title>
            </v-list-item>
            <v-list-item>
              <template v-slot:prepend>
                <v-icon color="primary">mdi-lock</v-icon>
              </template>
              <v-list-item-title class="">
                <strong>{{ ABOUT_PAGE.FEATURES.USER_ISOLATION.TITLE }}:</strong>
                {{ ABOUT_PAGE.FEATURES.USER_ISOLATION.DESCRIPTION }}
              </v-list-item-title>
            </v-list-item>
          </v-list>
        </v-card-text>
      </v-card>

      <v-card class="mb-6">
        <v-card-text class="pa-6">
          <h2 class="text-2xl font-semibold mb-4">
            {{ ABOUT_PAGE.TECHNOLOGIES.TITLE }}
          </h2>
          <div class="flex flex-wrap gap-2">
            <v-chip
              v-for="(tech, index) in ABOUT_PAGE.TECHNOLOGIES.STACK"
              :key="index"
              color="primary"
            >
              {{ tech }}
            </v-chip>
          </div>
        </v-card-text>
      </v-card>

      <v-card class="mb-6">
        <v-card-text class="pa-6">
          <h2 class="text-2xl font-semibold mb-4">
            {{ ABOUT_PAGE.HOW_TO_USE.TITLE }}
          </h2>
          <v-list class="bg-transparent" density="comfortable">
            <v-list-item
              v-for="(step, index) in ABOUT_PAGE.HOW_TO_USE.STEPS"
              :key="index"
            >
              <template v-slot:prepend>
                <div
                  class="bg-primary rounded-full w-8 h-8 flex items-center justify-center mr-3"
                >
                  <span class="font-bold">{{ index + 1 }}</span>
                </div>
              </template>
              <v-list-item-title>
                {{ step }}
              </v-list-item-title>
            </v-list-item>
          </v-list>
        </v-card-text>
      </v-card>

      <v-card class="mb-6">
        <v-card-text class="pa-6">
          <h2 class="text-2xl font-semibold mb-4">
            {{ ABOUT_PAGE.DEVELOPER.TITLE }}
          </h2>
          <div class="mb-4">
            <p class="text-xl font-semibold mb-2">
              {{ ABOUT_PAGE.DEVELOPER.NAME }}
            </p>
            <p class="mb-4">{{ ABOUT_PAGE.DEVELOPER.ROLE }}</p>
          </div>
          <p class="mb-4">
            {{ ABOUT_PAGE.DEVELOPER.BIO_1 }}
          </p>
          <p class="mb-6">
            {{ ABOUT_PAGE.DEVELOPER.BIO_2 }}
          </p>
          <div class="flex flex-wrap gap-3">
            <v-btn
              :href="ABOUT_PAGE.DEVELOPER.PORTFOLIO_URL"
              target="_blank"
              color="primary"
              prepend-icon="mdi-web"
            >
              {{ ABOUT_PAGE.DEVELOPER.PORTFOLIO }}
            </v-btn>
            <v-btn
              :href="ABOUT_PAGE.DEVELOPER.GITHUB_URL"
              target="_blank"
              color="grey-darken-3"
              prepend-icon="mdi-github"
            >
              {{ ABOUT_PAGE.DEVELOPER.GITHUB }}
            </v-btn>
            <v-btn
              :href="ABOUT_PAGE.DEVELOPER.LINKEDIN_URL"
              target="_blank"
              color="blue"
              prepend-icon="mdi-linkedin"
            >
              {{ ABOUT_PAGE.DEVELOPER.LINKEDIN }}
            </v-btn>
          </div>
        </v-card-text>
      </v-card>

      <v-card>
        <v-card-text class="pa-6 text-center">
          <h2 class="text-2xl font-semibold mb-4">
            {{ ABOUT_PAGE.CONTACT.TITLE }}
          </h2>
          <p class="mb-6">
            {{ ABOUT_PAGE.CONTACT.DESCRIPTION }}
          </p>
          <v-btn
            :href="ABOUT_PAGE.CONTACT.URL"
            target="_blank"
            color="primary"
            size="large"
            prepend-icon="mdi-email"
          >
            {{ ABOUT_PAGE.CONTACT.BUTTON }}
          </v-btn>
        </v-card-text>
      </v-card>
    </div>
  </v-container>
</template>

<script setup lang="ts">
import { ABOUT_PAGE } from "@/constants";
</script>
