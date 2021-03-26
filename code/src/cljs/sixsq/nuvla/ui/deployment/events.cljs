(ns sixsq.nuvla.ui.deployment.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.deployment.spec :as spec]
    [sixsq.nuvla.ui.deployment.utils :as utils]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))


(def refresh-action-deployments-summary-id :dashboard-get-deployments-summary)
(def refresh-action-deployments-id :dashboard-get-deployments)
(def refresh-action-nuvlaboxes-id :dashboard-get-nuvlaboxes-summary)


(reg-event-fx
  ::refresh
  (fn [{db :db} [_ {:keys [init? nuvlabox]}]]
    {:db (cond-> db
                 init? (merge spec/defaults)
                 nuvlabox (assoc ::spec/nuvlabox nuvlabox))
     :fx [[:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-deployments-summary-id
                       :frequency 20000
                       :event     [::get-deployments-summary]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-deployments-id
                       :frequency 20000
                       :event     [::get-deployments]}]]
          ]}))


(reg-event-fx
  ::set-creds-ids
  (fn [_ [_ credentials-ids]]
    (when (not-empty credentials-ids)
      (let [filter-creds-ids (str/join " or " (map #(str "id='" % "'") credentials-ids))
            query-params     {:filter (str/join " and " [filter-creds-ids "name!=null"])
                              :select "id, name"}
            callback         (fn [response]
                               (when-not (instance? js/Error response)
                                 (dispatch [::set-creds-name-map (->> response
                                                                      :resources
                                                                      (map (juxt :id :name))
                                                                      (into {}))])))]
        {::cimi-api-fx/search [:credential query-params callback]}))))


(reg-event-db
  ::set-creds-name-map
  (fn [db [_ creds-name-map]]
    (assoc db ::spec/creds-name-map creds-name-map)))


(reg-event-db
  ::set-deployments-params-map
  (fn [db [_ {deployment-params :resources}]]
    (assoc db ::spec/deployments-params-map
              (group-by :parent deployment-params))))


(reg-event-fx
  ::set-deployments
  (fn [{:keys [db]} [_ {:keys [resources] :as deployments}]]
    {:db (assoc db ::spec/loading? false
                   ::spec/deployments deployments)}))


(reg-event-fx
  ::get-deployments
  (fn [{{:keys [::spec/full-text-search
                ::spec/additional-filter
                ::spec/state-selector
                ::spec/nuvlabox
                ::spec/page
                ::spec/elements-per-page]} :db} _]
    (let [state (if (= "all" state-selector) nil state-selector)]
      {::cimi-api-fx/search [:deployment (utils/get-query-params
                                           full-text-search additional-filter state
                                           nuvlabox page elements-per-page)
                             #(dispatch [::set-deployments %])]
       })))


(reg-event-fx
  ::set-deployments-summary
  (fn [{:keys [db]} [_ deployments]]
    {:db (assoc db ::spec/loading? false
                   ::spec/deployments-summary deployments)}))


(reg-event-fx
  ::get-deployments-summary
  (fn [{{:keys [::spec/full-text-search
                ::spec/additional-filter]} :db} _]
    {::cimi-api-fx/search [:deployment (utils/get-query-params-summary
                                         full-text-search additional-filter)
                           #(dispatch [::set-deployments-summary %])]}))


(reg-event-fx
  ::set-deployments-summary-all
  (fn [{:keys [db]} [_ deployments]]
    {:db (assoc db ::spec/loading? false
                   ::spec/deployments-summary-all deployments)}))


(reg-event-fx
  ::get-deployments-summary-all
  (fn [_]
    {::cimi-api-fx/search [:deployment (utils/get-query-params-summary nil nil)
                           #(dispatch [::set-deployments-summary-all %])]}))


(reg-event-fx
  ::set-page
  (fn [{{:keys [::spec/full-text-search
                ::spec/page
                ::spec/elements-per-page] :as db} :db} [_ page]]
    {:db       (assoc db ::spec/page page)
     :dispatch [::refresh]}))


(reg-event-fx
  ::set-full-text-search
  (fn [{{:keys [::spec/page
                ::spec/elements-per-page] :as db} :db} [_ full-text-search]]
    {:db       (-> db
                   (assoc ::spec/full-text-search full-text-search)
                   (assoc ::spec/page 1))
     :dispatch [::refresh]}))


(reg-event-fx
  ::set-additional-filter
  (fn [{{:keys [::spec/page
                ::spec/elements-per-page] :as db} :db} [_ additional-filter]]
    {:db       (-> db
                   (assoc ::spec/additional-filter additional-filter)
                   (assoc ::spec/page 1))
     :dispatch [::refresh]}))


(reg-event-db
  ::set-view
  (fn [db [_ view-type]]
    (assoc db ::spec/view view-type)))


(reg-event-fx
  ::stop-deployment
  (fn [_ [_ href]]
    {::cimi-api-fx/operation
     [href "stop"
      #(if (instance? js/Error %)
         (let [{:keys [status message]} (response/parse-ex-info %)]
           (dispatch [::messages-events/add
                      {:header  (cond-> (str "error stopping deployment " href)
                                        status (str " (" status ")"))
                       :content message
                       :type    :error}]))
         (dispatch [::get-deployments]))]}))


(reg-event-fx
  ::set-state-selector
  (fn [{db :db} [_ state-selector]]
    {:dispatch [::get-deployments]
     :db       (assoc db ::spec/state-selector state-selector
                         ::spec/page 1)}))
