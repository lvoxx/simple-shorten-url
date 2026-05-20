<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth0 } from "@auth0/auth0-vue";

// constants
import { routes } from "@/constants";

const route = useRoute();
const router = useRouter();
const { getAccessTokenSilently } = useAuth0();

const error = ref<string | null>(null);

const linkId = computed(() => route.params.linkId as string);

const getLinkDetails = async () => {
  error.value = null;

  try {
    const token = await getAccessTokenSilently();
    const response = await fetch(
      routes.api.getLinkByShortCode.url(linkId.value),
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      },
    );

    if (response.ok) {
      const linkData = await response.json();
      console.log("Redirecting to:", linkData.url, "Clicks:", linkData.clicks);
      window.location.href = linkData.url;
    } else {
      router.replace({ name: "not-found" });
    }
  } catch (err) {
    console.error("Error fetching link details:", err);
    router.replace({ name: "not-found" });
  }
};

onMounted(() => {
  getLinkDetails();
});
</script>
