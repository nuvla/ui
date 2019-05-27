(ns sixsq.nuvla.ui.deployment-detail.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.client.spec :as client-spec]
    [sixsq.nuvla.ui.deployment-detail.spec :as spec]
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
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ resource-id]]
    (let [filter-depl-params       {:filter  (str "deployment/href='" resource-id "'")
                                    :orderby "name"}
          get-depl-params-callback #(dispatch [::set-deployment-parameters %])]
      {::cimi-api-fx/search [client :deployment-parameter filter-depl-params get-depl-params-callback]})))


(reg-event-fx
  ::get-deployment
  (fn [{{:keys [::client-spec/client
                ::spec/deployment] :as db} :db} [_ resource-id]]
    (when client
      (let [get-depl-callback #(if (instance? js/Error %)
                                 (let [{:keys [status message]} (response/parse-ex-info %)]
                                   (dispatch [::messages-events/add
                                              {:header  (cond-> (str "error getting deployment " resource-id)
                                                                status (str " (" status ")"))
                                               :content message
                                               :type    :error}])
                                   (dispatch [::history-events/navigate "deployment"]))
                                 (dispatch [::set-deployment %]))]
        {:db               (cond-> (assoc db ::spec/loading? true)
                                   (not= (:id deployment) resource-id) (assoc ::spec/deployment nil
                                                                              ::spec/deployment-parameters nil
                                                                              ::spec/events nil
                                                                              ::spec/node-parameters nil)
                                   )
         ::cimi-api-fx/get [client resource-id get-depl-callback]}))))


(reg-event-fx
  ::stop-deployment
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ href]]
    {:db                     db
     ::cimi-api-fx/operation [client href "stop"
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
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ href]]
    (let [filter-str   (str "content/resource/href='" href "'")
          order-by-str "timestamp:desc"
          select-str   "id, content, severity, timestamp, category"
          query-params {:filter  filter-str
                        :orderby order-by-str
                        :select  select-str}]
      {::cimi-api-fx/search [client
                             :event
                             (general-utils/prepare-params query-params)
                             #(dispatch [::set-events (:resources %)])]})))


(reg-event-db
  ::set-jobs
  (fn [db [_ jobs]]
    (assoc db ::spec/jobs jobs)))


(reg-event-fx
  ::get-jobs
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ href]]
    (let [filter-str   (str "target-resource/href='" href "'")
          order-by-str "time-of-status-change:desc,updated:desc"
          select-str   (str "id, action, time-of-status-change, updated, state, "
                            "target-resource, return-code, progress, status-message")
          query-params {:filter  filter-str
                        :orderby order-by-str
                        :select  select-str
                        :last    10}]
      {::cimi-api-fx/search [client
                             :job
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
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ resource-id]]
    (when client
      {::cimi-api-fx/delete [client resource-id
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
                                  (dispatch [:sixsq.nuvla.ui.deployment.events/get-deployments])
                                  (dispatch [::history-events/navigate "deployment"])))]
       })))


(reg-event-fx
  ::edit
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ resource-id data]]
    (when client
      {::cimi-api-fx/edit [client resource-id data
                           #(if (instance? js/Error %)
                              (let [{:keys [status message]} (response/parse-ex-info %)]
                                (dispatch [::messages-events/add
                                           {:header  (cond-> (str "error editing " resource-id)
                                                             status (str " (" status ")"))
                                            :content message
                                            :type    :error}]))
                              (dispatch [::set-deployment %]))]})))


(reg-event-fx
  ::operation
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ resource-id operation]]
    {::cimi-api-fx/operation [client resource-id operation
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
