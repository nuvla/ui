(ns sixsq.slipstream.webui.docs.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)


(s/def ::documents any?)


(s/def ::db (s/keys :req [::loading?
                          ::documents]))


(def defaults {::loading?  true
               ::documents {}})
