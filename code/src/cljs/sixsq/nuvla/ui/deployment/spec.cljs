(ns sixsq.nuvla.ui.deployment.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)

(s/def ::active-only? boolean?)

(s/def ::nuvlabox (s/nilable string?))

(s/def ::deployments any?)

(s/def ::page int?)

(s/def ::elements-per-page int?)

(s/def ::full-text-search (s/nilable string?))

(s/def ::creds-name-map any?)

(s/def ::view #{"cards" "table"})

(s/def ::deployments-params-map {})

(s/def ::db (s/keys :req [::loading?
                          ::deployments
                          ::deployments-params-map
                          ::page
                          ::elements-per-page
                          ::full-text-search
                          ::active-only?
                          ::nuvlabox
                          ::creds-name-map
                          ::view]))

(def defaults {::loading?               false
               ::page                   1
               ::elements-per-page      9
               ::full-text-search       nil
               ::active-only?           true
               ::nuvlabox               nil
               ::deployments            nil
               ::deployments-params-map nil
               ::creds-name-map         {}
               ::view                   "cards"})
