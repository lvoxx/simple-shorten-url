# ADR 0005: Reactive Stack (WebFlux + R2DBC + Reactive Redis)

**Status:** Accepted  
**Date:** 2025-01-01  
**Deciders:** Project Lead  

## Context

The redirect service needs to handle thousands of concurrent connections with sub-10ms latency. The traditional blocking Servlet model wastes threads on I/O wait.

## Decision

Use Spring WebFlux (reactive) throughout, with R2DBC for database access and Spring Data Redis Reactive for caching.

## Consequences

**Positive:**
- Non-blocking I/O maximizes throughput per CPU core
- Small, fixed thread pool (Netty event loop) handles many concurrent connections
- End-to-end reactive pipeline (controller → service → repository)
- Backpressure handling via Reactive Streams

**Negative:**
- Steeper learning curve (Mono/Flux, reactive operators)
- Blocking operations must be explicitly offloaded (`Schedulers.boundedElastic()`)
- Debugging reactive stacks is harder (stack traces span async boundaries)
- Some libraries lack reactive drivers (e.g., Redisson `RBloomFilter.contains()` is blocking)

**Rejected alternatives:**
- Spring MVC with Tomcat: thread-per-request model wastes memory at scale
- Spring MVC with async servlet: partial benefit, still uses blocking JDBC
- Vert.x: less ecosystem support, fewer Spring Boot integrations
