(ns sixsq.nuvla.ui.dashboard-detail.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.dashboard-detail.spec :as spec]
    [sixsq.nuvla.ui.dashboard.events :as dashboard-events]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [sixsq.nuvla.ui.utils.time :as time]
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
    (let [query-params             {:filter  (str "parent='" resource-id "'")
                                    :orderby "name"
                                    :last    10000}
          get-depl-params-callback #(dispatch [::set-deployment-parameters %])]
      {::cimi-api-fx/search [:deployment-parameter query-params get-depl-params-callback]})))


(reg-event-fx
  ::get-deployment
  (fn [{{:keys [::spec/deployment] :as db} :db} [_ id]]
    (let [different-deployment? (not= (:id deployment) id)]
      (cond-> {:dispatch-n       [[::get-deployment-parameters id]
                                  [::get-events id]
                                  [::get-jobs id]]
               ::cimi-api-fx/get [id #(dispatch [::set-deployment %])]}
              different-deployment? (assoc :db (merge db spec/defaults))))))


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
                                 (do
                                   (dispatch [::get-deployment href])
                                   (dispatch [::dashboard-events/get-deployments])))]}))


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
  ::set-job-page
  (fn [{:keys [::spec/deployment] :as db} [_ page]]
    (dispatch [::get-jobs (:id deployment)])
    (assoc db ::spec/job-page page)))


(reg-event-db
  ::set-jobs
  (fn [db [_ jobs]]
    (assoc db ::spec/jobs jobs)))


(reg-event-fx
  ::get-jobs
  (fn [{{:keys [::spec/job-page
                ::spec/jobs-per-page]} :db} [_ href]]
    (let [filter-str   (str "target-resource/href='" href "'")
          select-str   (str "id, action, time-of-status-change, updated, state, "
                            "target-resource, return-code, progress, status-message")
          query-params {:filter  filter-str
                        :select  select-str
                        :first   (inc (* (dec job-page) jobs-per-page))
                        :last    (* job-page jobs-per-page)
                        :orderby "created:desc"}]
      {::cimi-api-fx/search [:job
                             (general-utils/prepare-params query-params)
                             #(dispatch [::set-jobs %])]})))


(reg-event-db
  ::set-node-parameters
  (fn [db [_ node-parameters]]
    (assoc db ::spec/node-parameters node-parameters)))

(reg-event-fx
  ::fetch-deployment-log
  (fn [{{:keys [::spec/deployment-log-id]} :db} _]
    {::cimi-api-fx/operation [deployment-log-id "fetch" #()]}))


(reg-event-fx
  ::set-deployment-log
  (fn [{{:keys [::spec/deployment-log] :as db} :db} [_ new-deployment-log]]
    (let [concat-log (-> (:log deployment-log)
                         (concat (:log new-deployment-log))
                         (dedupe))]
      {:db       (assoc db ::spec/deployment-log
                           (assoc new-deployment-log :log concat-log))
       :dispatch [::fetch-deployment-log]})))


(reg-event-db
  ::clear-deployment-log
  (fn [{:keys [::spec/deployment-log] :as db} _]
    (assoc-in db [::spec/deployment-log :log] [])))


(reg-event-fx
  ::set-deployment-log-id
  (fn [{{:keys [::spec/deployment-log-play?] :as db} :db} [_ deployment-log-id]]
    {:db       (assoc db ::spec/deployment-log-id deployment-log-id)
     :dispatch [::set-deployment-log-play? deployment-log-play?]}))


(reg-event-fx
  ::create-log
  (fn [{{:keys [::spec/deployment
                ::spec/deployment-log-id
                ::spec/deployment-log-service
                ::spec/deployment-log-since] :as db} :db} _]
    (cond-> {:db                     (assoc db ::spec/deployment-log-id nil
                                               ::spec/deployment-log nil)
             ::cimi-api-fx/operation [(:id deployment) "create-log"
                                      #(dispatch [::set-deployment-log-id (:resource-id %)])
                                      {:service deployment-log-service
                                       :since   (time/time->utc-str deployment-log-since)}]}
            deployment-log-id (assoc ::cimi-api-fx/delete [deployment-log-id #()]))))


(reg-event-fx
  ::get-deployment-log
  (fn [{{:keys [::spec/deployment-log-id]} :db} _]
    (when deployment-log-id
      {::cimi-api-fx/get [deployment-log-id #(dispatch [::set-deployment-log %])]})))

(reg-event-fx
  ::set-deployment-log-service
  (fn [{db :db} [_ service]]
    {:db       (assoc db ::spec/deployment-log-service service)
     :dispatch [::create-log]}))


(reg-event-fx
  ::set-deployment-log-since
  (fn [{db :db} [_ since]]
    {:db       (assoc db ::spec/deployment-log-since since)
     :dispatch [::create-log]}))


(reg-event-fx
  ::set-deployment-log-play?
  (fn [{db :db} [_ play?]]
    (cond-> {:db       (assoc db ::spec/deployment-log-play? play?)
             :dispatch (if play?
                         [::fetch-deployment-log]
                         [::main-events/action-interval-delete
                          :dashboard-detail-get-deployment-log])}
            play? (assoc :dispatch-later
                         [{:ms       5000
                           :dispatch [::main-events/action-interval-start
                                      {:id        :dashboard-detail-get-deployment-log
                                       :frequency 10000
                                       :event     [::get-deployment-log]}]}]))))

;;
;; events used for cimi operations
;;
;; FIXME: These have been copied from the CIMI detail page.  Refactor to reduce duplication.
;;

(reg-event-fx
  ::delete
  (fn [_ [_ resource-id]]
    {::cimi-api-fx/delete [resource-id
                           #(let [{:keys [status message]} (response/parse %)]
                              (dispatch [::messages-events/add
                                         {:header  (cond-> (str "deleted " resource-id)
                                                           status (str " (" status ")"))
                                          :content message
                                          :type    :success}])
                              (dispatch [:sixsq.nuvla.ui.dashboard.events/get-deployments])
                              (dispatch [::history-events/navigate "dashboard"]))]}))


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
