(ns pitch-pipe.enrich-test
  "Tests for pitch-pipe.enrich — zone computation and xG enrichment."
  (:require [clojure.test :refer [deftest is testing]]
            [pitch-pipe.enrich :as enrich]))

(def shot-event
  {:id    "shot-001"
   :index 1
   :minute 12
   :second 45
   :type  {:id 16 :name "Shot"}
   :location [102.0 34.0]})

(def shot-event-under-pressure
  (assoc shot-event :under_pressure true))

(def non-shot-event
  {:id    "pass-001"
   :index 2
   :minute 5
   :second 30
   :type  {:id 30 :name "Pass"}
   :location [60.0 40.0]})

(deftest compute-zone-centre-pitch
  (testing "[60.0 40.0] maps to :zone10 (centre pitch, middle third)"
    (is (= :zone10 (enrich/compute-zone [60.0 40.0])))))

(deftest compute-zone-central-attacking
  (testing "[110.0 40.0] maps to :zone16 (central attacking)"
    (is (= :zone16 (enrich/compute-zone [110.0 40.0])))))

(deftest compute-zone-defensive-left
  (testing "[5.0 5.0] maps to :zone01 (defensive left)"
    (is (= :zone01 (enrich/compute-zone [5.0 5.0])))))

(deftest estimate-xg-returns-nil-for-non-shot
  (testing "estimate-xg returns nil for a non-shot event"
    (is (nil? (enrich/estimate-xg non-shot-event)))))

(deftest estimate-xg-returns-double-for-shot
  (testing "estimate-xg returns a double in [0.0, 1.0] for a shot event"
    (let [xg (enrich/estimate-xg shot-event)]
      (is (some? xg))
      (is (double? xg))
      (is (>= xg 0.0))
      (is (<= xg 1.0)))))

(deftest estimate-xg-lower-under-pressure
  (testing "estimate-xg returns a lower value under pressure than without"
    (let [xg-normal   (enrich/estimate-xg shot-event)
          xg-pressure (enrich/estimate-xg shot-event-under-pressure)]
      (is (< xg-pressure xg-normal)))))

(deftest enrich-event-leaves-non-shot-unchanged
  (testing "enrich-event leaves non-shot events unchanged (no :zone or :xg)"
    (let [result (enrich/enrich-event non-shot-event)]
      (is (= non-shot-event result))
      (is (not (contains? result :zone)))
      (is (not (contains? result :xg))))))

(deftest enrich-event-adds-zone-and-xg-to-shot
  (testing "enrich-event adds :zone and :xg to a shot event"
    (let [result (enrich/enrich-event shot-event)]
      (is (contains? result :zone))
      (is (contains? result :xg))
      (is (keyword? (:zone result)))
      (is (double? (:xg result))))))
