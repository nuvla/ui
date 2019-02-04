(ns sixsq.slipstream.webui.usage.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.slipstream.webui.usage.spec :as usage-spec]))


(reg-sub
  ::initialized?
  ::usage-spec/initialized?)


(reg-sub
  ::loading-totals?
  ::usage-spec/loading-totals?)


(reg-sub
  ::totals
  ::usage-spec/totals)


(reg-sub
  ::loading-details?
  ::usage-spec/loading-details?)


(reg-sub
  ::results
  ::usage-spec/results)


(reg-sub
  ::credentials-map
  ::usage-spec/credentials-map)


(reg-sub
  ::selected-credentials
  ::usage-spec/selected-credentials)


(reg-sub
  ::loading-credentials-map?
  ::usage-spec/loading-credentials-map?)


(reg-sub
  ::sort
  ::usage-spec/sort)


(reg-sub
  ::selected-users-roles
  ::usage-spec/selected-users-roles)


(reg-sub
  ::users-roles-list
  ::usage-spec/users-roles-list)


(reg-sub
  ::date-range
  ::usage-spec/date-range)


(reg-sub
  ::billable-only?
  ::usage-spec/billable-only?)
