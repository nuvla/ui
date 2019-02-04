(ns sixsq.slipstream.webui.metrics.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.cimi-api.effects :as cimi-api-fx]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.metrics.effects :as metrics-fx]
    [sixsq.slipstream.webui.metrics.spec :as metrics-spec]))


(reg-event-db
  ::set-metrics
  (fn [db [_ metrics]]
    (assoc db
      ::metrics-spec/loading? false
      ::metrics-spec/raw-metrics metrics)))


(reg-event-fx
  ::fetch-metrics
  (fn [{:keys [db]} _]
    (if-let [client (::client-spec/client db)]
      {:db                   (assoc db ::metrics-spec/loading? true)
       ::cimi-api-fx/metrics [client #(dispatch [::set-metrics %])]}
      {:db db})))


(reg-event-db
  ::set-job-info
  (fn [db [_ job-info]]
    (assoc db
      ::metrics-spec/loading-job-info? false
      ::metrics-spec/job-info job-info)))


(reg-event-fx
  ::fetch-job-info
  (fn [{:keys [db]} _]
    (if-let [client (::client-spec/client db)]
      {:db                         (assoc db ::metrics-spec/loading-job-info? true)
       ::metrics-fx/fetch-job-info [client #(dispatch [::set-job-info %])]}
      {:db db})))
