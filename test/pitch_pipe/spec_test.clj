(ns pitch-pipe.spec-test
  "Tests for pitch-pipe.spec — boundary validation of StatsBomb events."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [pitch-pipe.spec :as spec]))

(def valid-event
  {:id    "a1b2c3d4-e5f6-7890-abcd-000000000001"
   :index 1
   :minute 12
   :second 30
   :type  {:id 30 :name "Pass"}
   :location [60.0 40.0]})

(def valid-shot-event
  {:id    "a1b2c3d4-e5f6-7890-abcd-000000000005"
   :index 5
   :minute 12
   :second 45
   :type  {:id 16 :name "Shot"}
   :location [102.0 34.0]})

(deftest validate-event-passes-valid-event
  (testing "A valid event passes validation and is returned unchanged"
    (is (= valid-event (spec/validate-event! valid-event)))))

(deftest validate-event-throws-on-missing-id
  (testing "A map missing :id throws ex-info with :type :pitch-pipe/spec-violation"
    (let [bad-event (dissoc valid-event :id)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"spec validation"
                            (spec/validate-event! bad-event)))
      (try
        (spec/validate-event! bad-event)
        (catch clojure.lang.ExceptionInfo e
          (is (= :pitch-pipe/spec-violation (:type (ex-data e)))))))))

(deftest validate-event-throws-on-bad-minute
  (testing "A map with :minute out of range (200) fails validation"
    (let [bad-event (assoc valid-event :minute 200)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"spec validation"
                            (spec/validate-event! bad-event)))
      (try
        (spec/validate-event! bad-event)
        (catch clojure.lang.ExceptionInfo e
          (is (= :pitch-pipe/spec-violation (:type (ex-data e)))))))))

(deftest valid-shot-event-passes-shot-spec
  (testing "A valid shot event passes ::shot-event spec"
    (is (s/valid? ::spec/shot-event valid-shot-event))))

(deftest generative-test-valid-events
  (testing "50 generated events from manual generators all pass validation"
    (let [gen-event (fn []
                      {:id     (str (java.util.UUID/randomUUID))
                       :index  (gen/generate (gen/fmap inc (s/gen (s/int-in 0 999))))
                       :minute (gen/generate (s/gen (s/int-in 0 90)))
                       :second (gen/generate (s/gen (s/int-in 0 58)))
                       :type   {:id   (gen/generate (s/gen (s/int-in 1 50)))
                                :name (gen/generate (s/gen #{"Pass" "Shot" "Duel" "Pressure"}))}})
          events    (repeatedly 50 gen-event)]
      (doseq [e events]
        (is (= e (spec/validate-event! e)))))))
