(ns pitch-pipe.pipeline-test
  "Tests for pitch-pipe.pipeline — transducer composition and pipeline execution."
  (:require [clojure.test :refer [deftest is testing]]
            [pitch-pipe.pipeline :as pipeline]))

(def sample-events
  [{:id "e1" :index 1 :minute 0 :second 0  :type {:id 35 :name "Starting XI"}}
   {:id "e2" :index 2 :minute 1 :second 12 :type {:id 30 :name "Pass"} :location [35.0 22.0]}
   {:id "e3" :index 3 :minute 12 :second 45 :type {:id 16 :name "Shot"} :location [102.0 34.0]}
   {:id "e4" :index 4 :minute 23 :second 14 :type {:id 16 :name "Shot"} :location [110.0 40.0] :under_pressure true}
   {:id "e5" :index 5 :minute 34 :second 0  :type {:id 17 :name "Pressure"} :location [45.0 60.0]}
   {:id "e6" :index 6 :minute 55 :second 22 :type {:id 16 :name "Shot"} :location [95.0 55.0]}
   {:id "e7" :index 7 :minute 88 :second 30 :type {:id 30 :name "Pass"} :location [80.0 40.0]}])

(deftest shot-pipeline-filters-non-shots
  (testing "run-shot-pipeline filters out non-shot events"
    (let [result (pipeline/run-shot-pipeline sample-events)]
      (is (= 3 (count result)))
      (doseq [event result]
        (is (= "Shot" (get-in event [:type :name])))))))

(deftest shot-pipeline-enriches-events
  (testing "run-shot-pipeline adds :zone and :xg to every returned event"
    (let [result (pipeline/run-shot-pipeline sample-events)]
      (doseq [event result]
        (is (contains? event :zone))
        (is (contains? event :xg))))))

(deftest full-pipeline-preserves-count
  (testing "run-full-pipeline returns the same count as input"
    (let [result (pipeline/run-full-pipeline sample-events)]
      (is (= (count sample-events) (count result))))))

(deftest pipelines-return-vectors
  (testing "Both functions return vectors (not lazy seqs)"
    (is (vector? (pipeline/run-shot-pipeline sample-events)))
    (is (vector? (pipeline/run-full-pipeline sample-events)))))

(deftest transducer-is-a-function
  (testing "The transducer is a composed value satisfying fn?"
    (is (fn? (pipeline/shot-enrichment-xf)))
    (is (fn? (pipeline/full-pipeline-xf)))))
