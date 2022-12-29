(ns sixsq.nuvla.ui.data-set.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.data-set.utils :as utils]
    [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
    [sixsq.nuvla.ui.utils.time :as time]))



(s/def ::not-found? boolean?)
(s/def ::time-period (s/tuple inst? inst?))
(s/def ::time-period-filter (s/nilable string?))
(s/def ::data-set-id (s/nilable string?))
(s/def ::data-set any?)
(s/def ::data-record-filter (s/nilable string?))
(s/def ::map-selection (s/nilable any?))
(s/def ::geo-operation (s/nilable string?))
(s/def ::data-records any?)
(s/def ::data-objects any?)
(s/def ::content-type-filter (s/nilable string?))
(s/def ::selected-data-record-ids (s/coll-of string? :kind set?))
(s/def ::pagination any?)

(def default-time-period [(time/days-before 30)
                          (time/days-before -1)])

(def defaults {::not-found?               false
               ::time-period              default-time-period
               ::time-period-filter       (utils/create-time-period-filter
                                            default-time-period)
               ::data-set-id              nil
               ::data-set                 nil
               ::data-record-filter       nil
               ::map-selection            nil
               ::geo-operation            "intersects"
               ::data-records             nil
               ::data-objects             {}
               ::content-type-filter      nil
               ::selected-data-record-ids #{}})

(def pagination-default {::pagination (pagination-plugin/build-spec
                                        :default-items-per-page 8)})
