import { test, expect } from "@playwright/test";

// constants
import { LABELS } from "../src/constants/index.js";

test("visits the app root url", async ({ page }) => {
  await page.goto("/");
  await expect(page.locator("h1")).toHaveText(LABELS.WELCOME_TITLE);
});
