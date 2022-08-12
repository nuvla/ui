(ns sixsq.nuvla.ui.deployments.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::deployments any?)

(s/def ::deployments-summary any?)

(s/def ::deployments-summary-all any?)

(s/def ::page int?)

(s/def ::elements-per-page int?)

(s/def ::full-text-search (s/nilable string?))

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

(s/def ::db (s/keys :req [::deployments
                          ::deployments-summary
                          ::deployments-summary-all
                          ::deployments-params-map
                          ::page
                          ::elements-per-page
                          ::full-text-search
                          ::additional-filter
                          ::filter-external
                          ::view
                          ::state-selector
                          ::bulk-update-modal
                          ::selected-set
                          ::select-all?
                          ::bulk-jobs-monitored]))

(def defaults {::page                    1
               ::elements-per-page       8
               ::full-text-search        nil
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
               ::bulk-jobs-monitored     (sorted-map)})
