# pitch-pipe 🎵⚽

[![CI](https://github.com/dennisgathu8/pitch-pipe/actions/workflows/ci.yml/badge.svg)](https://github.com/dennisgathu8/pitch-pipe/actions/workflows/ci.yml)

**StatsBomb ingestion + transducer pipeline for football event analytics.**

## What problem does this solve?

Football event data arrives as deeply nested JSON files with inconsistent schemas and no enrichment. `pitch-pipe` ingests, validates, enriches, and emits clean football event data through a single-pass transducer pipeline — no intermediate collections, no silent data corruption.

## Why Clojure over Python?

| Concern | Python (Pandas) | Clojure (pitch-pipe) |
|---|---|---|
| **Memory** | `df.query(...).assign(...)` creates two intermediate DataFrames | Transducers compose into a single pass — zero intermediate collections |
| **Data integrity** | Pandas silently coerces bad types (NaN for missing ints) | `clojure.spec` validates at the ingestion boundary; malformed events throw descriptive errors before entering the pipeline |
| **Composability** | Pipeline steps are method chains bound to DataFrames | Transducers are first-class values — compose, store, pass as arguments, reuse across projects |
| **Downstream use** | Requires serialisation to move data between tools | Library returns plain Clojure maps — consumed directly by downstream projects (temporal-squad, press-logic) |

## Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                         pitch-pipe pipeline                         │
│                                                                     │
│  ┌──────────┐   ┌────────────┐   ┌────────────┐   ┌─────────────┐ │
│  │  JSON    │──▶│   Spec     │──▶│ Transducer │──▶│   Emit      │ │
│  │  Ingest  │   │ Validation │   │  Pipeline  │   │ (EDN/JSON)  │ │
│  └──────────┘   └────────────┘   └────────────┘   └─────────────┘ │
│       ▲                               │                     │      │
│       │                        ┌──────┴──────┐              │      │
│  StatsBomb                     │   Enrich    │          stdout     │
│  open data                     │  zone + xG  │                     │
│  (local dir)                   └─────────────┘                     │
└──────────────────────────────────────────────────────────────────────┘
```

## How to run

### Prerequisites

- Java 21+
- [Leiningen](https://leiningen.org/)
- [StatsBomb open data](https://github.com/statsbomb/open-data) cloned locally

### Setup

```bash
# Clone StatsBomb open data (choose a persistent location — /tmp is cleared on reboot)
git clone https://github.com/statsbomb/open-data.git ~/data/statsbomb-open-data

# Set the data path
export STATSBOMB_DATA_PATH="$HOME/data/statsbomb-open-data/data"

# Clone this project
git clone https://github.com/dennisgathu8/pitch-pipe.git
cd pitch-pipe

# Run tests
lein test

# Run the pipeline (shots only, EDN output)
lein run --match-id 3764760 --mode shots --format edn

# Run the pipeline (all events, JSON output)
lein run --match-id 3764760 --mode all --format json
```

### As a library dependency

```clojure
;; In your project.clj
:dependencies [[pitch-pipe "0.1.0-SNAPSHOT"]]

;; In your namespace
(require '[pitch-pipe.ingest :as ingest]
         '[pitch-pipe.pipeline :as pipeline])

(def events (ingest/load-match-events 3764760))
(def shots  (pipeline/run-shot-pipeline events))
```

## Example output

```clojure
;; A single enriched shot event (EDN)
{:id #uuid "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 :index 42
 :minute 23
 :second 14
 :type {:id 16 :name "Shot"}
 :location [102.3 34.5]
 :zone :zone16
 :xg 0.073}
```

## What I learned building this

Building pitch-pipe taught me that transducers aren't just a performance optimisation — they change how you think about data pipelines. In Pandas, I'd chain `.query()` and `.assign()` and never question the intermediate DataFrames being allocated at each step. With transducers, the pipeline is a single composed function that I can inspect, test, and store as a value before ever applying it to data. The `comp` of `filter` and `map` is not three steps — it's one step that happens to do three things. That distinction matters when you're processing 3,000+ events per match across an entire season. It also forced me to be rigorous about data validation at boundaries: once an event passes `clojure.spec` validation and enters the transducer, I know its shape is correct. No defensive `nil?` checks scattered through enrichment code. The boundary validates; the pipeline transforms. Clean separation.

## Data attribution

This project uses [StatsBomb open data](https://github.com/statsbomb/open-data), made available under the [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International licence](https://creativecommons.org/licenses/by-nc-sa/4.0/).

StatsBomb open data is freely available for non-commercial use. Any commercial use of this data requires a licence agreement with StatsBomb.

## Licence

MIT — see [project.clj](project.clj).
