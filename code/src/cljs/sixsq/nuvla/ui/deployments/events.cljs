(ns sixsq.nuvla.ui.deployments.events
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.deployments.spec :as spec]
    [sixsq.nuvla.ui.deployments.utils :as utils]
    [sixsq.nuvla.ui.edges-detail.spec :as edges-detail-spec]
    [sixsq.nuvla.ui.job.events :as job-events]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]))


(def refresh-action-deployments-summary-id :dashboard-get-deployments-summary)
(def refresh-action-deployments-id :dashboard-get-deployments)
(def refresh-action-nuvlaboxes-id :dashboard-get-nuvlaboxes-summary)


(reg-event-fx
  ::refresh
  (fn [{_db :db} [_]]
    {:fx [[:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-deployments-summary-id
                       :frequency 20000
                       :event     [::get-deployments-summary]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-deployments-id
                       :frequency 20000
                       :event     [::get-deployments]}]]]}))


(reg-event-db
  ::set-deployments-params-map
  (fn [db [_ {deployment-params :resources}]]
    (assoc db ::spec/deployments-params-map
              (group-by :parent deployment-params))))


(reg-event-fx
  ::set-deployments
  (fn [{:keys [db]} [_ {:keys [resources] :as deployments}]]
    (let [deployments-resource-ids (map :id resources)
          filter-deps-ids          (str/join " or " (map #(str "parent='" % "'")
                                                         deployments-resource-ids))
          query-params             {:filter (str "(" filter-deps-ids ") and value!=null")
                                    :select "parent, id, deployment, name, value"
                                    :last   10000}
          callback                 (fn [response]
                                     (when-not (instance? js/Error response)
                                       (dispatch [::set-deployments-params-map response])))]
      (cond-> {:db (assoc db ::main-spec/loading? false
                             ::spec/deployments deployments)}
              (not-empty deployments-resource-ids) (assoc ::cimi-api-fx/search
                                                          [:deployment-parameter
                                                           query-params callback])))))


