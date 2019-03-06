(ns sixsq.nuvla.ui.data.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.data.utils :as utils]
    [sixsq.nuvla.ui.utils.time :as time]))


(s/def ::time-period (s/tuple any? any?))

(s/def ::time-period-filter (s/nilable string?))

(s/def ::data-records any?)

(s/def ::credentials (s/nilable (s/coll-of any? :kind vector?)))

(s/def ::cloud-filter (s/nilable string?))

(s/def ::content-type-filter (s/nilable string?))

(s/def ::application-select-visible? boolean?)

(s/def ::loading-applications? boolean?)

(s/def ::applications (s/nilable (s/coll-of any? :kind vector?)))

(s/def ::selected-application-id (s/nilable string?))

(s/def ::full-text-search (s/nilable string?))

(s/def ::counts any?)

(s/def ::sizes any?)

(s/def ::data-sets (s/map-of string? map?))

(s/def ::data-records-by-data-set (s/map-of string? vector?))

(s/def ::selected-data-set-ids (s/coll-of string? :kind set?))

(s/def ::db (s/keys :req [::time-period
                          ::time-period-filter
                          ::data-records
                          ::credentials
                          ::cloud-filter
                          ::application-select-visible?
                          ::loading-applications?
                          ::applications
                          ::content-type-filter
                          ::full-text-search
                          ::counts
                          ::sizes
                          ::data-sets
                          ::data-records-by-data-set
                          ::selected-data-set-ids
                          ]))

(def default-time-period [(time/days-before 30)
                          (time/days-before 0)])

(def defaults {::time-period                 default-time-period
               ::time-period-filter          (utils/create-time-period-filter default-time-period)
               ::data-records                nil
               ::credentials                 nil
               ::cloud-filter                nil
               ::application-select-visible? false
               ::loading-applications?       false
               ::applications                nil
               ::content-type-filter         nil
               ::full-text-search            nil
               ::counts                      nil
               ::sizes                       nil
               ::data-sets                   {}
               ::data-records-by-data-set    {}
               ::selected-data-set-ids       #{}
               })
