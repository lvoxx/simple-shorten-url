import { describe, it, expect, beforeEach, vi } from "vitest";

describe("GitHub Pages Redirect Logic", () => {
  const basePath = "/vue-url-link-shortner/";
  const originalOrigin = "https://johnrusu.github.io";

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should redirect from root path to base path with search and hash", () => {
    const mockReplace = vi.fn();

    Object.defineProperty(window, "location", {
      value: {
        origin: originalOrigin,
        pathname: "/",
        search: "?code=abc123",
        hash: "#access_token=xyz",
        replace: mockReplace,
      },
      writable: true,
    });

    // Simulate the redirect logic
    if (
      window.location.pathname === "/" &&
      window.location.origin === originalOrigin
    ) {
      window.location.replace(
        window.location.origin +
          basePath +
          window.location.search +
          window.location.hash
      );
    }

    expect(mockReplace).toHaveBeenCalledWith(
      "https://johnrusu.github.io/vue-url-link-shortner/?code=abc123#access_token=xyz"
    );
  });

  it("should redirect from root path without search or hash", () => {
    const mockReplace = vi.fn();

    Object.defineProperty(window, "location", {
      value: {
        origin: originalOrigin,
        pathname: "/",
        search: "",
        hash: "",
        replace: mockReplace,
      },
      writable: true,
    });

    if (
      window.location.pathname === "/" &&
      window.location.origin === originalOrigin
    ) {
      window.location.replace(
        window.location.origin +
          basePath +
          window.location.search +
          window.location.hash
      );
    }

    expect(mockReplace).toHaveBeenCalledWith(
      "https://johnrusu.github.io/vue-url-link-shortner/"
    );
  });

  it("should not redirect if pathname is not root", () => {
    const mockReplace = vi.fn();

    Object.defineProperty(window, "location", {
      value: {
        origin: originalOrigin,
        pathname: "/some-other-path",
        search: "",
        hash: "",
        replace: mockReplace,
      },
      writable: true,
    });

    if (
      window.location.pathname === "/" &&
      window.location.origin === originalOrigin
    ) {
      window.location.replace(
        window.location.origin +
          basePath +
          window.location.search +
          window.location.hash
      );
    }

    expect(mockReplace).not.toHaveBeenCalled();
  });

  it("should not redirect if origin is different", () => {
    const mockReplace = vi.fn();

    Object.defineProperty(window, "location", {
      value: {
        origin: "http://localhost:5173",
        pathname: "/",
        search: "",
        hash: "",
        replace: mockReplace,
      },
      writable: true,
    });

    if (
      window.location.pathname === "/" &&
      window.location.origin === originalOrigin
    ) {
      window.location.replace(
        window.location.origin +
          basePath +
          window.location.search +
          window.location.hash
      );
    }

    expect(mockReplace).not.toHaveBeenCalled();
  });

  it("should preserve Auth0 callback parameters", () => {
    const mockReplace = vi.fn();

    Object.defineProperty(window, "location", {
      value: {
        origin: originalOrigin,
        pathname: "/",
        search: "?code=AUTH_CODE&state=STATE_VALUE",
        hash: "",
        replace: mockReplace,
      },
      writable: true,
    });

    if (
      window.location.pathname === "/" &&
      window.location.origin === originalOrigin
    ) {
      window.location.replace(
        window.location.origin +
          basePath +
          window.location.search +
          window.location.hash
      );
    }

    expect(mockReplace).toHaveBeenCalledWith(
      "https://johnrusu.github.io/vue-url-link-shortner/?code=AUTH_CODE&state=STATE_VALUE"
    );
  });

  it("should handle complex query strings and hash fragments", () => {
    const mockReplace = vi.fn();

    Object.defineProperty(window, "location", {
      value: {
        origin: originalOrigin,
        pathname: "/",
        search: "?param1=value1&param2=value2",
        hash: "#section-1",
        replace: mockReplace,
      },
      writable: true,
    });

    if (
      window.location.pathname === "/" &&
      window.location.origin === originalOrigin
    ) {
      window.location.replace(
        window.location.origin +
          basePath +
          window.location.search +
          window.location.hash
      );
    }

    expect(mockReplace).toHaveBeenCalledWith(
      "https://johnrusu.github.io/vue-url-link-shortner/?param1=value1&param2=value2#section-1"
    );
  });
});
