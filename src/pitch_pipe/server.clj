(ns pitch-pipe.server
  "Lightweight HTTP server for Prometheus metric scraping.
   Uses http-kit to expose GET /metrics and GET /health.
   start! returns a stop-fn — call it to shut down the server."
  (:require [org.httpkit.server :as http]
            [pitch-pipe.metrics :as metrics]
            [taoensso.timbre :as log]))

(defn- handler
  "Ring-style request handler.
   GET /metrics — Prometheus text format export
   GET /health  — simple 200 OK
   Everything else — 404"
  [{:keys [request-method uri]}]
  (cond
    (and (= :get request-method) (= "/metrics" uri))
    {:status  200
     :headers {"Content-Type" "text/plain; version=0.0.4"}
     :body    (metrics/text-format)}

    (and (= :get request-method) (= "/health" uri))
    {:status  200
     :headers {"Content-Type" "application/json"}
     :body    "{\"status\":\"ok\"}"}

    :else
    {:status 404
     :headers {"Content-Type" "text/plain"}
     :body "Not Found"}))

(defn start!
  "Starts an http-kit server on the given port.
   Returns a zero-arg stop-fn that shuts the server down gracefully."
  [{:keys [port] :or {port 8081}}]
  (log/info "Starting metrics server" {:port port})
  (let [stop-fn (http/run-server handler {:port port})]
    (log/info "Metrics server listening" {:port port
                                          :endpoints ["/metrics" "/health"]})
    stop-fn))
