(ns sixsq.slipstream.webui.data.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.slipstream.webui.data.utils :as utils]
    [sixsq.slipstream.webui.utils.time :as time]))


(s/def ::time-period (s/tuple any? any?))
(s/def ::time-period-filter (s/nilable string?))


(s/def ::service-offers any?)

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

(s/def ::datasets (s/map-of string? map?))

(s/def ::service-offers-by-dataset (s/map-of string? vector?))

(s/def ::selected-dataset-ids (s/coll-of string? :kind set?))

(s/def ::db (s/keys :req [::time-period
                          ::time-period-filter
                          ::service-offers
                          ::credentials
                          ::cloud-filter
                          ::application-select-visible?
                          ::loading-applications?
                          ::applications
                          ::content-type-filter
                          ::full-text-search
                          ::counts
                          ::sizes
                          ::datasets
                          ::service-offers-by-dataset
                          ::selected-dataset-ids
                          ]))

(def default-time-period [(time/days-before 30)
                          (time/days-before 0)])

(def defaults {::time-period                 default-time-period
               ::time-period-filter          (utils/create-time-period-filter default-time-period)
               ::service-offers              nil
               ::credentials                 nil
               ::cloud-filter                nil
               ::application-select-visible? false
               ::loading-applications?       false
               ::applications                nil
               ::content-type-filter         nil
               ::full-text-search            nil
               ::counts                      nil
               ::sizes                       nil
               ::datasets                    {}
               ::service-offers-by-dataset   {}
               ::selected-dataset-ids        #{}
               })
