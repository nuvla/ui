(ns sixsq.nuvla.ui.dashboard.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.dashboard.spec :as spec]
    [sixsq.nuvla.ui.dashboard.utils :as utils]
    [sixsq.nuvla.ui.deployment.events :as deployment-events]
    [sixsq.nuvla.ui.edge.events :as edge-events]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))


(def refresh-action-id :dashboard-get-deployments)

(reg-event-fx
  ::refresh
  (fn [{db :db} [_ {:keys [init? nuvlabox]}]]
    {:db (cond-> db
                 init? (assoc :db (merge db spec/defaults))
                 nuvlabox (assoc-in [:db ::spec/nuvlabox] nuvlabox))
     :fx [[:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-id
                       :frequency 20000
                       :event     [::deployment-events/get-all-deployments]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-id
                       :frequency 20000
                       :event     [::edge-events/get-nuvlaboxes]}]]]}))
