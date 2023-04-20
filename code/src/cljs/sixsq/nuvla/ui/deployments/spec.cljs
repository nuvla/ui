(ns sixsq.nuvla.ui.deployments.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :refer [build-ordering] :as table-plugin]))

(def resource-name "deployment")

(s/def ::deployments any?)
(s/def ::deployments-summary any?)
(s/def ::deployments-summary-all any?)
(s/def ::deployments-search any?)
(s/def ::additional-filter (s/nilable string?))
(s/def ::filter-external (s/nilable string?))
(s/def ::view #{"cards" "table"})
(s/def ::deployments-params-map {})
(s/def ::state-selector #{"all"
                          "started"
                          "starting"
                          "created"
                          "stopped"
                          "error"
                          "pending"})
(s/def ::bulk-update-modal any?)
(s/def ::selected-set set?)
(s/def ::select-all? boolean?)
(s/def ::bulk-jobs-monitored any?)
(s/def ::bulk-jobs any?)

(def default-ordering {:field :created :order "desc"})

(def defaults {::deployments-search      (full-text-search-plugin/build-spec)
               ::additional-filter       nil
               ::deployments             nil
               ::deployments-summary     nil
               ::deployments-summary-all nil
               ::deployments-params-map  nil
               ::filter-external         nil
               ::view                    "table"
               ::state-selector          "all"
               ::bulk-update-modal       nil
               ::selected-set            #{}
               ::select-all?             false
               ::bulk-jobs-monitored     (sorted-map)
               ::bulk-jobs               (bulk-progress-plugin/build-spec)
               ::ordering                (build-ordering)
               ::select                  (table-plugin/build-bulk-edit-spec)})

(def pagination-default {::pagination (pagination-plugin/build-spec
                                        :default-items-per-page 25)})
