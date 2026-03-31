(ns pitch-pipe.emit
  "Output formatting for enriched event data.
   Supports EDN and pretty-printed JSON serialisation."
  (:require [cheshire.core :as json]))

(defn ->edn
  "Serialises a collection of event maps to EDN string."
  [events]
  (pr-str events))

(defn ->json
  "Serialises a collection of event maps to pretty-printed JSON."
  [events]
  (json/generate-string events {:pretty true}))

(defn emit!
  "Writes enriched events to *out* in the requested format.
   format-kw - :edn or :json (default :edn)"
  [events format-kw]
  (println
   (case format-kw
     :json (->json events)
     (->edn events))))
