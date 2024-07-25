(ns sixsq.nuvla.ui.pages.deployments-detail.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.job.events :as job-events]
            [sixsq.nuvla.ui.common-components.messages.events :as messages-events]
            [sixsq.nuvla.ui.common-components.plugins.audit-log :as audit-log-plugin]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.pages.credentials.events :as creds-events]
            [sixsq.nuvla.ui.pages.deployments-detail.spec :as spec]
            [sixsq.nuvla.ui.pages.deployments.events :as deployments-events]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.utils.response :as response]))

(reg-event-db
  ::set-module-versions
  (fn [db [_ module]]
    (assoc db ::spec/module-versions (:versions module))))

(reg-event-db
  ::set-nuvlabox
  (fn [db [_ resource]]
    (assoc db ::spec/nuvlabox resource)))

(reg-event-fx
  ::get-nuvlabox
  (fn [{:keys [db]} [_ id]]
    (if id
      {::cimi-api-fx/get [id #(dispatch [::set-nuvlabox %])]}
      {:db (assoc db ::spec/nuvlabox nil)})))

(reg-event-fx
  ::set-deployment
  (fn [{{:keys [::spec/module-versions] :as db} :db}
       [_ {:keys [nuvlabox module] :as resource}]]
    (let [module-href (:href module)]
      (cond-> {:db (assoc db ::spec/not-found? (nil? resource)
                             ::main-spec/loading? false
                             ::spec/loading? false
                             ::spec/deployment resource)
               :fx [[:dispatch [::get-nuvlabox nuvlabox]]]}
              (and (not module-versions)
                   module-href) (assoc ::cimi-api-fx/get
                                       [module-href #(dispatch [::set-module-versions %])])))))

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
      (cond-> {:db               (assoc db ::spec/loading? true)
               :fx               [[:dispatch [::get-deployment-parameters id]]
                                  [:dispatch [::audit-log-plugin/load-events
                                              [::spec/events] {:href id} true]]
                                  [:dispatch [::job-events/get-jobs id]]]
               ::cimi-api-fx/get [id #(dispatch [::set-deployment %])
                                  :on-error #(dispatch [::set-deployment nil])]}
              different-deployment? (assoc :db (merge db spec/defaults))))))

(reg-event-fx
  ::stop-deployment
  (fn [_ [_ href]]
    (let [on-success #(do
                        (dispatch [::get-deployment href])
                        (dispatch [::deployments-events/get-deployments]))]
      {::cimi-api-fx/operation [href "stop" on-success]})))

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
                           #(let [{:keys [status message]} (response/parse %)]
                              (dispatch [::messages-events/add
                                         {:header  (cond-> (str "deleted " resource-id)
                                                           status (str " (" status ")"))
                                          :content message
                                          :type    :success}])
                              (dispatch [::deployments-events/get-deployments])
                              (dispatch [::routing-events/navigate routes/deployments]))]}))

(reg-event-fx
  ::edit
  (fn [_ [_ resource-id data success-msg]]
    {::cimi-api-fx/edit [resource-id data
                         #(if (instance? js/Error %)
                            (let [{:keys [status message]} (response/parse-ex-info %)]
                              (dispatch [::messages-events/add
                                         {:header  (cond-> (str "error editing " resource-id)
                                                           status (str " (" status ")"))
                                          :content message
                                          :type    :error}]))
                            (do
                              (when success-msg
                                (dispatch [::messages-events/add
                                           {:header  success-msg
                                            :content success-msg
                                            :type    :success}]))
                              (dispatch [::set-deployment %])))]}))

(reg-event-fx
  ::detach
  (fn [_ [_ href]]
    (let [on-success #(dispatch [::set-deployment %])]
      {::cimi-api-fx/operation [href "detach" on-success]})))

(reg-event-fx
  ::check-credential
  (fn [_ [_ credential-href]]
    {::cimi-api-fx/get [credential-href
                        #(dispatch [::creds-events/check-credential % 1])]}))

(reg-event-db
  ::not-found?
  (fn [db [_ e]]
    (let [{:keys [_status _message]} (response/parse-ex-info e)]
      (assoc db ::spec/not-found? (instance? js/Error e)))))
