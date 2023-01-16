(ns sixsq.nuvla.ui.resource-log.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::resource-log any?)
(s/def ::id (s/nilable string?))
(s/def ::parent (s/nilable string?))
(s/def ::since (s/nilable #(inst? %)))
(s/def ::play? boolean?)
(s/def ::components any?)
(s/def ::available-components coll?)


(def defaults {::resource-log         nil
               ::id                   nil
               ::parent               nil
               ::play?                false
               ::since                (js/Date.)
               ::components           nil
               ::available-components []})
