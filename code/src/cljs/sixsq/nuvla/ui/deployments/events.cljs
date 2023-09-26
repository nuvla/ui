(ns sixsq.nuvla.ui.deployments.events
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.deployments.spec :as spec]
            [sixsq.nuvla.ui.deployments.utils :as utils :refer [build-bulk-filter]]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :as table-plugin :refer [ordering->order-string]]
            [sixsq.nuvla.ui.routing.events :as route-events]
            [sixsq.nuvla.ui.routing.utils :refer [get-query-param
                                                  get-stored-db-value-from-query-param]]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.utils.general :as general-utils :refer [create-filter-for-read-only-resources]]
            [sixsq.nuvla.ui.utils.response :as response]))

(def refresh-action-deployments-summary-id :dashboard-get-deployments-summary)
(def refresh-action-deployments-id :dashboard-get-deployments)

(reg-event-fx
  ::init
  (fn [{{:keys [current-route] :as db} :db} _]
    (let [state-filter (get-stored-db-value-from-query-param current-route [::spec/state-selector])
          filter-query  (get-query-param current-route (keyword spec/resource-name))]
      {:db (merge db spec/defaults
                  {::spec/state-selector    state-filter
                   ::spec/additional-filter filter-query})
       :fx [[:dispatch [::refresh]]]})))

(reg-event-fx
  ::refresh
  (fn [_ [_ db-path]]
    {:fx [[:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-deployments-summary-id
                       :frequency 20000
                       :event     [::get-deployments-summary]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-deployments-id
                       :frequency 20000
                       :event     [::get-deployments {:pagination-db-path db-path}]}]]]}))

(reg-event-db
  ::set-deployments-params-map
  (fn [db [_ {deployment-params :resources}]]
    (assoc db ::spec/deployments-params-map
              (group-by :parent deployment-params))))

(reg-event-fx
  ::set-deployments
  (fn [{:keys [db]} [_ {:keys [resources] :as deployments}]]
    (let [deployments-resource-ids (map :id resources)
          filter-deps-ids           (str/join " or " (map #(str "parent='" % "'")
                                                          deployments-resource-ids))
          query-params             {:filter (str "(" filter-deps-ids ") and value!=null")
                                    :select "parent, id, deployment, name, value"
                                    :last   10000}
          callback                 (fn [response]
                                     (when-not (instance? js/Error response)
                                       (dispatch [::set-deployments-params-map response])))]
      (cond-> {:db (assoc db ::main-spec/loading? false
                          ::spec/deployments deployments)
               :fx [[:dispatch [::get-edges-of-deployments resources]]]}
              (not-empty deployments-resource-ids) (assoc ::cimi-api-fx/search
                                                   [:deployment-parameter
                                                    query-params callback])))))

