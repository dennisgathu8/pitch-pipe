# ADR-001: Transducers over Pandas for Event Transformation Pipeline

## Status

Accepted

## Date

2026-03-31

## Context

pitch-pipe needs a transformation pipeline that:

1. Ingests thousands of deeply nested StatsBomb JSON event objects per match
2. Filters to specific event types (e.g. shots)
3. Enriches events with computed fields (pitch zone, xG estimate)
4. Emits the result in a configurable format (EDN or JSON)

This pipeline runs per-match and will be called repeatedly across a full season. Memory efficiency and composability matter.

## Options Evaluated

### Option A: Pandas (Python)

```python
shots = df[df["type_name"] == "Shot"].copy()
shots["zone"] = shots["location"].apply(compute_zone)
shots["xg"]   = shots.apply(estimate_xg, axis=1)
```

- **Pros:** Familiar to football analysts; rich ecosystem for visualisation.
- **Cons:**
  - `df[...]` creates a new DataFrame. `.copy()` creates another. `.apply()` iterates row-by-row (not vectorised). Three intermediate allocations for what is logically one pass.
  - Silent type coercion: missing integers become `NaN` (float). No boundary validation without manual effort.
  - Not a library — a notebook. Downstream projects can't `import` a pipeline as a first-class value.

### Option B: Vanilla Clojure `reduce`

```clojure
(reduce (fn [acc event]
          (if (shot? event)
            (conj acc (enrich event))
            acc))
        [] events)
```

- **Pros:** Single pass, no intermediate collections, explicit.
- **Cons:** The reducing function mixes concerns (filtering + enrichment + accumulation). Hard to compose or reuse individual steps.

### Option C: Clojure Transducers

```clojure
(def shot-xf
  (comp
    (filter shot?)
    (map enrich-event)))

(transduce shot-xf conj [] events)
```

- **Pros:**
  - Single pass — `filter` and `map` compose into one reducing function. Zero intermediate collections.
  - The transducer `shot-xf` is a first-class value. It can be tested, stored, composed with other transducers, and reused across projects.
  - Separation of concerns: each step (`filter`, `map`) is independent and testable.
  - Works with any reducible source (vectors, channels, streams).
- **Cons:** Steeper initial learning curve for developers unfamiliar with the transducer protocol.

## Decision

**Option C: Clojure transducers.**

The pipeline is the core intellectual contribution of this project. Transducers make it composable, memory-efficient, and independently testable — properties that Pandas pipelines and vanilla `reduce` cannot achieve simultaneously.

## Consequences

- **Positive:** The pipeline is a single composed `fn` — no hidden allocations, no DataFrame shape assumptions. Downstream projects (temporal-squad, press-logic) can import and extend the transducer.
- **Positive:** Adding a new enrichment step (e.g. pass sequence tagging) means adding one `(map tag-pass)` to the `comp` — no refactoring of accumulation logic.
- **Negative:** Transducers are less intuitive than method chaining for developers from a Python/Pandas background. This is mitigated by clear docstrings and the ADR itself.
- **Neutral:** Performance difference is negligible for single-match event counts (~3,000 events). The win is architectural, not micro-benchmark.
