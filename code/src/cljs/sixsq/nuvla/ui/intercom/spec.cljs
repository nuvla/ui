(ns sixsq.nuvla.ui.intercom.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::events any?)


(s/def ::db (s/keys :req [::events]))


(def defaults {::events {}})
