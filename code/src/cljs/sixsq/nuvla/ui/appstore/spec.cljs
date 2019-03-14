(ns sixsq.nuvla.ui.appstore.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::modules any?)

(s/def ::full-text-search (s/nilable string?))

(s/def ::page int?)

(s/def ::elements-per-page int?)


(s/def ::db (s/keys :req [::modules
                          ::full-text-search
                          ::page
                          ::elements-per-page]))

(def defaults {::modules nil
               ::full-text-search     nil
               ::page                 1
               ::elements-per-page    8})
