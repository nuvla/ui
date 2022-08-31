(ns sixsq.nuvla.ui.deployments.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
    [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search]))

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

(def defaults {::deployments-search      (full-text-search/build-spec)
               ::additional-filter       nil
               ::deployments             nil
               ::deployments-summary     nil
               ::deployments-summary-all nil
               ::deployments-params-map  nil
               ::filter-external         nil
               ::view                    "cards"
               ::state-selector          "all"
               ::bulk-update-modal       nil
               ::selected-set            #{}
               ::select-all?             false
               ::bulk-jobs-monitored     (sorted-map)
               ::pagination              (pagination-plugin/build-spec
                                           :default-items-per-page 8)})
