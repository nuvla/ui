(ns sixsq.nuvla.ui.pages.dashboard.events
  (:require [re-frame.core :refer [reg-event-fx]]
            [sixsq.nuvla.ui.common-components.plugins.audit-log :as audit-log-plugin]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.pages.apps.apps-store.events :as apps-store-events]
            [sixsq.nuvla.ui.pages.credentials.events :as credentials-events]
            [sixsq.nuvla.ui.pages.dashboard.spec :as spec]
            [sixsq.nuvla.ui.pages.deployments.events :as deployments-events]
            [sixsq.nuvla.ui.pages.edges.events :as edges-events]))


(def refresh-action-deployments-id :dashboard-get-deployments-summary)
(def refresh-action-nuvlaboxes-summary-id :dashboard-get-nuvlaboxes-summary)
(def refresh-action-nuvlaboxes-id :dashboard-get-nuvlaboxes)
(def refresh-action-apps-id :dashboard-get-apps-summary)
(def refresh-action-credentials-id :dashboard-get-credentials-summary)
(def refresh-audit-logs-id :dashboard-load-events)

(reg-event-fx
  ::init
  (fn [{db :db}]
    {:db (merge db spec/defaults)}))

(reg-event-fx
  ::refresh
  (fn [_ _]
    {:fx [
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-deployments-id
                       :frequency 20000
                       :event     [::deployments-events/get-deployments-summary-all]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-nuvlaboxes-summary-id
                       :frequency 20000
                       :event     [::edges-events/get-nuvlaboxes-summary-all]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-nuvlaboxes-id
                       :frequency 20000
                       :event     [::edges-events/get-nuvlaboxes]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-apps-id
                       :frequency 20000
                       :event     [::apps-store-events/get-modules-summary]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-credentials-id
                       :frequency 20000
                       :event     [::credentials-events/get-credentials-summary]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-audit-logs-id
                       :frequency 20000
                       :event     [::audit-log-plugin/load-events
                                   [::spec/events]
                                   nil
                                   false]}]]]}))
