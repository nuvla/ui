(ns sixsq.slipstream.webui.nuvlabox-detail.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.slipstream.webui.nuvlabox-detail.spec :as nuvlabox-spec]))


(reg-sub
  ::loading?
  ::nuvlabox-spec/loading?)


(reg-sub
  ::state
  ::nuvlabox-spec/state)


(reg-sub
  ::record
  ::nuvlabox-spec/record)
