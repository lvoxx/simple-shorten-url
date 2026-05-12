/**
 * mocks/handlers/health.handlers.ts
 * Handlers cho Spring Boot Actuator health endpoints:
 *   GET /actuator/health
 *   GET /actuator/health/redis
 *   GET /actuator/health/db
 */

import { http, HttpResponse } from "msw";

export const healthHandlers = [
  http.get("/actuator/health", () =>
    HttpResponse.json({ status: "UP" }),
  ),

  http.get("/actuator/health/redis", () =>
    HttpResponse.json({
      status: "UP",
      components: {
        redis: { status: "UP", details: { version: "7.2.0" } },
      },
    }),
  ),

  http.get("/actuator/health/db", () =>
    HttpResponse.json({
      status: "UP",
      components: {
        db: {
          status: "UP",
          details: { database: "PostgreSQL", validationQuery: "isValid()" },
        },
      },
    }),
  ),
];
