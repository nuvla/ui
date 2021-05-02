(ns sixsq.nuvla.ui.dashboard.events
  (:require
    [re-frame.core :refer [reg-event-fx]]
    [sixsq.nuvla.ui.apps-store.events :as apps-store-events]
    [sixsq.nuvla.ui.credentials.events :as credentials-events]
    [sixsq.nuvla.ui.deployment.events :as deployment-events]
    [sixsq.nuvla.ui.edge.events :as edge-events]
    [sixsq.nuvla.ui.main.events :as main-events]))


(def refresh-action-deployments-id :dashboard-get-deployments-summary)
(def refresh-action-nuvlaboxes-summary-id :dashboard-get-nuvlaboxes-summary)
(def refresh-action-nuvlaboxes-id :dashboard-get-nuvlaboxes)
(def refresh-action-apps-id :dashboard-get-apps-summary)
(def refresh-action-credentials-id :dashboard-get-credentials-summary)


(reg-event-fx
  ::refresh
  (fn [_ _]
    {:fx [
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-deployments-id
                       :frequency 20000
                       :event     [::deployment-events/get-deployments-summary-all]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-nuvlaboxes-summary-id
                       :frequency 20000
                       :event     [::edge-events/get-nuvlaboxes-summary-all]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-nuvlaboxes-id
                       :frequency 20000
                       :event     [::edge-events/get-nuvlaboxes]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-apps-id
                       :frequency 20000
                       :event     [::apps-store-events/get-modules-summary]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-credentials-id
                       :frequency 20000
                       :event     [::credentials-events/get-credentials-summary]}]]]}))
