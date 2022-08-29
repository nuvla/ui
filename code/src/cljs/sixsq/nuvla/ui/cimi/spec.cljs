(ns sixsq.nuvla.ui.cimi.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::base-uri string?)
(s/def ::collection-key (s/map-of string? keyword?))
(s/def ::collection-href (s/map-of keyword? string?))

(s/def ::cloud-entry-point (s/nilable (only-keys :req-un [::base-uri
                                                          ::collection-key
                                                          ::collection-href])))

(s/def ::first nat-int?)
(s/def ::last nat-int?)
(s/def ::filter (s/nilable string?))
(s/def ::orderby (s/nilable string?))
(s/def ::select (s/nilable string?))
(s/def ::aggregation (s/nilable string?))

(s/def ::query-params (s/keys :req-un [::first
                                       ::last
                                       ::filter
                                       ::orderby
                                       ::select
                                       ::aggregation]))

(s/def ::loading? boolean?)

(s/def ::aggregations any?)

(s/def ::collection any?)

(s/def ::collection-name (s/nilable string?))

(s/def ::selected-fields (s/coll-of string? :min-count 1))

(s/def ::available-fields (s/coll-of string? :min-count 1))

(s/def ::show-add-modal? boolean?)

(s/def ::descriptions-vector any?)

(s/def ::collections-templates-cache (s/map-of keyword? any?))

(s/def ::selected-rows (s/nilable set?))

(s/def ::resource-metadata any?)

(def defaults {::cloud-entry-point           nil
               ::query-params                {:first       0
                                              :last        20
                                              :filter      nil
                                              :orderby     nil
                                              :select      nil
                                              :aggregation nil}
               ::loading?                    false
               ::aggregations                nil
               ::collection                  nil
               ::collection-name             nil
               ::selected-fields             ["id", "name"]
               ::available-fields            ["id", "name"]
               ::show-add-modal?             false
               ::collections-templates-cache {}
               ::selected-rows               #{}
               ::resource-metadata           {}})
