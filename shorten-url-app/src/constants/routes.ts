/**
 * API Routes Configuration
 * This file exports all available API endpoints for the Link Shortener application
 */

// Safe access to import.meta.env for both browser and test environments
const getBaseUrl = () => {
  // Check if import.meta.env exists (browser environment)
  if (typeof import.meta !== "undefined" && import.meta.env) {
    return import.meta.env.MODE === "production"
      ? (import.meta.env.VITE_API_BASE_URL as string)
      : (import.meta.env.VITE_API_BASE_URL_LOCAL as string) ||
          "http://localhost:8000";
  }
  // Fallback for test environments
  return "http://localhost:8000";
};

const BASE_URL = getBaseUrl();

const routes = {
  // Public routes
  root: {
    method: "GET",
    path: "/",
    url: `${BASE_URL}/`,
    description: "Welcome endpoint",
    public: true,
  },

  redirect: {
    method: "GET",
    path: "/:shortCode",
    url: (shortCode: string) => `${BASE_URL}/${shortCode}`,
    description: "Redirect to original URL using short code",
    public: true,
  },

  // Protected API routes
  api: {
    // Links collection endpoints
    getAllLinks: {
      method: "GET",
      path: "/api/links",
      url: `${BASE_URL}/api/links`,
      description: "Get all links for authenticated user",
      protected: true,
    },

    getLinkById: {
      method: "GET",
      path: "/api/links/:id",
      url: (id: string) => `${BASE_URL}/api/links/${id}`,
      description: "Get a specific link by ID",
      protected: true,
    },

    createLink: {
      method: "POST",
      path: "/api/link",
      url: `${BASE_URL}/api/link`,
      description: "Create a new short link",
      protected: true,
      body: {
        url: "string (required)",
        shortCode: "string (optional)",
      },
    },

    getLinkByShortCode: {
      method: "GET",
      path: "/api/link/:shortCode",
      url: (shortCode: string) => `${BASE_URL}/api/link/${shortCode}`,
      description: "Get link by short code",
      protected: true,
    },

    updateLink: {
      method: "PUT",
      path: "/api/links/:id",
      url: (id: string) => `${BASE_URL}/api/links/${id}`,
      description: "Update a link",
      protected: true,
      body: {
        url: "string (optional)",
        shortCode: "string (optional)",
      },
    },

    deleteLink: {
      method: "DELETE",
      path: "/api/links/:id",
      url: (id: string) => `${BASE_URL}/api/links/${id}`,
      description: "Delete a link",
      protected: true,
    },

    deleteAllLinks: {
      method: "DELETE",
      path: "/api/links",
      url: `${BASE_URL}/api/links`,
      description: "Delete all links",
      protected: true,
    },

    addLinks: {
      method: "POST",
      path: "/api/links/import",
      url: `${BASE_URL}/api/links/import`,
      description: "Import multiple links",
      protected: true,
      body: {
        links: "array of link objects (required)",
      },
    },
  },
} as const;

export { routes };
