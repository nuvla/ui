(ns sixsq.slipstream.webui.deployment-detail.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.cimi-api.effects :as cimi-api-fx]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.deployment-detail.spec :as spec]
    [sixsq.slipstream.webui.history.events :as history-events]
    [sixsq.slipstream.webui.main.effects :as main-fx]
    [sixsq.slipstream.webui.messages.events :as messages-events]
    [sixsq.slipstream.webui.utils.general :as general-utils]
    [sixsq.slipstream.webui.utils.general :as general]
    [sixsq.slipstream.webui.utils.response :as response]
    [taoensso.timbre :as log]))


(reg-event-db
  ::set-runUUID
  (fn [{:keys [::spec/runUUID] :as db} [_ uuid]]
    (assoc db ::spec/runUUID uuid)))


(reg-event-db
  ::set-reports
  (fn [db [_ reports]]
    (assoc db ::spec/reports reports)))


(reg-event-fx
  ::get-reports
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ href]]
    (let [filter-str (str "objectType='report' and runUUID='" href "'")
          order-by-str "created:desc, component"
          select-str "id, state, created, component"
          query-params {:$filter  filter-str
                        :$orderby order-by-str
                        :$select  select-str}]
      {::cimi-api-fx/search [client
                             "externalObjects"
                             (general-utils/prepare-params query-params)
                             #(dispatch [::set-reports %])]})))


(reg-event-db
  ::set-deployment
  (fn [db [_ resource]]
    (assoc db ::spec/loading? false
              ::spec/deployment resource)))


(reg-event-db
  ::set-deployment-parameters
  (fn [db [_ resources]]
    (assoc db ::spec/global-deployment-parameters
              (into {} (map (juxt :name identity) (get resources :deploymentParameters []))))))


(reg-event-fx
  ::get-global-deployment-parameters
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ resource-id]]
    (let [filter-depl-params {:$filter  (str "deployment/href='" resource-id "' and nodeID=null")
                              :$orderby "name"}
          get-depl-params-callback #(dispatch [::set-deployment-parameters %])]
      {::cimi-api-fx/search [client "deploymentParameters" filter-depl-params get-depl-params-callback]})))


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
                                                                              ::spec/global-deployment-parameters nil
                                                                              ::spec/events nil
                                                                              ::spec/reports nil
                                                                              ::spec/node-parameters-modal nil
                                                                              ::spec/node-parameters nil
                                                                              ::spec/summary-nodes-parameters nil)
                                   )
         ::cimi-api-fx/get [client resource-id get-depl-callback]}))))


(reg-event-fx
  ::stop-deployment
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ href]]
    {:db                     db
     ::cimi-api-fx/operation [client href "http://schemas.dmtf.org/cimi/2/action/stop"
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
    (assoc db ::spec/events events
              ::spec/force-refresh-events-steps (random-uuid))))


(reg-event-fx
  ::get-events
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ href]]
    (let [filter-str (str "content/resource/href='" href "'")
          order-by-str "timestamp:desc"
          select-str "id, content, severity, timestamp, type"
          query-params {:$filter  filter-str
                        :$orderby order-by-str
                        :$select  select-str}]
      {::cimi-api-fx/search [client
                             "events"
                             (general-utils/prepare-params query-params)
                             #(dispatch [::set-events (:events %)])]})))


(reg-event-db
  ::set-jobs
  (fn [db [_ jobs]]
    (assoc db ::spec/jobs jobs
              ::spec/force-refresh-events-steps (random-uuid))))


(reg-event-fx
  ::get-jobs
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ href]]
    (let [filter-str (str "targetResource/href='" href "'")
          order-by-str "timeOfStatusChange:desc"
          select-str "id, timeOfStatusChange, state, targetResource, returnCode, progress, statusMessage"
          query-params {:$filter  filter-str
                        :$orderby order-by-str
                        :$select  select-str}]
      {::cimi-api-fx/search [client
                             "jobs"
                             (general-utils/prepare-params query-params)
                             #(dispatch [::set-jobs (:jobs %)])]})))


(reg-event-db
  ::set-node-parameters
  (fn [db [_ node-parameters]]
    (assoc db ::spec/node-parameters node-parameters)))


(defn get-node-parameters
  [client deployment node-name]
  (let [filter-str (str "deployment/href='" (:id deployment) "' and nodeID='" node-name "'")
        select-str "id, created, updated, name, description, value"
        query-params {:$filter filter-str
                      :$select select-str}]
    {::cimi-api-fx/search [client
                           "deploymentParameters"
                           (general-utils/prepare-params query-params)
                           #(dispatch [::set-node-parameters (:deploymentParameters %)])]}))


(reg-event-db
  ::show-node-parameters-modal
  (fn [{:keys [::client-spec/client
               ::spec/deployment] :as db} [_ node-name]]
    (assoc db ::spec/node-parameters-modal node-name
              ::spec/node-parameters nil)))


(reg-event-fx
  ::get-node-parameters
  (fn [{{:keys [::client-spec/client
                ::spec/deployment
                ::spec/node-parameters-modal] :as db} :db} _]
    (when (boolean node-parameters-modal)
      (let [filter-str (str "deployment/href='" (:id deployment) "' and nodeID='" node-parameters-modal "'")
            select-str "id, created, updated, name, description, value"
            query-params {:$filter  filter-str
                          :$select  select-str
                          :$orderby "name"}]
        {::cimi-api-fx/search [client
                               "deploymentParameters"
                               (general-utils/prepare-params query-params)
                               #(dispatch [::set-node-parameters (:deploymentParameters %)])]}))))


(reg-event-db
  ::close-node-parameters-modal
  (fn [db _]
    (assoc db ::spec/node-parameters-modal nil)))


(reg-event-db
  ::set-summary-nodes-parameters
  (fn [db [_ summary-nodes-parameters]]
    (assoc db ::spec/summary-nodes-parameters (group-by :nodeID summary-nodes-parameters))))


(def summary-param-names #{"statecustom"
                           "url.service"
                           "url.ssh"
                           "password.ssh"
                           "complete"})

(reg-event-fx
  ::get-summary-nodes-parameters
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ resource-id nodes]]
    (when (some? nodes)
      (let [nodes-filter (str/join " or " (map #(str "nodeID='" % "'") nodes))
            names-filter (str/join " or " (map #(str "name='" % "'") summary-param-names))
            filter-str (str/join " and " [(str "deployment/href='" resource-id "'")
                                          (str "(" nodes-filter ")")
                                          (str "(" names-filter ")")])
            select-str "nodeID, name, value"
            query-params {:$filter  filter-str
                          :$select  select-str
                          :$orderby "name"}]
        {::cimi-api-fx/search [client
                               "deploymentParameters"
                               (general-utils/prepare-params query-params)
                               #(dispatch [::set-summary-nodes-parameters (:deploymentParameters %)])]}))))


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
