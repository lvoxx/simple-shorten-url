# Image & logo placeholders

Drop real assets here, then wire them where noted. These are intentionally left
blank so they can be supplied later without hand-rolled decorative art.

| File to add            | Suggested size | Used for                                           |
| ---------------------- | -------------- | -------------------------------------------------- |
| `logo.svg`             | square         | App wordmark (replace the inline mark in `src/components/layout/AppLogo.vue`) |
| `og-image.png`         | 1200×630       | Social/Open-Graph preview (reference from `index.html`) |
| `hero.png` _(optional)_| 1600×1000      | Optional hero visual on `HomeView.vue` if a product shot is wanted |
| `favicon.svg`          | square         | Replace `public/favicon.ico`                       |

The current UI ships with a generated SVG wordmark and a CSS grid hero backdrop,
so the app renders fully without these. Replace them when brand assets exist.
