(ns sixsq.slipstream.webui.cimi-detail.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.slipstream.webui.cimi-detail.spec :as cimi-detail-spec]))


(reg-sub
  ::loading?
  ::cimi-detail-spec/loading?)


(reg-sub
  ::resource-id
  ::cimi-detail-spec/resource-id)


(reg-sub
  ::resource
  ::cimi-detail-spec/resource)
