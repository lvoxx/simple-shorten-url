# ADR 0003: 302 Redirect (Temporary) for Analytics

**Status:** Accepted  
**Date:** 2025-01-01  
**Deciders:** Project Lead

## Context

Short URL redirects can use either `301 Moved Permanently` (cached by browsers) or `302 Found` (not cached). The choice affects analytics accuracy and performance.

## Decision

Use **302 Found** for all redirect responses from the redirect service. Use **301** only at the Cloudflare CDN edge for popular URLs.

## Consequences

**Positive:**

- Every click reaches the server, enabling accurate analytics (IP, User-Agent, referer)
- 302 is never cached by browsers, so users always get fresh redirects
- Cloudflare edge still caches 301 responses for cache-hit URLs (best of both worlds)

**Negative:**

- Higher origin server load (every redirect hits the service at least once)
- 302 responses cannot be cached by intermediary proxies without explicit configuration

**Rejected alternatives:**

- Always 301: analytics lost for cached responses, inflates click counts artificially
- JavaScript redirect: adds latency, breaks users without JS
- Meta refresh: poor UX, not standard for URL shorteners
