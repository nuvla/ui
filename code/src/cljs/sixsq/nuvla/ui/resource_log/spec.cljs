(ns sixsq.nuvla.ui.resource-log.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.time :as time]))

(s/def ::resource-log any?)
(s/def ::id (s/nilable string?))
(s/def ::parent (s/nilable string?))
(s/def ::since (s/nilable string?))
(s/def ::play? boolean?)
(s/def ::components any?)
(s/def ::available-components coll?)

(defn default-since []
  (.seconds (time/now) 0))

(def defaults {::resource-log nil
               ::id           nil
               ::parent       nil
               ::play?        false
               ::since        (default-since)
               ::components   nil
               ::available-components []})
