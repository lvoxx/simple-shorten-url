# MSW Mock API — URL Shortener

A mock API suite using **MSW v2** for the URL Shortener project, compatible with both browser and Node.js (Vitest/Jest).

---

## Installation

```bash
npm install -D msw
```

---

## File Structure

```
mocks/
├── db.ts                          # In-memory store + helpers
├── browser.ts                     # Setup for browser (Vite/CRA)
├── server.ts                      # Setup for Node.js / Vitest / Jest
├── handlers/
│   ├── index.ts                   # Aggregates all handlers
│   ├── auth.handlers.ts           # POST /api/v1/auth/**
│   ├── url.handlers.ts            # CRUD /api/v1/urls/**
│   ├── redirect.handlers.ts       # GET /:shortCode (302 redirect)
│   └── health.handlers.ts         # GET /actuator/health/**
└── __tests__/
    └── example.test.ts            # Example test with Vitest
```

---

## Mocked Endpoints

| Method   | Path                                | Auth | Description            |
| -------- | ----------------------------------- | ---- | ---------------------- |
| `POST`   | `/api/v1/auth/register`             | ❌   | Register account       |
| `POST`   | `/api/v1/auth/login`                | ❌   | Login → returns tokens |
| `POST`   | `/api/v1/auth/refresh`              | ❌   | Refresh access token   |
| `POST`   | `/api/v1/auth/logout`               | ✅   | Logout (revoke token)  |
| `POST`   | `/api/v1/urls`                      | ✅   | Create new short URL   |
| `GET`    | `/api/v1/urls`                      | ✅   | List user URLs         |
| `GET`    | `/api/v1/urls/:shortCode`           | ✅   | Get URL details        |
| `DELETE` | `/api/v1/urls/:shortCode`           | ✅   | Soft delete URL        |
| `GET`    | `/api/v1/urls/:shortCode/analytics` | ✅   | URL analytics          |
| `GET`    | `/:shortCode`                       | ❌   | Redirect (302/404/410) |
| `GET`    | `/actuator/health`                  | ❌   | Health check           |
| `GET`    | `/actuator/health/redis`            | ❌   | Redis health           |
| `GET`    | `/actuator/health/db`               | ❌   | DB health              |

---

## Usage in Browser (Vite / CRA)

### 1. Create Service Worker

```bash
npx msw init public/ --save
```

### 2. Enable in `main.tsx`

```tsx
async function enableMocking() {
  if (import.meta.env.MODE !== "development") return;
  const { worker } = await import("./mocks/browser");
  return worker.start({ onUnhandledRequest: "warn" });
}

enableMocking().then(() => {
  ReactDOM.createRoot(document.getElementById("root")!).render(<App />);
});
```

---

## Usage in Vitest

### `vitest.setup.ts`

```ts
import { server } from "./mocks/server";
import { resetDb } from "./mocks/db";

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => {
  server.resetHandlers();
  resetDb();
});
afterAll(() => server.close());
```

### `vitest.config.ts`

```ts
export default defineConfig({
  test: {
    setupFiles: ["./vitest.setup.ts"],
    environment: "jsdom",
  },
});
```

---

## Seeded Accounts

| Email               | Password     | Role  |
| ------------------- | ------------ | ----- |
| `demo@example.com`  | `Demo@1234`  | USER  |
| `admin@example.com` | `Admin@1234` | ADMIN |

---

## Seeded Short Codes

| Short Code | URL                                                          | Status  |
| ---------- | ------------------------------------------------------------ | ------- |
| `4c92`     | [https://www.example.com/](https://www.example.com/)...      | Active  |
| `xYz1`     | [https://github.com/some/repo](https://github.com/some/repo) | Active  |
| `exp1`     | [https://expired-link.com](https://expired-link.com)         | Expired |

---

## Predefined Blacklisted Domains

- `malware.io`
- `spam-site.com`

---

## Override Handler in Tests

```ts
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";

it("server error", () => {
  server.use(
    http.post("/api/v1/auth/login", () =>
      HttpResponse.json({ message: "Internal Server Error" }, { status: 500 }),
    ),
  );
  // ...
});
```
