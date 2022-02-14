(ns sixsq.nuvla.ui.log-resource.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.time :as time]))

(s/def ::resource-log any?)
(s/def ::id (s/nilable string?))
(s/def ::since (s/nilable string?))
(s/def ::play? boolean?)
(s/def ::components any?)


(s/def ::db (s/keys :req [::resource-log
                          ::id
                          ::since
                          ::play?
                          ::components]))

(defn default-since []
  (-> (time/now) (.seconds 0)))

(def defaults {::resource-log nil
               ::id           nil
               ::play?        false
               ::since        (default-since)
               ::components   nil})
