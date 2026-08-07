# ADR-002: Real-Time Monitoring Architecture & Historical Replay

## Status

Accepted

## Date

2026-08-07

## Context

pitch-pipe was originally built as a single-shot CLI tool for batch processing football match JSON files. As downstream applications rely on pitch-pipe for continuous event streaming, we need observability into pipeline health (throughput, validation errors, processing lag) and domain analytics (xG distribution, shot locations by zone).

Requirements for real-time monitoring:
1. Standardized metrics exposition for scraping and visualization.
2. Simulated real-time event feed without relying on live/expensive match API subscriptions.
3. Boundary validation resilience so malformed continuous stream records don't crash the replay engine.
4. Frictionless local development environment.

## Options Evaluated

### Metrics Exposition & Datastore

- **Option A: Push-based statsD / Graphite**
  - Cons: Requires active UDP pushing from application code, managing sockets, and external collector daemons.
- **Option B: Prometheus Pull-based HTTP Endpoint (`clj-commons/iapetos` + `http-kit`) [ACCEPTED]**
  - Pros: Clean separation of concerns. `pitch-pipe` exposes a standard `GET /metrics` text endpoint. Prometheus scrapes on a configured interval (`scrape_interval: 5s`). Thread-safe atomic counter/histogram updates via `iapetos`.

### Real-Time Event Source

- **Option A: External Websocket or Live API Stream**
  - Cons: High cost, rate limits, non-deterministic for local testing/CI.
- **Option B: Timer-Paced Replay Engine (`pitch-pipe.replay`) [ACCEPTED]**
  - Pros: Replays historical StatsBomb match data chronologically based on match clock (`minute * 60 + second`). Paced dynamically via a `:speed` multiplier (e.g. 60x match speed). Fully deterministic, offline-capable, and zero external network dependency.

## Decision

1. **Prometheus + Grafana**: Integrate `clj-commons/iapetos` registry and `http-kit` server exposing `/metrics`.
2. **Replay Simulation Engine**: Implement `pitch-pipe.replay` to simulate live feeds from local StatsBomb match files.
3. **Non-Throwing Boundary Validation**: Introduce `spec/validate-event` returning `{:valid? true/false}` to log and count spec failures without terminating continuous replay.
4. **Local Dev Stack**: Provide `docker-compose.yml` with Prometheus and pre-provisioned Grafana dashboards targeting `host.docker.internal:8081`.

## Consequences

- **Positive:** Pipeline health and football metrics can be observed in real time using Grafana dashboards.
- **Positive:** Replay simulation allows testing rate-limiting, pipeline lag, and spec violation counters locally.
- **Negative:** Adds `iapetos` and `http-kit` dependencies to `project.clj`.
- **Neutral:** Scope remains local dev / single-process CLI; no production clustering or container packaging required.
