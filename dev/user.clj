(ns user
  "REPL development helpers. Never shipped in production uberjar.
   Loads config from .env via environ."
  (:require [pitch-pipe.ingest   :as ingest]
            [pitch-pipe.pipeline :as pipeline]
            [pitch-pipe.enrich   :as enrich]
            [pitch-pipe.emit     :as emit]))

(comment
  ;; List available matches
  (ingest/list-available-matches)

  ;; Load and run pipeline on a match
  (def events (ingest/load-match-events 3764760))
  (count events)

  (def shots (pipeline/run-shot-pipeline events))
  (count shots)

  ;; Inspect first enriched shot
  (first shots)

  ;; Check zone distribution
  (frequencies (map :zone shots))

  ;; Emit as JSON to stdout
  (emit/emit! shots :json)
  )
