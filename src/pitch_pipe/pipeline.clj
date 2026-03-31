(ns pitch-pipe.pipeline
  "Transducer-based event processing pipeline.
   The transducer is a composed value — filter and map compose into
   a single reducing function with zero intermediate collections.

   This is the structural advantage over Pandas:
   df.query(...).assign(...) creates two DataFrames.
   These transducers create zero intermediate collections."
  (:require [pitch-pipe.enrich :as enrich]))

(defn shot-enrichment-xf
  "Returns a transducer that filters to shot events only, then
   enriches each with zone and xG. Single pass — no intermediate
   collections are created between filter and map steps.

   This is the structural advantage over Pandas:
   df.query(...).assign(...) creates two DataFrames.
   This transducer creates zero intermediate collections."
  []
  (comp
   (filter #(= "Shot" (get-in % [:type :name])))
   (map enrich/enrich-event)))

(defn full-pipeline-xf
  "Returns a transducer that processes all event types:
   - Enriches shots with zone + xG
   - Passes all other events through unchanged
   Useful for producing a complete enriched match event log."
  []
  (map enrich/enrich-event))

(defn run-shot-pipeline
  "Runs the shot enrichment transducer over a seq of validated events.
   Returns a vector of enriched shot events.

   events - seq of validated StatsBomb event maps (from ingest/load-match-events)

   Example:
     (run-shot-pipeline (ingest/load-match-events 3764760))"
  [events]
  (transduce (shot-enrichment-xf) conj [] events))

(defn run-full-pipeline
  "Runs the full enrichment transducer over all events.
   Returns a vector of all events with shots enriched."
  [events]
  (transduce (full-pipeline-xf) conj [] events))
