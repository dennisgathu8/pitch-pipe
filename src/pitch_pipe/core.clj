(ns pitch-pipe.core
  "CLI entry point for pitch-pipe.
   Parses command-line flags and orchestrates the ingestion,
   pipeline, and emit stages."
  (:require [pitch-pipe.ingest   :as ingest]
            [pitch-pipe.pipeline :as pipeline]
            [pitch-pipe.emit     :as emit]
            [taoensso.timbre     :as log])
  (:gen-class))

(defn- parse-args
  "Parses CLI args vector into an options map.
   Supported flags:
     --match-id  <id>       StatsBomb match ID (required)
     --mode      shots|all  Pipeline mode (default: shots)
     --format    edn|json   Output format (default: edn)"
  [args]
  (let [m (apply hash-map args)]
    {:match-id (some-> (get m "--match-id") Long/parseLong)
     :mode     (keyword (get m "--mode" "shots"))
     :format   (keyword (get m "--format" "edn"))}))

(defn run
  "Core entry point — can be called from REPL or CLI.
   opts map: {:match-id 3764760 :mode :shots :format :edn}"
  [{:keys [match-id mode format]}]
  (when-not match-id
    (throw (ex-info "match-id is required" {:type :pitch-pipe/usage-error})))
  (log/info "pitch-pipe starting" {:match-id match-id :mode mode :format format})
  (let [events    (ingest/load-match-events match-id)
        processed (case mode
                    :all    (pipeline/run-full-pipeline events)
                    (pipeline/run-shot-pipeline events))]
    (log/info "Pipeline complete" {:input-count (count events)
                                   :output-count (count processed)})
    (emit/emit! processed format)))

(defn -main
  "Main CLI entry point. Parses args and runs the pipeline."
  [& args]
  (try
    (run (parse-args args))
    (catch clojure.lang.ExceptionInfo e
      (log/error "pitch-pipe error" (ex-data e))
      (System/exit 1))))
