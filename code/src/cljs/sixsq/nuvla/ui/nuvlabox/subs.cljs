(ns sixsq.nuvla.ui.nuvlabox.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.nuvlabox.spec :as spec]))


(reg-sub
  ::loading?
  ::spec/loading?)


(reg-sub
  ::health-info
  ::spec/health-info)


(reg-sub
  ::query-params
  ::spec/query-params)


(reg-sub
  ::nuvlaboxes
  ::spec/nuvlaboxes)


(reg-sub
  ::state-selector
  ::spec/state-selector)

(reg-sub
  ::elements-per-page
  ::spec/elements-per-page)


(reg-sub
  ::page
  ::spec/page)


(reg-sub
  ::state-nuvlaboxes
  ::spec/state-nuvlaboxes)


(reg-sub
  ::status-nuvlaboxes
  ::spec/status-nuvlaboxes)


(reg-sub
  ::status-nuvlabox
  :<- [::status-nuvlaboxes]
  (fn [{:keys [online offline]} [_ nuvlabox-id]]
    (cond
      (contains? online nuvlabox-id) :online
      (contains? offline nuvlabox-id) :offline
      :else :unknown)))

;(reg-sub
;  ::user-id
;  :<- [::session]
;  (fn [session _]
;    (:user session)))