(ns sixsq.nuvla.ui.data-record.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.data-record.utils :as utils]
    [sixsq.nuvla.ui.utils.time :as time]))


(s/def ::time-period (s/tuple any? any?))

(s/def ::time-period-filter (s/nilable string?))

(s/def ::data-set any?)

(s/def ::data-record-filter any?)

(s/def ::data-records any?)

(s/def ::data-objects any?)

(s/def ::content-type-filter (s/nilable string?))

(s/def ::full-text-search (s/nilable string?))

(s/def ::page int?)
(s/def ::elements-per-page int?)

(s/def ::db (s/keys :req [::time-period
                          ::time-period-filter
                          ::data-set
                          ::data-record-filter
                          ::data-records
                          ::data-objects
                          ::content-type-filter
                          ::full-text-search
                          ::selected-data-record-ids
                          ::page
                          ::elements-per-page]))

(def default-time-period [(time/days-before 30)
                          (time/days-before 0)])

(def defaults {::time-period                 default-time-period
               ::time-period-filter          (utils/create-time-period-filter default-time-period)
               ::data-set                    nil
               ::data-record-filter          nil
               ::data-records                nil
               ::data-objects                {}
               ::content-type-filter         nil
               ::full-text-search            nil
               ::selected-data-record-ids    #{}
               ::page                        1
               ::elements-per-page           8})
