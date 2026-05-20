import { createRouter, createWebHistory } from "vue-router";
import HomeView from "@/views/HomeView.vue";
import LinkAnalytics from "@/views/LinkAnalytics.vue";
import NotFound from "@/views/NotFound.vue";
import AddLinkForm from "@/components/AddLinkForm.vue";
import RedirectLinkAnalytics from "@/views/RedirectLinkAnalytics.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "home",
      component: HomeView,
      beforeEnter: (_to, _from, next) => {
        // Check if we have a redirect from 404.html
        const redirect = sessionStorage.getItem("redirect");
        if (redirect) {
          sessionStorage.removeItem("redirect");
          next(redirect);
        } else {
          next();
        }
      },
    },
    {
      path: "/links/:linkId",
      name: "link-analytics",
      component: LinkAnalytics,
    },
    {
      path: "/:linkId",
      name: "redirect-analytics",
      component: RedirectLinkAnalytics,
    },
    {
      path: "/add-link",
      name: "add-link",
      component: AddLinkForm,
    },
    {
      path: "/about",
      name: "about",
      component: () => import("@/views/AboutView.vue"),
    },
    {
      path: "/:pathMatch(.*)*",
      name: "not-found",
      component: NotFound,
    },
  ],
});

export default router;
