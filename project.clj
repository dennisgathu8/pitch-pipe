(defproject pitch-pipe "0.1.0-SNAPSHOT"
  :description "StatsBomb ingestion + transducer pipeline for football event analytics"
  :url "https://github.com/dennisgathu8/pitch-pipe"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [clj-http "3.13.0"]
                 [cheshire "5.13.0"]
                 [org.clojure/spec.alpha "0.5.238"]
                 [com.taoensso/timbre "6.5.0"]
                 [environ "1.2.0"]]
  :plugins [[lein-environ "1.2.0"]]
  :main pitch-pipe.core
  :repl-options {:host "127.0.0.1"}
  :profiles
  {:dev {:dependencies [[org.clojure/test.check "1.1.1"]]
         :source-paths ["dev"]
         :resource-paths ["test/resources"]
         :env {:statsbomb-data-path "/tmp/statsbomb-open-data/data"}}
   :uberjar {:aot :all
             :omit-source true}})
