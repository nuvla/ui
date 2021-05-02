(ns sixsq.nuvla.ui.dashboard.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)

(s/def ::db (s/keys :req [::loading?]))

(def defaults {::loading?               false})
