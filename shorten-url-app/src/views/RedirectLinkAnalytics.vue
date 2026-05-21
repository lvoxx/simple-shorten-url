<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth0 } from "@auth0/auth0-vue";

// api
import { createLinksApi } from "@/api/links";

const route = useRoute();
const router = useRouter();
const { getAccessTokenSilently } = useAuth0();
const linksApi = () => createLinksApi(getAccessTokenSilently);

const error = ref<string | null>(null);

const linkId = computed(() => route.params.linkId as string);

const getLinkDetails = async () => {
  error.value = null;

  try {
    const linkData = await linksApi().fetchByShortCode(linkId.value);
    console.log("Redirecting to:", linkData.originalUrl, "Clicks:", linkData.clickCount);
    window.location.href = linkData.originalUrl;
  } catch (err) {
    console.error("Error fetching link details:", err);
    router.replace({ name: "not-found" });
  }
};

onMounted(() => {
  getLinkDetails();
});
</script>
