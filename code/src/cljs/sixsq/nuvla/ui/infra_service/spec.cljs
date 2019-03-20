(ns sixsq.nuvla.ui.infra-service.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::services any?)

(s/def ::full-text-search (s/nilable string?))

(s/def ::page int?)

(s/def ::elements-per-page int?)


(s/def ::db (s/keys :req [::services
                          ::full-text-search
                          ::page
                          ::elements-per-page]))

(def defaults {::services nil
               ::full-text-search     nil
               ::page                 1
               ::elements-per-page    8})
