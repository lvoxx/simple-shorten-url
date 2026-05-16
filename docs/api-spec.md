# API Specification — URL Shortener

**Base URL:** `http://localhost:8080/api/v1`  
**Content-Type:** `application/json`  
**Authentication:** Bearer JWT (except public endpoints)

---

## Authentication

### `POST /auth/register`

Register a new user.

**Request:**

```json
{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "Str0ngP@ss!"
}
```

**Validation Rules:**
| Field | Rule |
|---|---|
| `email` | Valid format, max 255 chars, unique |
| `username` | 3–50 chars, alphanumeric + underscore |
| `password` | Min 8 chars, at least one uppercase, one digit |

**Response `201 Created`:**

```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "createdAt": "2025-01-01T00:00:00Z"
}
```

**Errors:** `409` email/username already exists, `400` validation failed

---

### `POST /auth/login`

Authenticate and receive tokens.

**Request:**

```json
{
  "email": "user@example.com",
  "password": "Str0ngP@ss!"
}
```

**Response `200 OK`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "d3a4f5b6...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Response Headers:**

```
Set-Cookie: refreshToken=<token>; HttpOnly; Path=/api/v1/auth; Max-Age=604800; SameSite=Strict
```

**Errors:** `401` invalid credentials

---

### `POST /auth/refresh`

Exchange refresh token for a new access token.

**Request (cookie):**

```
Cookie: refreshToken=<token>
```

**Response `200 OK`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 900
}
```

**Errors:** `401` invalid/expired/revoked refresh token

---

### `POST /auth/logout`

Revoke the current refresh token. Requires `Authorization: Bearer <token>`.

**Request (cookie):**

```
Cookie: refreshToken=<token>
```

**Response `204 No Content`**

---

## URLs

### `POST /api/v1/urls`

Create a new short URL. **Public** (no auth required). Anonymous users receive 7-day TTL.

**Request:**

```json
{
  "originalUrl": "https://www.example.com/very/long/path",
  "title": "Example Link",
  "expireAt": "2026-01-01T00:00:00Z"
}
```

**Validation Rules:**
| Field | Rule |
|---|---|
| `originalUrl` | `@NotBlank`, `@URL` — must be valid HTTP/HTTPS URL |
| `title` | Max 100 chars, optional |
| `expireAt` | Future timestamp, optional |

**Response `201 Created`:**

```json
{
  "shortCode": "4c92",
  "shortUrl": "http://localhost:8081/4c92",
  "originalUrl": "https://www.example.com/very/long/path",
  "title": "Example Link",
  "clickCount": 0,
  "expireAt": null,
  "isActive": true,
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z"
}
```

**Errors:** `400` invalid URL / blacklisted domain / validation failed

---

### `GET /api/v1/urls/{shortCode}`

Get metadata for a specific short URL. Requires authentication (owner only).

**Response `200 OK`:**

```json
{
  "shortCode": "4c92",
  "shortUrl": "http://localhost:8081/4c92",
  "originalUrl": "https://www.example.com/very/long/path",
  "title": "Example Link",
  "clickCount": 42,
  "expireAt": null,
  "isActive": true,
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z"
}
```

**Errors:** `404` not found, `401` unauthorized

---

### `GET /api/v1/urls/my`

List authenticated user's URLs with cursor-based pagination.

**Query Parameters:**
| Param | Default | Description |
|---|---|---|
| `cursor` | — | Last `id` from previous page (exclusive). Omit for first page. |
| `size` | 20 | Items per page (max 100). |

**Response `200 OK`:**

```json
{
  "content": [
    {
      "shortCode": "4c92",
      "originalUrl": "https://example.com/...",
      "title": "Example",
      "clickCount": 1500,
      "isActive": true,
      "createdAt": "2025-01-01T00:00:00Z"
    }
  ],
  "nextCursor": 101,
  "hasNext": false
}
```

---

### `PUT /api/v1/urls/{id}`

Update a short URL's metadata (title only). Requires authentication (owner only).

**Request:**

```json
{
  "title": "Updated Title"
}
```

**Response `200 OK`:**

```json
{
  "shortCode": "4c92",
  "title": "Updated Title",
  "updatedAt": "2025-01-02T00:00:00Z"
}
```

---

### `DELETE /api/v1/urls/{shortCode}`

Soft-delete (deactivate) a short URL. Sets `is_active = false` and evicts cache.

**Response `204 No Content`**

**Errors:** `404` not found, `401` unauthorized

---

## Users

### `GET /api/v1/users/me`

Get current user profile.

**Response `200 OK`:**

```json
{
  "id": 1,
  "username": "johndoe",
  "email": "user@example.com",
  "role": "USER",
  "isActive": true,
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z"
}
```

---

### `PATCH /api/v1/users/me/email`

Update current user's email.

**Request:**

```json
{
  "newEmail": "newemail@example.com"
}
```

**Response `200 OK`:** updated user profile

---

### `DELETE /api/v1/users/me`

Deactivate current user account (soft delete, sets `is_active = false`).

**Response `204 No Content`**

---

## Redirect

### `GET /{shortCode}`

Resolve a short code and redirect to the original URL. Served by the redirect service (port 8081).

**Success:** `302 Found` with `Location: <original_url>` header

**Errors:**
| Status | Reason |
|---|---|
| 404 | Short code not found |
| 410 | URL has expired |

---

## Health

### `GET /actuator/health`

```json
{
  "status": "UP"
}
```

Additional health indicators available at `/actuator/health/redis`, `/actuator/health/db`, `/actuator/health/kafka`.

---

## Error Response Format

All errors return `ProblemDetail` (RFC 9457):

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Short code 'xxxxx' does not exist",
  "instance": "/api/v1/urls/xxxxx"
}
```

Validation errors include additional `errors` property:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/urls",
  "errors": ["originalUrl: must be a valid URL"]
}
```

---

## Rate Limiting

Rate limit headers are included in every response:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1704067260
```

When exceeded:

```json
HTTP 429 Too Many Requests
Retry-After: 30
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Too many requests. Try again in 30 seconds"
}
```

---

## Swagger UI

Interactive API documentation available at:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
