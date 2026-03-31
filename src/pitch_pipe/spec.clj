(ns pitch-pipe.spec
  "Clojure.spec schemas for StatsBomb event structures.
   Validates events at the data boundary before they enter the transducer pipeline."
  (:require [clojure.spec.alpha :as s]))

;; Location is a 2-element vector of numbers [x y]
;; StatsBomb pitch: x 0-120, y 0-80
(s/def ::x (s/and number? #(>= % 0.0) #(<= % 120.0)))
(s/def ::y (s/and number? #(>= % 0.0) #(<= % 80.0)))
(s/def ::location (s/tuple ::x ::y))

;; Event-level id — UUID string from StatsBomb JSON
(s/def ::id (s/or :uuid uuid? :string string?))

;; Type sub-map — has its own integer :id and string :name
;; We use a separate spec namespace to avoid conflict with event ::id
(s/def ::type-id (s/or :int int? :string string?))
(s/def ::type-name string?)
(s/def ::type (s/keys :req-un [::type-id ::type-name]))

;; Re-register unqualified keys for the type map
;; s/keys with :req-un matches unqualified :id and :name against
;; the specs in THIS namespace. We need type's :id to accept ints.
;; Solution: define ::type with a conformer.
(s/def ::type
  (s/and map?
         #(contains? % :id)
         #(contains? % :name)
         #(string? (:name %))
         #(or (int? (:id %)) (string? (:id %)))))

(s/def ::index pos-int?)
(s/def ::minute (s/and int? #(>= % 0) #(<= % 130)))
(s/def ::second (s/and int? #(>= % 0) #(<= % 59)))
(s/def ::under_pressure boolean?)

;; Top-level event — only require fields that are present on ALL events
(s/def ::event
  (s/and map?
         #(contains? % :id)
         #(contains? % :index)
         #(contains? % :minute)
         #(contains? % :second)
         #(contains? % :type)
         #(s/valid? ::id (:id %))
         #(s/valid? ::index (:index %))
         #(s/valid? ::minute (:minute %))
         #(s/valid? ::second (:second %))
         #(s/valid? ::type (:type %))))

;; Shot-specific
(s/def ::shot-event
  (s/and ::event
         #(= "Shot" (get-in % [:type :name]))
         #(contains? % :location)))

(defn validate-event!
  "Validates a single StatsBomb event map against ::event spec.
   Throws ex-info with :type :pitch-pipe/spec-violation on failure.
   Returns the event unchanged on success."
  [event]
  (if (s/valid? ::event event)
    event
    (throw (ex-info "StatsBomb event failed spec validation"
                    {:type     :pitch-pipe/spec-violation
                     :event-id (:id event)
                     :explain  (s/explain-str ::event event)}))))
