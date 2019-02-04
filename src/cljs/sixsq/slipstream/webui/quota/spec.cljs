(ns sixsq.slipstream.webui.quota.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading-quotas? boolean?)


(s/def ::credentials-quotas-map any?)


(s/def ::db (s/keys :req [::loading-quotas?
                          ::credentials-quotas-map]))


(def defaults {::loading-quotas?        true
               ::credentials-quotas-map {}})
