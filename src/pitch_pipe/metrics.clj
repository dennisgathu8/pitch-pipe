(ns pitch-pipe.metrics
  "Prometheus metric registry for pitch-pipe.
   Exposes pipeline health and domain-level football analytics metrics
   via iapetos, a Clojure wrapper around the Prometheus Java client.

   All metric mutation goes through the record-* / set-* functions
   so the rest of the codebase never touches the registry directly."
  (:require [iapetos.core :as prometheus]
            [iapetos.export :as export]))

;; ---------------------------------------------------------------------------
;; Registry — created once at namespace load time
;; ---------------------------------------------------------------------------

(defonce registry
  (-> (prometheus/collector-registry)
      (prometheus/register
       ;; Pipeline health metrics
       (prometheus/counter
        :pitch-pipe/events-processed-total
        {:description "Total events processed by the pipeline"
         :labels      [:mode]})
       (prometheus/counter
        :pitch-pipe/spec-validation-failures-total
        {:description "Total events that failed spec validation"})
       (prometheus/histogram
        :pitch-pipe/ingestion-lag-seconds
        {:description "Wall-clock lag vs scheduled replay time"
         :buckets     [0.001 0.005 0.01 0.05 0.1 0.25 0.5 1.0 2.5 5.0]})
       (prometheus/gauge
        :pitch-pipe/replay-active
        {:description "1 when a replay loop is running, 0 otherwise"})

       ;; Domain metrics
       (prometheus/counter
        :pitch-pipe/shots-total
        {:description "Total shots observed"
         :labels      [:zone]})
       (prometheus/histogram
        :pitch-pipe/shot-xg
        {:description "Distribution of xG values on shots"
         :buckets     [0.01 0.03 0.05 0.1 0.15 0.2 0.3 0.5 0.7 0.95]})
       (prometheus/counter
        :pitch-pipe/events-by-type-total
        {:description "Event count by StatsBomb event type"
         :labels      [:event-type]}))))

;; ---------------------------------------------------------------------------
;; Mutation helpers — the public API for recording metrics
;; ---------------------------------------------------------------------------

(defn record-event!
  "Record a processed event. Increments events-processed-total (labelled
   by pipeline mode) and events-by-type-total. If the event is a shot
   with :zone and :xg, also increments shots-total and observes shot-xg."
  [mode event]
  (let [event-type (get-in event [:type :name] "unknown")]
    (prometheus/inc registry :pitch-pipe/events-processed-total {:mode (name mode)})
    (prometheus/inc registry :pitch-pipe/events-by-type-total   {:event-type event-type})
    (when (= "Shot" event-type)
      (when-let [zone (:zone event)]
        (prometheus/inc registry :pitch-pipe/shots-total {:zone (name zone)}))
      (when-let [xg (:xg event)]
        (prometheus/observe registry :pitch-pipe/shot-xg xg)))))

(defn record-validation-failure!
  "Increment the spec-validation-failures-total counter."
  []
  (prometheus/inc registry :pitch-pipe/spec-validation-failures-total))

(defn record-lag!
  "Observe an ingestion lag value (seconds) in the histogram."
  [seconds]
  (prometheus/observe registry :pitch-pipe/ingestion-lag-seconds seconds))

(defn set-replay-active!
  "Set the replay-active gauge to 1.0 (true) or 0.0 (false)."
  [active?]
  (prometheus/set registry :pitch-pipe/replay-active (if active? 1.0 0.0)))

;; ---------------------------------------------------------------------------
;; Export — text format for Prometheus scraping
;; ---------------------------------------------------------------------------

(defn text-format
  "Returns the Prometheus text-format string for the current registry state."
  []
  (export/text-format registry))
