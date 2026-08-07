(ns pitch-pipe.core
  "CLI entry point for pitch-pipe.
   Parses command-line flags and orchestrates the ingestion,
   pipeline, emit, and real-time monitoring replay stages."
  (:require [pitch-pipe.ingest   :as ingest]
            [pitch-pipe.pipeline :as pipeline]
            [pitch-pipe.emit     :as emit]
            [pitch-pipe.replay   :as replay]
            [taoensso.timbre     :as log])
  (:gen-class))

(defn- parse-args
  "Parses CLI args vector into an options map.
   Supported flags:
     --match-id <id>       StatsBomb match ID (required)
     --mode     shots|all  Pipeline mode (default: shots)
     --format   edn|json   Output format (default: edn)
     --replay              Run real-time monitoring simulation loop
     --once                With --replay, run a single pass instead of looping
     --speed    <n>        Replay speed multiplier (default: 60.0)
     --port     <n>        HTTP metrics server port (default: 8081)"
  [args]
  (loop [args args
         opts {:mode :shots :format :edn :speed 60.0 :port 8081 :replay false :once false}]
    (if (seq args)
      (let [arg (first args)]
        (case arg
          "--match-id" (recur (drop 2 args) (assoc opts :match-id (Long/parseLong (second args))))
          "--mode"     (recur (drop 2 args) (assoc opts :mode (keyword (second args))))
          "--format"   (recur (drop 2 args) (assoc opts :format (keyword (second args))))
          "--speed"    (recur (drop 2 args) (assoc opts :speed (Double/parseDouble (second args))))
          "--port"     (recur (drop 2 args) (assoc opts :port (Long/parseLong (second args))))
          "--replay"   (recur (rest args)   (assoc opts :replay true))
          "--once"     (recur (rest args)   (assoc opts :once true))
          (recur (rest args) opts)))
      opts)))

(defn run
  "Core entry point — can be called from REPL or CLI.
   opts map: {:match-id 3764760 :mode :shots :format :edn :replay false ...}"
  [{:keys [match-id mode format replay once speed port] :as opts}]
  (when-not match-id
    (throw (ex-info "match-id is required" {:type :pitch-pipe/usage-error})))
  (if replay
    (replay/run! {:match-id match-id
                  :mode     mode
                  :format   format
                  :speed    (or speed 60.0)
                  :port     (or port 8081)
                  :loop?    (not once)})
    (do
      (log/info "pitch-pipe starting" {:match-id match-id :mode mode :format format})
      (let [events    (ingest/load-match-events match-id)
            processed (case mode
                        :all    (pipeline/run-full-pipeline events)
                        (pipeline/run-shot-pipeline events))]
        (log/info "Pipeline complete" {:input-count (count events)
                                       :output-count (count processed)})
        (emit/emit! processed format)))))

(defn -main
  "Main CLI entry point. Parses args and runs the pipeline."
  [& args]
  (try
    (run (parse-args args))
    (catch clojure.lang.ExceptionInfo e
      (log/error "pitch-pipe error" (ex-data e))
      (System/exit 1))))
