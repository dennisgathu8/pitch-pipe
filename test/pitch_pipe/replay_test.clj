(ns pitch-pipe.replay-test
  "Tests for pitch-pipe.replay — single-pass replay engine."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [pitch-pipe.ingest :as ingest]
            [pitch-pipe.replay :as replay]
            [pitch-pipe.metrics :as metrics]))

(deftest replay-once-records-metrics-test
  (testing "replay-once! processes fixture events at high speed and records metrics"
    (let [tmp-base (io/file (System/getProperty "java.io.tmpdir") "pitch-pipe-replay-test")
          events-dir (io/file tmp-base "events")
          fixture-src (io/resource "sample_events.json")
          target-file (io/file events-dir "12345.json")]
      (.mkdirs events-dir)
      (io/copy (io/input-stream fixture-src) target-file)
      (try
        (with-redefs [ingest/data-path (constantly (.getPath tmp-base))]
          (replay/replay-once! {:match-id 12345 :mode :all :speed 1000000.0})
          (let [text (metrics/text-format)]
            (is (str/includes? text "pitch_pipe_events_processed_total"))
            (is (str/includes? text "pitch_pipe_events_by_type_total"))))
        (finally
          (io/delete-file target-file true)
          (io/delete-file events-dir true)
          (io/delete-file tmp-base true))))))
