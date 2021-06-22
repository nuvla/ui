(ns sixsq.nuvla.ui.acl.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::groups any?)


(s/def ::db (s/keys :req [::groups]))


(def defaults {::groups []})
