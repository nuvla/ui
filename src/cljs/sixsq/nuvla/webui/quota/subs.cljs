(ns sixsq.nuvla.webui.quota.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.webui.quota.spec :as quota-spec]))


(reg-sub
  ::loading-quotas?
  ::quota-spec/loading-quotas?)


(reg-sub
  ::credentials-quotas-map
  ::quota-spec/credentials-quotas-map)

