(ns pitch-pipe.replay
  "Replay engine for simulating live match data from historical StatsBomb events.
   Events are sorted by in-match clock (minute*60+second) and paced so that
   :speed match-seconds elapse per wall-clock second. A full ~90min match at
   speed 60.0 replays in ~90s real time.

   replay-once! runs a single pass; run! starts the metrics server and
   loops replay-once! until interrupted."
  (:refer-clojure :exclude [run!])
  (:require [pitch-pipe.ingest   :as ingest]
            [pitch-pipe.spec     :as spec]
            [pitch-pipe.pipeline :as pipeline]
            [pitch-pipe.emit     :as emit]
            [pitch-pipe.metrics  :as metrics]
            [pitch-pipe.server   :as server]
            [taoensso.timbre     :as log]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- event-clock
  "Returns the in-match clock for an event in seconds (minute*60 + second)."
  [event]
  (+ (* (:minute event 0) 60)
     (:second event 0)))

(defn- select-xf
  "Returns the appropriate transducer for the given pipeline mode."
  [mode]
  (case mode
    :all (pipeline/full-pipeline-xf)
    (pipeline/shot-enrichment-xf)))

;; ---------------------------------------------------------------------------
;; Single-pass replay
;; ---------------------------------------------------------------------------

(defn replay-once!
  "Replays a match's events in chronological order, paced by :speed.
   Options map:
     :match-id  — StatsBomb match ID (required)
     :mode      — :shots or :all (default :shots)
     :format    — :edn or :json (default :edn)
     :speed     — match-seconds per wall-clock second (default 60.0)

   For each event: validate (skip on failure), run through the pipeline
   transducer, emit, record metrics, and pace via sleep."
  [{:keys [match-id mode format speed]
    :or   {mode :shots format :edn speed 60.0}}]
  (let [raw-events   (ingest/load-match-events-raw match-id)
        sorted       (sort-by event-clock raw-events)
        xf           (select-xf mode)
        start-nanos  (System/nanoTime)
        first-clock  (event-clock (first sorted))]
    (log/info "Replay starting" {:match-id match-id
                                 :events   (count sorted)
                                 :mode     mode
                                 :speed    speed})
    (doseq [raw-event sorted]
      (let [result (spec/validate-event raw-event)]
        (if-not (:valid? result)
          ;; Skip invalid event, record the failure
          (do
            (log/warn "Skipping invalid event" {:event-id (:event-id result)
                                                 :explain  (:explain result)})
            (metrics/record-validation-failure!))
          ;; Process valid event
          (let [event      (:event result)
                match-secs (- (event-clock event) first-clock)
                ;; How long should have elapsed on the wall clock by now
                target-elapsed-ms (* (/ match-secs speed) 1000.0)
                actual-elapsed-ms (/ (- (System/nanoTime) start-nanos) 1e6)
                sleep-ms          (- target-elapsed-ms actual-elapsed-ms)
                ;; Run through transducer
                processed  (transduce xf conj [] [event])]
            ;; Pace the replay
            (when (> sleep-ms 1.0)
              (Thread/sleep (long sleep-ms)))
            ;; Emit processed events (may be empty if filtered by shot-xf)
            (when (seq processed)
              (emit/emit! processed format))
            ;; Record metrics
            (doseq [p processed]
              (metrics/record-event! mode p))
            ;; Record lag (how far behind schedule we are)
            (let [post-elapsed-ms (/ (- (System/nanoTime) start-nanos) 1e6)
                  lag-seconds     (max 0.0 (/ (- post-elapsed-ms target-elapsed-ms) 1000.0))]
              (metrics/record-lag! lag-seconds))))))
    (log/info "Replay pass complete" {:match-id match-id
                                      :events   (count sorted)})))

;; ---------------------------------------------------------------------------
;; Continuous replay loop
;; ---------------------------------------------------------------------------

(defn run!
  "Starts the metrics server, sets replay-active, and loops replay-once!
   Options map inherits all replay-once! keys plus:
     :port    — HTTP port for /metrics (default 8081)
     :loop?   — if false, run a single pass then stop (default true)

   Registers a JVM shutdown hook to clean up on Ctrl+C."
  [{:keys [port loop?] :or {port 8081 loop? true} :as opts}]
  (let [stop-server (server/start! {:port port})
        running?    (atom true)]
    (metrics/set-replay-active! true)
    ;; Shutdown hook for clean Ctrl+C handling
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread.
      ^Runnable
      (fn []
        (log/info "Shutdown hook triggered — cleaning up")
        (reset! running? false)
        (metrics/set-replay-active! false)
        (stop-server))))
    (try
      (if loop?
        (while @running?
          (replay-once! opts)
          (when @running?
            (log/info "Replay pass finished, restarting in 2s…")
            (Thread/sleep 2000)))
        (replay-once! opts))
      (finally
        (metrics/set-replay-active! false)
        (stop-server)
        (log/info "Replay engine stopped")))))
