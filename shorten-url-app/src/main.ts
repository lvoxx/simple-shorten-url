import "@/assets/css/main.css";

import "@mdi/font/css/materialdesignicons.css";

import { createApp } from "vue";
import { createPinia } from "pinia";
import router from "@/router";
import { createAuth0 } from "@auth0/auth0-vue";
import vuetify from "@/plugins/vuetify";

import App from "./App.vue";

const app = createApp(App);

app.use(
  createAuth0({
    domain: import.meta.env.VITE_AUTH0_DOMAIN,
    clientId: import.meta.env.VITE_AUTH0_CLIENT_ID,
    authorizationParams: {
      redirect_uri: `${window.location.origin}${import.meta.env.BASE_URL}`,
      audience: import.meta.env.VITE_AUTH0_AUDIENCE,
      scope: "openid profile email offline_access",
    },
    cacheLocation: "localstorage",
    useRefreshTokens: true,
  }),
);

app.use(createPinia());
app.use(router);
app.use(vuetify);

app.mount("#app");
