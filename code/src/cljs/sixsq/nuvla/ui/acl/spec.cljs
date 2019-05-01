(ns sixsq.nuvla.ui.acl.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::users-and-groups any?)

(s/def ::users-options any?)

(s/def ::groups-options any?)


(s/def ::db (s/keys :req [::users-and-groups
                          ::users-options
                          ::groups-options]))


(def defaults {::users-and-groups {}
               ::users-options    []
               ::groups-options   []})