(reg-event-fx
  ::get-deployments
  (fn [{{:keys [::spec/additional-filter
                ::spec/state-selector
                ::spec/filter-external
                ::spec/ordering] :as db} :db} [_ {:keys [filter-external-arg
                                                         pagination-db-path
                                                         external-filter-only?]}]]
    (let [filter-external (or filter-external-arg filter-external)
          filter-str      (utils/get-filter-param
                            (if external-filter-only?
                              {:filter-external filter-external}
                              {:full-text-search  (full-text-search-plugin/filter-text
                                                    db [::spec/deployments-search])
                               :additional-filter additional-filter
                               :state-selector    state-selector
                               :filter-external   filter-external}))]
      {:db                  (assoc db ::spec/filter-external filter-external)
       ::cimi-api-fx/search [:deployment
                             (->> {:aggregation "terms:state"
                                   :orderby     (ordering->order-string ordering)
                                   :filter      filter-str}
                                  (pagination-plugin/first-last-params
                                    db [(or pagination-db-path ::spec/pagination)]))
                             #(dispatch [::set-deployments %])]})))

(reg-event-fx
 ::get-edges-of-deployments
 (fn [_ [_ resources]]
   (let [filter-str (apply
                     general-utils/join-or
                     (map (fn [deployment]
                            (str "id='" (:nuvlabox deployment) "'"))
                          (filter :nuvlabox resources)))]
     {::cimi-api-fx/search
      [:nuvlabox
       (->> {:filter filter-str})
       #(dispatch [::set-deployment-edges %])]})))

(reg-event-db
 ::set-deployment-edges
 (fn [db [_ response]]
   (let [edges (:resources response)]
     (assoc db ::spec/deployment-edges (zipmap (map :id edges)
                                               edges)))))

(reg-event-fx
  ::set-deployments-summary
  (fn [{:keys [db]} [_ deployments]]
    {:db (assoc db ::main-spec/loading? false
                   ::spec/deployments-summary deployments)}))

(reg-event-fx
  ::get-deployments-summary
  (fn [{{:keys [::spec/additional-filter] :as db} :db} _]
    {::cimi-api-fx/search [:deployment (utils/get-query-params-summary
                                         {:full-text-search (full-text-search-plugin/filter-text
                                                              db [::spec/deployments-search])
                                          :additional-filter additional-filter})
                           #(dispatch [::set-deployments-summary %])]}))

(reg-event-fx
  ::set-deployments-summary-all
  (fn [{:keys [db]} [_ deployments]]
    {:db (assoc db ::main-spec/loading? false
                   ::spec/deployments-summary-all deployments)}))

(reg-event-fx
  ::get-deployments-summary-all
  (fn [_ [_ external-filter]]
    {::cimi-api-fx/search [:deployment (utils/get-query-params-summary {:external-filter external-filter})
                           #(dispatch [::set-deployments-summary-all %])]}))

(reg-event-fx
  ::reset-deployments-summary-all
  (fn []
    {:fx [[:dispatch [::set-deployments-summary-all nil]]]}))

(reg-event-fx
  ::set-additional-filter
  (fn [{db :db} [_ additional-filter]]
    {:db (assoc db ::spec/additional-filter additional-filter
                   ::spec/selected-set #{})
     :fx [[:dispatch
           [::pagination-plugin/change-page [::spec/pagination] 1]]]}))

(reg-event-db
  ::set-view
  (fn [db [_ view-type]]
    (assoc db ::spec/view view-type)))


(reg-event-fx
  ::set-state-selector
  (fn [{db :db} [_ state-selector]]
    (let [db-path ::spec/state-selector]
      {:db (assoc db db-path state-selector
             ::spec/selected-set #{})
       :fx [[:dispatch [::route-events/store-in-query-param {:db-path [db-path]
                                                             :value state-selector}]]
            [:dispatch
             [::pagination-plugin/change-page [::spec/pagination] 1]]]})))

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

(reg-event-db
  ::open-modal-bulk-stop
  (fn [db]
    (assoc db ::spec/bulk-stop-modal true)))

(reg-event-db
  ::close-modal-bulk-stop
  (fn [db _]
    (assoc db ::spec/bulk-stop-modal false)))

(reg-event-db
  ::open-modal-bulk-delete
  (fn [db]
    (assoc db ::spec/bulk-delete-modal true)))

(reg-event-db
  ::close-modal-bulk-delete
  (fn [db _]
    (assoc db ::spec/bulk-delete-modal false)))

(reg-event-fx
  ::bulk-update-params
  (fn [{db :db}]
    (let [filter-str (utils/build-bulk-filter db)]
      {::cimi-api-fx/search
       [:deployment {:last        0
                     :aggregation "terms:module/id"
                     :filter      filter-str}
        #(let [buckets      (get-in % [:aggregations :terms:module/id :buckets])
               same-module? (= (count buckets) 1)
               module-href  (when same-module? (-> buckets first :key))]
           (dispatch [::open-modal-bulk-update filter-str module-href]))]})))

(reg-event-fx
  ::bulk-operation
  (fn [{db :db} [_ bulk-action data dispatch-vec]]
    (cond-> {::cimi-api-fx/operation-bulk
             [:deployment
              (fn [{:keys [location] :as _response}]
                (dispatch [::bulk-progress-plugin/monitor
                           [::spec/bulk-jobs] location])
                (dispatch [::table-plugin/reset-bulk-edit-selection [::spec/select]]))
              bulk-action (utils/build-bulk-filter db) data]}
            dispatch-vec (assoc :dispatch dispatch-vec))))


(reg-event-fx
  ::get-deployments-without-edit-rights
  (fn [{{:keys [::session-spec/session] :as db} :db} _]
    (let [selected-filter (build-bulk-filter db)
          filter          (create-filter-for-read-only-resources session selected-filter)]
      {::cimi-api-fx/search
       [:deployment
        {:filter filter :select "id"}
        #(dispatch [::set-deployments-without-edit-rights %])]})))

(reg-event-fx
  ::set-deployments-without-edit-rights
  (fn [{:keys [db]} [_ deployments]]
    (if (instance? js/Error deployments)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info deployments)]
                   {:header  (cond-> (str "failure getting deployments")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      {:db (assoc db ::spec/deployments-without-edit-rights deployments)})))
