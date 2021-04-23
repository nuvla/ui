(ns sixsq.nuvla.ui.deployment.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)

(s/def ::nuvlabox (s/nilable string?))

(s/def ::deployments any?)

(s/def ::deployments-summary any?)

(s/def ::deployments-summary-all any?)

(s/def ::page int?)

(s/def ::elements-per-page int?)

(s/def ::full-text-search (s/nilable string?))

(s/def ::additional-filter (s/nilable string?))

(s/def ::creds-name-map any?)

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

(s/def ::db (s/keys :req [::loading?
                          ::deployments
                          ::deployments-summary
                          ::deployments-summary-all
                          ::deployments-params-map
                          ::page
                          ::elements-per-page
                          ::full-text-search
                          ::additional-filter
                          ::nuvlabox
                          ::creds-name-map
                          ::view
                          ::state-selector
                          ::bulk-update-modal
                          ::selected-set
                          ::select-all?
                          ::bulk-jobs-monitored]))

(def defaults {::loading?                false
               ::page                    1
               ::elements-per-page       8
               ::full-text-search        nil
               ::additional-filter       nil
               ::nuvlabox                nil
               ::deployments             nil
               ::deployments-summary     nil
               ::deployments-summary-all nil
               ::deployments-params-map  nil
               ::creds-name-map          {}
               ::view                    "cards"
               ::state-selector          "all"
               ::bulk-update-modal       nil
               ::selected-set            #{}
               ::select-all?             false
               ::bulk-jobs-monitored     (sorted-map)})
