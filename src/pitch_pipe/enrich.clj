(ns pitch-pipe.enrich
  "Pure enrichment functions for StatsBomb event data.
   No I/O, no side effects. Fully testable.

   The pitch zone system divides the StatsBomb 120×80 pitch into
   18 named zones: 3 horizontal thirds × 6 vertical channels.")

(def ^:private goal-centre
  "Centre of the goal on the StatsBomb coordinate system."
  [120.0 40.0])

(defn- euclidean-dist
  "Computes Euclidean distance between two 2D points."
  [[x1 y1] [x2 y2]]
  (Math/sqrt (+ (Math/pow (- x2 x1) 2)
                (Math/pow (- y2 y1) 2))))

(defn- shot-angle
  "Computes the angle subtended by the goal posts from a given pitch location.
   Uses the law of cosines. Goal posts at [120, 36] and [120, 44]."
  [[x y]]
  (let [post-top    [120.0 36.0]
        post-bottom [120.0 44.0]
        a (euclidean-dist [x y] post-top)
        b (euclidean-dist [x y] post-bottom)
        c (euclidean-dist post-top post-bottom)
        cos-c (/ (- (+ (* a a) (* b b)) (* c c))
                 (* 2 a b))]
    (Math/toDegrees (Math/acos (max -1.0 (min 1.0 cos-c))))))

(defn compute-zone
  "Maps a StatsBomb [x y] location to a named pitch zone keyword.
   Zones are labelled :zone01 through :zone18.
   Horizontal thirds: defensive (x < 40), middle (40 ≤ x < 80), attacking (x ≥ 80).
   Vertical channels: 6 bands of ~13.33 units across the 80-unit width."
  [[x y]]
  (let [h-third (cond (< x 40.0) 0 (< x 80.0) 1 :else 2)
        v-band  (min 5 (int (/ y (/ 80.0 6))))]
    (keyword (format "zone%02d" (+ (* h-third 6) v-band 1)))))

(defn estimate-xg
  "Computes a basic distance-and-angle xG estimate for a shot event.
   Returns a double in [0.01, 0.95]. Not a production model —
   illustrates the pipeline enrichment pattern.
   Returns nil for non-shot events."
  [{:keys [location under_pressure type] :as _event}]
  (when (= "Shot" (:name type))
    (let [dist  (euclidean-dist location goal-centre)
          angle (shot-angle location)
          base  (/ angle (* dist 2.5))
          adj   (if under_pressure (* base 0.85) base)]
      (max 0.01 (min 0.95 adj)))))

(defn enrich-event
  "Adds :zone and :xg keys to a shot event.
   Non-shot events are returned unchanged."
  [{:keys [location type] :as event}]
  (if (and (= "Shot" (:name type)) location)
    (assoc event
           :zone (compute-zone location)
           :xg   (estimate-xg event))
    event))
