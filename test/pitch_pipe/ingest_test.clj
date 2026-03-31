(ns pitch-pipe.ingest-test
  "Tests for pitch-pipe.ingest — JSON loading with config-driven data path.
   All tests work WITHOUT the StatsBomb dataset — uses fixture files."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [pitch-pipe.ingest :as ingest]
            [environ.core :as environ]))

(deftest load-match-events-throws-on-missing-file
  (testing "load-match-events throws with :type :pitch-pipe/file-not-found for nonexistent match"
    (let [tmp-dir (System/getProperty "java.io.tmpdir")]
      ;; Point data-path to a temp dir that has no events/ subfolder with this ID
      (with-redefs [ingest/data-path (constantly tmp-dir)]
        (try
          (ingest/load-match-events 99999999)
          (is false "Expected exception was not thrown")
          (catch clojure.lang.ExceptionInfo e
            (is (= :pitch-pipe/file-not-found (:type (ex-data e))))))))))

(deftest data-path-throws-when-not-set
  (testing "data-path throws with :type :pitch-pipe/config-error when env var is not set"
    (with-redefs [environ/env {}]
      (try
        (ingest/data-path)
        (is false "Expected exception was not thrown")
        (catch clojure.lang.ExceptionInfo e
          (is (= :pitch-pipe/config-error (:type (ex-data e)))))))))

(deftest load-match-events-from-fixture
  (testing "load-match-events loads and validates the sample fixture"
    (let [;; Create a temp dir with events/ subdirectory structure
          tmp-base (io/file (System/getProperty "java.io.tmpdir") "pitch-pipe-test")
          events-dir (io/file tmp-base "events")
          fixture-src (io/resource "sample_events.json")
          target-file (io/file events-dir "12345.json")]
      ;; Setup: create events dir and copy fixture
      (.mkdirs events-dir)
      (io/copy (io/input-stream fixture-src) target-file)
      (try
        (with-redefs [ingest/data-path (constantly (.getPath tmp-base))]
          (let [events (ingest/load-match-events 12345)]
            (is (vector? events))
            (is (= 10 (count events)))))
        (finally
          ;; Cleanup
          (io/delete-file target-file true)
          (io/delete-file events-dir true)
          (io/delete-file tmp-base true))))))
