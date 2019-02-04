(ns sixsq.slipstream.webui.cimi.spec
  (:require-macros [sixsq.slipstream.webui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::baseURI string?)
(s/def ::collection-key (s/map-of string? keyword?))
(s/def ::collection-href (s/map-of keyword? string?))

(s/def ::cloud-entry-point (s/nilable (only-keys :req-un [::baseURI
                                                          ::collection-key
                                                          ::collection-href])))

(s/def ::$first nat-int?)
(s/def ::$last nat-int?)
(s/def ::$filter (s/nilable string?))
(s/def ::$orderby (s/nilable string?))
(s/def ::$select (s/nilable string?))
(s/def ::$aggregation (s/nilable string?))

(s/def ::query-params (s/keys :req-un [::$first
                                       ::$last
                                       ::$filter
                                       ::$orderby
                                       ::$select
                                       ::$aggregation]))

(s/def ::loading? boolean?)

(s/def ::aggregations any?)

(s/def ::collection any?)

(s/def ::collection-name (s/nilable string?))

(s/def ::selected-fields (s/coll-of string? :min-count 1))

(s/def ::available-fields (s/coll-of string? :min-count 1))

(s/def ::show-add-modal? boolean?)

(s/def ::descriptions-vector any?)

(s/def ::collections-templates-cache (s/map-of keyword? any?))

(s/def ::db (s/keys :req [::cloud-entry-point
                          ::query-params
                          ::loading?
                          ::aggregations
                          ::collection
                          ::collection-name
                          ::selected-fields
                          ::available-fields
                          ::show-add-modal?
                          ::collections-templates-cache]))

(def defaults {::cloud-entry-point   nil
               ::query-params        {:$first       0
                                      :$last        20
                                      :$filter      nil
                                      :$orderby     nil
                                      :$select      nil
                                      :$aggregation nil}
               ::loading?            false
               ::aggregations        nil
               ::collection          nil
               ::collection-name     nil
               ::selected-fields     ["id"]
               ::available-fields    ["id"]
               ::show-add-modal?     false
               ::collections-templates-cache {}})
