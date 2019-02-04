(ns sixsq.slipstream.webui.deployment-detail.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.slipstream.webui.deployment-detail.spec :as spec]))


(reg-sub
  ::runUUID
  (fn [db]
    (::spec/runUUID db)))

(reg-sub
  ::reports
  ::spec/reports)

(reg-sub
  ::loading?
  ::spec/loading?)


(reg-sub
  ::deployment
  ::spec/deployment)


(reg-sub
  ::events
  (fn [db]
    (::spec/events db)))


(reg-sub
  ::jobs
  (fn [db]
    (::spec/jobs db)))


(reg-sub
  ::force-refresh-events-steps
  (fn [db]
    (::spec/force-refresh-events-steps db)))


(reg-sub
  ::global-deployment-parameters
  (fn [db]
    (::spec/global-deployment-parameters db)))


(reg-sub
  ::node-parameters-modal
  (fn [db]
    (::spec/node-parameters-modal db)))


(reg-sub
  ::node-parameters
  ::spec/node-parameters)


(reg-sub
  ::summary-nodes-parameters
  (fn [db]
    (::spec/summary-nodes-parameters db)))
