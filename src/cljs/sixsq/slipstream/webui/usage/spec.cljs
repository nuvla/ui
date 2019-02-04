(ns sixsq.slipstream.webui.usage.spec
  (:require-macros [sixsq.slipstream.webui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.slipstream.webui.usage.utils :as u]))


(s/def ::initialized? boolean?)

(s/def ::loading-totals? boolean?)

(s/def ::totals any?)

(s/def ::loading-details? boolean?)

(s/def ::results any?)

(s/def ::credentials-map any?)

(s/def ::selected-credentials vector?)

(s/def ::selected-users-roles (s/nilable string?))

(s/def ::loading-credentials-map? boolean?)

(s/def ::column (s/nilable keyword?))

(s/def ::direction #{:ascending :descending})

(s/def ::sort (s/keys :req-un {::column ::direction}))

(s/def ::users-roles-list (s/coll-of string? :kind vector?))

(s/def ::date-range (s/tuple any? any?))

(s/def ::billable-only? boolean?)

(s/def ::is-admin? boolean?)

(s/def ::db (s/keys :req [::initialized?
                          ::loading-totals?
                          ::loading-details?
                          ::totals
                          ::results
                          ::credentials-map
                          ::selected-credentials
                          ::loading-credentials-map?
                          ::sort
                          ::selected-users-roles
                          ::users-roles-list
                          ::date-range
                          ::billable-only?
                          ::is-admin?]))

(def defaults {::initialized?             false
               ::loading-totals?          false
               ::loading-details?         false
               ::totals                   nil
               ::results                  nil
               ::credentials-map          {}
               ::selected-credentials     []
               ::loading-credentials-map? true
               ::sort                     {:column :price, :direction :descending}
               ::selected-users-roles     nil
               ::users-roles-list         []
               ::date-range               (get u/date-range-entries u/default-date-range)
               ::billable-only?           true
               ::is-admin?                false})
