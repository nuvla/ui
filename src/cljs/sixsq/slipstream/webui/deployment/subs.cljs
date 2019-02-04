(ns sixsq.slipstream.webui.deployment.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.slipstream.webui.deployment.spec :as spec]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))


(reg-sub
  ::deployments
  (fn [db]
    (::spec/deployments db)))


(reg-sub
  ::elements-per-page
  (fn [db]
    (::spec/elements-per-page db)))


(reg-sub
  ::page
  (fn [db]
    (::spec/page db)))


(reg-sub
  ::deployments-creds-map
  (fn [db]
    (::spec/deployments-creds-map db)))


(reg-sub
  ::deployments-service-url-map
  (fn [db]
    (::spec/deployments-service-url-map db)))


(reg-sub
  ::deployments-ss-state-map
  (fn [db]
    (::spec/deployments-ss-state-map db)))


(reg-sub
  ::active-only?
  (fn [db]
    (::spec/active-only? db)))


(reg-sub
  ::creds-name-map
  (fn [db]
    (::spec/creds-name-map db)))


(reg-sub
  ::view
  (fn [db]
    (::spec/view db)))