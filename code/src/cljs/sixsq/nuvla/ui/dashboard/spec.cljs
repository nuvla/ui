(ns sixsq.nuvla.ui.dashboard.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::db (s/keys :req []))

(def defaults {})