(reg-event-fx
  ::get-module-deployments
  (fn [{{:keys [::spec/full-text-search
                ::spec/additional-filter
                ::spec/state-selector
                ::apps-spec/module
                ::spec/page
                ::spec/elements-per-page]} :db} _]
    (let [state     (when-not (= "all" state-selector) state-selector)
          module-id (:id module)]
      {::cimi-api-fx/search [:deployment (utils/get-query-params
                                           {:full-text-search  full-text-search
                                            :additional-filter additional-filter
                                            :state-selector    state
                                            :module-id         module-id
                                            :page              page
                                            :elements-per-page elements-per-page})
                             #(dispatch [::set-deployments %])]})))


(reg-event-fx
  ::get-nuvlabox-deployments
  (fn [{{:keys [::spec/full-text-search
                ::spec/additional-filter
                ::spec/state-selector
                ::edges-detail-spec/nuvlabox
                ::spec/page
                ::spec/elements-per-page]} :db} _]
    (let [state (when-not (= "all" state-selector) state-selector)
          nb-id (:id nuvlabox)]
      {::cimi-api-fx/search [:deployment (utils/get-query-params
                                           {:full-text-search  full-text-search
                                            :additional-filter additional-filter
                                            :state-selector    state
                                            :nuvlabox          nb-id
                                            :page              page
                                            :elements-per-page elements-per-page})
                             #(dispatch [::set-deployments %])]})))


(reg-event-fx
  ::get-deployments
  (fn [{{:keys [::spec/full-text-search
                ::spec/additional-filter
                ::spec/state-selector
                ::spec/page
                ::spec/elements-per-page]} :db}]
    (let [state (when-not (= "all" state-selector) state-selector)]
      {::cimi-api-fx/search [:deployment (utils/get-query-params
                                           {:full-text-search  full-text-search
                                            :additional-filter additional-filter
                                            :state-selector    state
                                            :page              page
                                            :elements-per-page elements-per-page})
                             #(dispatch [::set-deployments %])]})))


(reg-event-fx
  ::set-deployments-summary
  (fn [{:keys [db]} [_ deployments]]
    {:db (assoc db ::main-spec/loading? false
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
    {:db (assoc db ::main-spec/loading? false
                   ::spec/deployments-summary-all deployments)}))


(reg-event-fx
  ::get-deployments-summary-all
  (fn [_]
    {::cimi-api-fx/search [:deployment (utils/get-query-params-summary nil nil)
                           #(dispatch [::set-deployments-summary-all %])]}))


(reg-event-fx
  ::set-page
  (fn [{db :db} [_ page]]
    {:db       (assoc db ::spec/page page)
     :dispatch [::refresh]}))


(reg-event-fx
  ::set-full-text-search
  (fn [{db :db} [_ full-text-search]]
    {:db       (-> db
                   (assoc ::spec/full-text-search full-text-search)
                   (assoc ::spec/page 1)
                   (assoc ::spec/selected-set #{}))
     :dispatch [::refresh]}))


(reg-event-fx
  ::set-additional-filter
  (fn [{db :db} [_ additional-filter]]
    {:db       (assoc db ::spec/additional-filter additional-filter
                         ::spec/page 1
                         ::spec/selected-set #{})
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
                         ::spec/page 1
                         ::spec/selected-set #{})}))


(reg-event-fx
  ::open-modal-bulk-update
  (fn [{db :db} [_ filter-str module-href]]
    (cond-> {:db (assoc db ::spec/bulk-update-modal {:filter-str  filter-str
                                                     :module-href module-href})}
            module-href (assoc
                          ::cimi-api-fx/get
                          [module-href
                           #(dispatch
                              [:sixsq.nuvla.ui.deployments-detail.events/set-module-versions %])]))))


(reg-event-db
  ::close-modal-bulk-update
  (fn [db _]
    (assoc db ::spec/bulk-update-modal nil)))


(reg-event-fx
  ::bulk-update-params
  (fn [{db :db}]
    (let [filter-str (utils/build-bulk-filter db)]
      {::cimi-api-fx/search
       [:deployment {:last        0
                     :aggregation "terms:module/id"
                     :filter      (utils/build-bulk-filter db)}
        #(let [buckets      (get-in % [:aggregations :terms:module/id :buckets])
               same-module? (= (count buckets) 1)
               module-href  (when same-module? (-> buckets first :key))]
           (dispatch [::open-modal-bulk-update filter-str module-href]))]})))


(reg-event-fx
  ::bulk-operation
  (fn [{db :db} [_ bulk-action data dispatch-vec]]
    (cond-> {::cimi-api-fx/operation-bulk
             [:deployment
              (fn [response]
                (dispatch [::job-events/wait-job-to-complete
                           {:job-id              (:location response)
                            :on-complete         #(do
                                                    (dispatch [::add-bulk-job-monitored %])
                                                    (dispatch [::reset-selected-set]))
                            :on-refresh          #(dispatch [::add-bulk-job-monitored %])
                            :refresh-interval-ms 10000}]))
              bulk-action (utils/build-bulk-filter db) data]}
            dispatch-vec (assoc :dispatch dispatch-vec))))


(reg-event-db
  ::select-id
  (fn [{:keys [::spec/selected-set] :as db} [_ id]]
    (let [fn (if (utils/is-selected? selected-set id) disj conj)]
      (update db ::spec/selected-set fn id))))


(reg-event-db
  ::select-all-page
  (fn [{:keys [::spec/selected-set
               ::spec/deployments] :as db} _]
    (let [visible-dep-ids    (utils/visible-deployment-ids deployments)
          all-page-selected? (utils/all-page-selected? selected-set visible-dep-ids)
          fn                 (if all-page-selected? set/difference set/union)]
      (-> db
          (update ::spec/selected-set fn visible-dep-ids)
          (assoc ::spec/select-all? false)))))


(reg-event-db
  ::select-all
  (fn [db]
    (-> db
        (update ::spec/select-all? not)
        (assoc ::spec/selected-set #{}))))


(reg-event-db
  ::reset-selected-set
  (fn [db]
    (assoc db ::spec/selected-set #{})))


(reg-event-db
  ::add-bulk-job-monitored
  (fn [db [_ {:keys [id] :as job}]]
    (update db ::spec/bulk-jobs-monitored assoc id job)))


(reg-event-db
  ::dissmiss-bulk-job-monitored
  (fn [db [_ job-id]]
    (update db ::spec/bulk-jobs-monitored dissoc job-id)))
