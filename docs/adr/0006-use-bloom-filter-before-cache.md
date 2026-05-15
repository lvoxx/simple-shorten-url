# ADR 0006: Bloom Filter for Early Rejection of Invalid Codes

**Status:** Accepted  
**Date:** 2025-01-01  
**Deciders:** Project Lead  

## Context

Malicious or random requests with invalid short codes can reach the service. Without a guard, every invalid code triggers a Redis lookup (and potentially a DB lookup), wasting resources.

## Decision

Place a **Redis-backed Bloom filter** (Redisson `RBloomFilter`) before the cache and database layers in the redirect path.

## Configuration

| Parameter | Value |
|-----------|-------|
| Expected elements | 10,000,000 |
| False positive rate | 1% |
| Memory | ~11.4 MB |
| Library | Redisson (backed by Redis) |

## Consequences

**Positive:**
- Invalid codes rejected in O(1) time with zero Redis/DB load
- ~11 MB memory for 10M entries — negligible
- Survives service restarts (data lives in Redis)
- Shared across all redirect service replicas

**Negative:**
- False positives (~1%) allow some invalid codes through to cache/DB
- No deletion support (cannot remove codes from bloom filter)
- Periodic rebuild needed to purge false positives from deleted URLs
- Redisson `contains()` is blocking — must offload from Netty event loop

**Rejected alternatives:**
- No bloom filter: every invalid code reaches Redis or DB
- In-memory bloom filter: lost on restart, not shared across replicas
- Redis Set of all codes: > 10M entries consumes excessive memory
