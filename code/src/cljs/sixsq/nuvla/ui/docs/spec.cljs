(ns sixsq.nuvla.ui.docs.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::documents any?)


(s/def ::db (s/keys :req [::documents]))


(def defaults {::documents nil})
