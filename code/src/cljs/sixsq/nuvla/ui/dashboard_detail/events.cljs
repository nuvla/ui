(ns sixsq.nuvla.ui.dashboard-detail.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.dashboard-detail.spec :as spec]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))


(reg-event-db
  ::set-deployment
  (fn [db [_ resource]]
    (assoc db ::spec/loading? false
              ::spec/deployment resource)))


(reg-event-db
  ::set-deployment-parameters
  (fn [db [_ resources]]
    (assoc db ::spec/deployment-parameters
              (into {} (map (juxt :name identity) (get resources :resources []))))))


(reg-event-fx
  ::get-deployment-parameters
  (fn [_ [_ resource-id]]
    (let [filter-depl-params       {:filter  (str "deployment/href='" resource-id "'")
                                    :orderby "name"}
          get-depl-params-callback #(dispatch [::set-deployment-parameters %])]
      {::cimi-api-fx/search [:deployment-parameter filter-depl-params get-depl-params-callback]})))


(reg-event-fx
  ::get-deployment
  (fn [{{:keys [::spec/deployment] :as db} :db} [_ id]]
    (let [get-depl-callback #(dispatch [::set-deployment %])]
      {:db               (cond-> (assoc db ::spec/loading? true)
                                 (not= (:id deployment) id) (merge db spec/defaults))
       ::cimi-api-fx/get [id get-depl-callback]})))


(reg-event-fx
  ::stop-deployment
  (fn [{:keys [db]} [_ href]]
    {:db                     db
     ::cimi-api-fx/operation [href "stop"
                              #(if (instance? js/Error %)
                                 (let [{:keys [status message]} (response/parse-ex-info %)]
                                   (dispatch [::messages-events/add
                                              {:header  (cond-> (str "error stopping deployment " href)
                                                                status (str " (" status ")"))
                                               :content message
                                               :type    :error}]))
                                 (dispatch [::get-deployment href]))]}))


(reg-event-db
  ::set-events
  (fn [db [_ events]]
    (assoc db ::spec/events events)))


(reg-event-fx
  ::get-events
  (fn [_ [_ href]]
    (let [filter-str   (str "content/resource/href='" href "'")
          order-by-str "timestamp:desc"
          select-str   "id, content, severity, timestamp, category"
          query-params {:filter  filter-str
                        :orderby order-by-str
                        :select  select-str}]
      {::cimi-api-fx/search [:event
                             (general-utils/prepare-params query-params)
                             #(dispatch [::set-events (:resources %)])]})))


(reg-event-db
  ::set-jobs
  (fn [db [_ jobs]]
    (assoc db ::spec/jobs jobs)))


(reg-event-fx
  ::get-jobs
  (fn [{:keys [db]} [_ href]]
    (let [filter-str   (str "target-resource/href='" href "'")
          order-by-str "time-of-status-change:desc,updated:desc"
          select-str   (str "id, action, time-of-status-change, updated, state, "
                            "target-resource, return-code, progress, status-message")
          query-params {:filter  filter-str
                        :orderby order-by-str
                        :select  select-str
                        :last    10}]
      {::cimi-api-fx/search [:job
                             (general-utils/prepare-params query-params)
                             #(dispatch [::set-jobs (:resources %)])]})))


(reg-event-db
  ::set-node-parameters
  (fn [db [_ node-parameters]]
    (assoc db ::spec/node-parameters node-parameters)))


;;
;; events used for cimi operations
;;
;; FIXME: These have been copied from the CIMI detail page.  Refactor to reduce duplication.
;;

(reg-event-fx
  ::delete
  (fn [_ [_ resource-id]]
    {::cimi-api-fx/delete [resource-id
                           #(if (instance? js/Error %)
                              (let [{:keys [status message]} (response/parse-ex-info %)]
                                (dispatch [::messages-events/add
                                           {:header  (cond-> (str "error deleting " resource-id)
                                                             status (str " (" status ")"))
                                            :content message
                                            :type    :error}]))
                              (let [{:keys [status message]} (response/parse %)]
                                (dispatch [::messages-events/add
                                           {:header  (cond-> (str "deleted " resource-id)
                                                             status (str " (" status ")"))
                                            :content message
                                            :type    :success}])
                                (dispatch [:sixsq.nuvla.ui.dashboard.events/get-deployments])
                                (dispatch [::history-events/navigate "dashboard"])))]}))


(reg-event-fx
  ::edit
  (fn [_ [_ resource-id data]]
    {::cimi-api-fx/edit [resource-id data
                         #(if (instance? js/Error %)
                            (let [{:keys [status message]} (response/parse-ex-info %)]
                              (dispatch [::messages-events/add
                                         {:header  (cond-> (str "error editing " resource-id)
                                                           status (str " (" status ")"))
                                          :content message
                                          :type    :error}]))
                            (dispatch [::set-deployment %]))]}))


(reg-event-fx
  ::operation
  (fn [_ [_ resource-id operation]]
    {::cimi-api-fx/operation [resource-id operation
                              #(let [op (second (re-matches #"(?:.*/)?(.*)" operation))]
                                 (if (instance? js/Error %)
                                   (let [{:keys [status message]} (response/parse-ex-info %)]
                                     (dispatch [::messages-events/add
                                                {:header  (cond-> (str "error executing operation " op)
                                                                  status (str " (" status ")"))
                                                 :content message
                                                 :type    :error}]))
                                   (let [{:keys [status message]} (response/parse %)]
                                     (dispatch [::messages-events/add
                                                {:header  (cond-> (str "success executing operation " op)
                                                                  status (str " (" status ")"))
                                                 :content message
                                                 :type    :success}]))))]}))
