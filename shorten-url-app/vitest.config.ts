import { fileURLToPath } from "node:url";
import { defineConfig, mergeConfig } from "vitest/config";
import viteConfig from "./vite.config";

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: "jsdom",
      exclude: ["**/node_modules/**", "**/dist/**", "**/e2e/**"],
      root: fileURLToPath(new URL("./", import.meta.url)),
    },
  })
);
