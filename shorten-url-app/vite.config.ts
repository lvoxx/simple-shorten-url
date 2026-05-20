import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import vueJsx from "@vitejs/plugin-vue-jsx";
import vueDevTools from "vite-plugin-vue-devtools";
import tailwindcss from "@tailwindcss/vite";
import { createHtmlPlugin } from "vite-plugin-html";
import vuetify from "vite-plugin-vuetify";

// App metadata configuration
const appMetadata = {
  title: "Url short link generator",
  author: "Rusu Ionut",
  description:
    "A simple URL shortener and QR code generator built with Vue.js, Vite, Auth0, and Pinia.",
  keywords:
    "url, shortener, short link, generator, vue, vite, auth0, pinia, qr code",
};

// https://vite.dev/config/
export default defineConfig({
  base: "/vue-url-link-shortner/",
  plugins: [
    vue(),
    vueJsx(),
    vueDevTools(),
    vuetify({ autoImport: true }),
    tailwindcss(),
    createHtmlPlugin({
      minify: true,
      inject: {
        data: {
          ...appMetadata,
        },
      },
    }),
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});
