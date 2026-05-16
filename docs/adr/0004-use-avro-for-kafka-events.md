# ADR 0004: Avro Serialization with Schema Registry for Kafka Events

**Status:** Accepted  
**Date:** 2025-01-01  
**Deciders:** Project Lead

## Context

Analytics events flow from `redirect_service` → Kafka → `analytics_worker`. The event schema must evolve over time (new fields, optional fields) without breaking producers or consumers.

## Decision

Use **Apache Avro** serialization with the **Confluent Schema Registry** for all Kafka messages.

## Consequences

**Positive:**

- Schema evolution: forward/backward compatibility via Schema Registry
- Strong typing: Avro generates Java classes from `.avsc` schema files
- Compact binary serialization (smaller than JSON)
- Confluent's free tier supports Schema Registry

**Negative:**

- Adds infrastructure dependency (Schema Registry must be running)
- Serialization/deserialization slightly slower than plain JSON
- More complex configuration (Avro serializer/deserializer classes)

**Schema file location:** `common/src/main/avro/AnalyticsEvent.avsc`

**Rejected alternatives:**

- JSON serialization: no schema enforcement, larger payload, fragile evolution
- Protocol Buffers: no Schema Registry integration without custom tooling
- Plain Avro without Schema Registry: manual schema management, versioning pain
