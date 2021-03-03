(ns sixsq.nuvla.ui.dashboard.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.dashboard.spec :as spec]
    [sixsq.nuvla.ui.dashboard.utils :as utils]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]))


(def refresh-action-id :dashboard-get-deployments)

(reg-event-fx
  ::refresh
  (fn [{db :db} [_ {:keys [init? nuvlabox]}]]
    (cond-> {:dispatch [::main-events/action-interval-start
                        {:id        refresh-action-id
                         :frequency 20000
                         :event     [::get-deployments]}]}
            init? (assoc :db (merge db spec/defaults))
            nuvlabox (assoc-in [:db ::spec/nuvlabox] nuvlabox))))


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
    (let [deployments-resource-ids (map :id resources)
          deployments-creds-ids    (distinct (map :parent resources))
          filter-deps-ids          (str/join " or " (map #(str "parent='" % "'")
                                                         deployments-resource-ids))
          query-params             {:filter (str "(" filter-deps-ids ") and value!=null")
                                    :select "parent, id, deployment, name, value"
                                    :last   10000}
          callback                 (fn [response]
                                     (when-not (instance? js/Error response)
                                       (dispatch [::set-deployments-params-map response])))]
      (cond-> {:db       (assoc db ::spec/loading? false
                                   ::spec/deployments deployments)
               :dispatch [::set-creds-ids deployments-creds-ids]}
              (not-empty deployments-resource-ids) (assoc ::cimi-api-fx/search
                                                          [:deployment-parameter
                                                           query-params callback])))))


(reg-event-fx
  ::get-deployments
  (fn [{{:keys [::spec/full-text-search
                ::spec/active-only?
                ::spec/nuvlabox
                ::spec/page
                ::spec/elements-per-page]} :db} _]
    {::cimi-api-fx/search [:deployment (utils/get-query-params full-text-search active-only?
                                                               nuvlabox page elements-per-page)
                           #(dispatch [::set-deployments %])]}))


(reg-event-fx
  ::set-page
  (fn [{{:keys [::spec/full-text-search
                ::spec/page
                ::spec/active-only?
                ::spec/elements-per-page] :as db} :db} [_ page]]
    {:db       (assoc db ::spec/page page)
     :dispatch [::refresh]}))


(reg-event-fx
  ::set-active-only?
  (fn [{{:keys [::spec/full-text-search
                ::spec/page
                ::spec/elements-per-page] :as db} :db} [_ active-only?]]
    {:db       (-> db
                   (assoc ::spec/active-only? active-only?)
                   (assoc ::spec/page 1))
     :dispatch [::refresh]}))

(reg-event-fx
  ::set-full-text-search
  (fn [{{:keys [::spec/page
                ::spec/active-only?
                ::spec/elements-per-page] :as db} :db} [_ full-text-search]]
    {:db       (-> db
                   (assoc ::spec/full-text-search full-text-search)
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
