# ssurl — frontend

Vue 3 + TypeScript single-page app for the simple-shorten-url backend. Dark,
Linear-style developer-tool UI built with Vite, Pinia, Tailwind v4, and a
layered `services → stores → composables → components` architecture.

## Features

- Anonymous URL shortening with copy + QR code (no account needed)
- Register / login / logout with silent token refresh
- Authenticated dashboard: create, list (cursor pagination), edit, delete links
- Account settings: update email, delete account

## Requirements

- Node `^20.19 || >=22.12`
- The backend `api_service` running on `http://localhost:8080` (see the repo
  root `CLAUDE.md` for starting Postgres, Redis, and the service). The dev
  server proxies `/api` to it, so no CORS config is needed.

## Scripts

```sh
npm install
npm run dev          # Vite dev server at http://localhost:5173 (proxies /api → :8080)
npm run build        # type-check + production bundle
npm run test:unit    # vitest
npm run lint         # oxlint + eslint
npm run type-check   # vue-tsc
```

To point the dev proxy at a non-default backend, set `VITE_API_TARGET`
(e.g. `VITE_API_TARGET=http://localhost:9000 npm run dev`).

> Low-memory machines (≈4 GB): `npm run build` can exhaust Node's heap. Run it
> with `NODE_OPTIONS=--max-old-space-size=3072 npm run build`. Development
> (`npm run dev`) and tests are unaffected.

## Architecture

```
src/
├── types/api.ts        # TS mirrors of backend DTOs
├── lib/                # http client, ProblemDetail handling, formatters
├── services/           # one module per API area (auth, url, user)
├── stores/             # Pinia: auth (in-memory token), toast
├── composables/        # useAsync, useForm (zod), useCopy, useReveal
├── components/
│   ├── ui/             # design-system primitives (Button, Input, Card, …)
│   ├── layout/         # header, footer, logo
│   ├── shorten/        # ShortenForm, ShortenResult
│   └── urls/           # UrlList, UrlCard, UrlEditModal, QrCode
├── views/              # routed pages
└── router/             # routes + auth guard
```

### Auth model

The access token lives only in memory (Pinia). The refresh token is an
HTTP-only cookie the browser manages. On load and on any `401`, the app silently
calls `/auth/refresh` to restore or renew the session, then retries once.

### Design tokens

All theme values (dark surfaces, the single electric-blue accent, one radius
scale, fonts) are defined in `src/assets/main.css` via Tailwind `@theme`.

Brand assets (logo, OG image) are placeholdered — see
`src/assets/placeholders/README.md`.

## Recommended IDE

[VS Code](https://code.visualstudio.com/) + the official
[Vue (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.volar)
extension (disable Vetur).
