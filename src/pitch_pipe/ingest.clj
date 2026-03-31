(ns pitch-pipe.ingest
  "Handles loading StatsBomb JSON event data from disk.
   Data path comes from environment variable STATSBOMB_DATA_PATH
   or config.edn — never hardcoded."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [taoensso.timbre :as log]
            [pitch-pipe.spec :as spec]))

(defn data-path
  "Returns the StatsBomb data directory path from environment or
   config.edn. Throws if neither is set."
  []
  (or (env :statsbomb-data-path)
      (throw (ex-info "STATSBOMB_DATA_PATH not set"
                      {:type :pitch-pipe/config-error}))))

(defn load-match-events
  "Loads and parses all events for a given match-id from the
   StatsBomb open data directory. Validates every event at the
   boundary before returning. Returns a vector of validated event
   maps."
  [match-id]
  (let [path (clojure.string/join "/" [(data-path) "events" (clojure.string/join "" [(java.lang.String/valueOf match-id) ".json"])])
        file (io/file path)]
    (when-not (.exists file)
      (throw (ex-info "Match event file not found"
                      {:type     :pitch-pipe/file-not-found
                       :match-id match-id
                       :path     path})))
    (log/info "Loading match events" {:match-id match-id :path path})
    (let [raw (json/parse-stream (io/reader file) keyword)]
      (mapv spec/validate-event! raw))))

(defn list-available-matches
  "Returns a sorted list of available match IDs (longs) from the
   events directory."
  []
  (let [events-dir (io/file (str (data-path) "/events"))]
    (when-not (.exists events-dir)
      (throw (ex-info "Events directory not found"
                      {:type :pitch-pipe/file-not-found
                       :path (.getPath events-dir)})))
    (->> (.listFiles events-dir)
         (filter #(.isFile %))
         (filter #(str/ends-with? (.getName %) ".json"))
         (map #(-> (.getName %) (str/replace #"\.json$" "")
                   Long/parseLong))
         sort
         vec)))
