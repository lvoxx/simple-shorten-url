# ADR 0002: Base62 Short Code from Auto-Increment IDs

**Status:** Accepted  
**Date:** 2025-01-01  
**Deciders:** Project Lead

## Context

Short codes must be compact (4-7 characters), URL-safe, collision-free, and human-typeable. Several generation strategies exist.

## Decision

Use PostgreSQL `BIGSERIAL` auto-increment IDs, convert to bytes, and encode with **Base62** (alphabet: `0-9`, `a-z`, `A-Z`) via `io.seruco.encoding:base62`.

```
ID 1_000_000       → bytes → Base62 → "4c92"   (4 chars)
ID 3_521_614_606_208 → bytes → Base62 → "zzzzzz" (6 chars)
```

## Consequences

**Positive:**

- Collision-free by construction (no retry/hash logic needed)
- Short, URL-safe codes with predictable length growth
- No external dependency (no Snowflake, no UUID generation)

**Negative:**

- Sequential codes are predictable (can enumerate all short URLs)
- ID dependency couples code generation to DB write order
- Cannot generate code before persistence (unlike hash-based approaches)

**Rejected alternatives:**

- UUID: 36 chars, not human-friendly
- Random alphanumeric: collision risk, O(1) lookup overhead
- Hash-based (MD5/SHA): longer codes, collision handling needed
- Snowflake: requires external coordination (ZK, or unique worker IDs)
