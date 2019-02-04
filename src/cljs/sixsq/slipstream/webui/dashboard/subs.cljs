(ns sixsq.slipstream.webui.dashboard.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.slipstream.webui.dashboard.spec :as dashboard-spec]))


(reg-sub
  ::statistics
  ::dashboard-spec/statistics)


(reg-sub
  ::loading?
  ::dashboard-spec/loading?)

(reg-sub
  ::selected-tab
  ::dashboard-spec/selected-tab)

(reg-sub
  ::virtual-machines
  ::dashboard-spec/virtual-machines)

(reg-sub
  ::deployments
  ::dashboard-spec/deployments)

(reg-sub
  ::records-displayed
  ::dashboard-spec/records-displayed)

(reg-sub
  ::page
  ::dashboard-spec/page)

(reg-sub
  ::total-pages
  ::dashboard-spec/total-pages)

(reg-sub
  ::active-deployments-only
  ::dashboard-spec/active-deployments-only)

(reg-sub
  ::deleted-deployments
  ::dashboard-spec/deleted-deployments)

(reg-sub
  ::delete-deployment-modal
  ::dashboard-spec/delete-deployment-modal)

(reg-sub
  ::error-message-deployment
  ::dashboard-spec/error-message-deployment)

(reg-sub
  ::loading-tab?
  ::dashboard-spec/loading-tab?)
