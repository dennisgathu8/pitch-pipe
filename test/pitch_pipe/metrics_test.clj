(ns pitch-pipe.metrics-test
  "Tests for pitch-pipe.metrics — Prometheus registry and recording functions."
  (:require [clojure.test :refer [deftest is testing]]
            [pitch-pipe.metrics :as metrics]
            [clojure.string :as str]))

(deftest metrics-recording-test
  (testing "Calling metric mutation functions updates the registry text format"
    (let [sample-shot {:id "s1"
                       :index 1
                       :minute 10
                       :second 0
                       :type {:id 16 :name "Shot"}
                       :location [100.0 40.0]
                       :zone :zone18
                       :xg 0.25}
          sample-pass {:id "p1"
                       :index 2
                       :minute 10
                       :second 5
                       :type {:id 30 :name "Pass"}}]
      
      (metrics/record-event! :shots sample-shot)
      (metrics/record-event! :all sample-pass)
      (metrics/record-validation-failure!)
      (metrics/record-lag! 0.123)
      (metrics/set-replay-active! true)
      
      (let [text (metrics/text-format)]
        (is (str/includes? text "pitch_pipe_events_processed_total"))
        (is (str/includes? text "pitch_pipe_spec_validation_failures_total"))
        (is (str/includes? text "pitch_pipe_ingestion_lag_seconds"))
        (is (str/includes? text "pitch_pipe_replay_active 1.0"))
        (is (str/includes? text "pitch_pipe_shots_total"))
        (is (str/includes? text "pitch_pipe_shot_xg"))
        (is (str/includes? text "pitch_pipe_events_by_type_total"))))))
