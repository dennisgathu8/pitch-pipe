# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Real-time monitoring replay engine (`pitch-pipe.replay`) for continuous timer-paced event simulation from historical StatsBomb match files
- Prometheus metrics registry (`pitch-pipe.metrics`) tracking pipeline health (throughput, spec validation failures, ingestion lag, replay status) and domain metrics (xG histogram, shot zones, event type totals)
- Lightweight HTTP server (`pitch-pipe.server`) via http-kit exposing `/metrics` (Prometheus exposition) and `/health`
- Non-throwing boundary validation (`spec/validate-event`) returning `{:valid? true/false}` to prevent malformed stream records from crashing replay loops
- New CLI flags: `--replay`, `--once`, `--speed`, `--port`
- Docker Compose stack (`docker-compose.yml`, `monitoring/`) provisioning Prometheus (port 9090) and Grafana (port 3000)
- Auto-provisioned Grafana dashboards: **Match Insights** (xG, shots by zone, event volume, events by type) and **Pipeline Health** (replay status, throughput, spec validation failures, ingestion lag percentiles)
- Real-Time Monitoring documentation in README with CLI flag reference, dashboard reference, and starter PromQL queries
- Architecture Decision Record (`ADR-002-realtime-monitoring-prometheus.md`)
- Security scanning via nvd-clojure 5.2.0 (standalone Clojure CLI, separate nvd/ helper project)
- Transducer pipeline for StatsBomb shot event enrichment
- clojure.spec validation at JSON ingestion boundary
- Zone computation across 18 pitch zones
- Basic xG estimate (distance × angle model)
- CLI with --match-id, --mode, --format flags
- Generative tests via test.check

## [0.1.0] - 2026-03-31

### Added
- Initial release
