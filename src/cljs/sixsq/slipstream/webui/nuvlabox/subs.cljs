(ns sixsq.slipstream.webui.nuvlabox.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.slipstream.webui.nuvlabox.spec :as nuvlabox-spec]))


(reg-sub
  ::loading?
  ::nuvlabox-spec/loading?)


(reg-sub
  ::health-info
  ::nuvlabox-spec/health-info)


(reg-sub
  ::query-params
  ::nuvlabox-spec/query-params)


(reg-sub
  ::nuvlabox-records
  ::nuvlabox-spec/nuvlabox-records)


(reg-sub
  ::state-selector
  ::nuvlabox-spec/state-selector)

(reg-sub
  ::elements-per-page
  ::nuvlabox-spec/elements-per-page)


(reg-sub
  ::page
  ::nuvlabox-spec/page)
