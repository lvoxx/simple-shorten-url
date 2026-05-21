type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

interface ApiError {
  status: number;
  message: string;
  details?: unknown;
}

class ApiClientError extends Error {
  status: number;
  details?: unknown;

  constructor({ status, message, details }: ApiError) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.details = details;
  }
}

const getBaseUrl = (): string => {
  if (typeof import.meta !== "undefined" && import.meta.env) {
    return import.meta.env.MODE === "production"
      ? (import.meta.env.VITE_API_BASE_URL as string)
      : (import.meta.env.VITE_API_BASE_URL_LOCAL as string) ||
          "http://localhost:8000";
  }
  return "http://localhost:8000";
};

const BASE_URL = getBaseUrl();

type TokenProvider = () => Promise<string>;

const createApiClient = (getToken: TokenProvider) => {
  const request = async <T>(
    method: HttpMethod,
    path: string,
    body?: unknown,
  ): Promise<T> => {
    const token = await getToken();
    const headers: Record<string, string> = {
      Authorization: `Bearer ${token}`,
    };

    if (body !== undefined) {
      headers["Content-Type"] = "application/json";
    }

    const response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });

    if (!response.ok) {
      let message = `Request failed with status ${response.status}`;
      try {
        const errorBody = await response.json();
        message = errorBody?.message || message;
        throw new ApiClientError({
          status: response.status,
          message,
          details: errorBody,
        });
      } catch (err) {
        if (err instanceof ApiClientError) throw err;
        throw new ApiClientError({
          status: response.status,
          message,
        });
      }
    }

    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  };

  return {
    get: <T>(path: string) => request<T>("GET", path),
    post: <T>(path: string, body?: unknown) => request<T>("POST", path, body),
    put: <T>(path: string, body?: unknown) => request<T>("PUT", path, body),
    delete: <T>(path: string) => request<T>("DELETE", path),
  };
};

export { createApiClient, ApiClientError };
export type { TokenProvider };
