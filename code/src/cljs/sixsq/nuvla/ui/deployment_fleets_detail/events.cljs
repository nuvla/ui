(ns sixsq.nuvla.ui.deployment-fleets-detail.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.deployments.events :as deployments-events]
    [sixsq.nuvla.ui.deployment-fleets-detail.spec :as spec]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.job.events :as job-events]
    [sixsq.nuvla.ui.plugins.events-table :as events-table]
    [sixsq.nuvla.ui.plugins.tab :as tab]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.session.spec :as session-spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search]
    [sixsq.nuvla.ui.plugins.pagination :as pagination]))

(reg-event-fx
  ::new
  (fn [{db :db}]
    {:db (merge db spec/defaults)}))

(reg-event-fx
  ::set-deployment-fleet
  (fn [{:keys [db]} [_ deployment-fleet]]
    {:db (assoc db ::spec/deployment-fleet-not-found? (nil? deployment-fleet)
                   ::spec/deployment-fleet deployment-fleet
                   ::main-spec/loading? false)}))

(reg-event-fx
  ::operation
  (fn [_ [_ resource-id operation data on-success-fn on-error-fn]]
    {::cimi-api-fx/operation
     [resource-id operation
      #(if (instance? js/Error %)
         (let [{:keys [status message]} (response/parse-ex-info %)]
           (dispatch [::messages-events/add
                      {:header  (cond-> (str "error executing operation " operation)
                                        status (str " (" status ")"))
                       :content message
                       :type    :error}])
           (on-error-fn))
         (do
           (let [{:keys [status message]} (response/parse %)]
             (dispatch [::messages-events/add
                        {:header  (cond-> (str "operation " operation " will be executed soon")
                                          status (str " (" status ")"))
                         :content message
                         :type    :success}]))
           (on-success-fn (:message %))
           (dispatch [::get-nuvlabox resource-id])))
      data]}))

(reg-event-fx
  ::get-deployment-fleet
  (fn [{{:keys [::spec/deployment-fleet] :as db} :db} [_ id]]
    {:db               (cond-> db
                               (not= (:id deployment-fleet) id) (merge spec/defaults))
     ::cimi-api-fx/get [id #(dispatch [::set-deployment-fleet %])
                        :on-error #(dispatch [::set-deployment-fleet nil])]
     :fx               [[:dispatch [::events-table/load-events [::spec/events] id]]
                        [:dispatch [::job-events/get-jobs id]]
                        [:dispatch [::deployments-events/get-deployments
                                    (str "deployment-fleet='" id "'")]]]}))

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
                              (dispatch [::set-deployment-fleet %])))]}))

(reg-event-fx
  ::delete
  (fn [{{:keys [::spec/deployment-fleet]} :db} _]
    (let [id (:id deployment-fleet)]
      {::cimi-api-fx/delete [id #(dispatch [::history-events/navigate "deployment-fleets"])]})))

(reg-event-fx
  ::custom-action
  (fn [_ [_ resource-id operation success-msg]]
    {::cimi-api-fx/operation
     [resource-id operation
      #(if (instance? js/Error %)
         (let [{:keys [status message]} (response/parse-ex-info %)]
           (dispatch [::messages-events/add
                      {:header  (cond-> (str "error on operation " operation " for " resource-id)
                                        status (str " (" status ")"))
                       :content message
                       :type    :error}]))

         (when success-msg
           (dispatch [::messages-events/add
                      {:header  success-msg
                       :content success-msg
                       :type    :success}])
           (dispatch
             [::job-events/wait-job-to-complete
              {:job-id              (:location %)
               :on-complete         (fn [{:keys [status-message return-code]}]
                                      (dispatch [::messages-events/add
                                                 {:header  (str (str/capitalize operation)
                                                                " on " resource-id
                                                                (if (= return-code 0)
                                                                  " completed."
                                                                  " failed!"))
                                                  :content status-message
                                                  :type    (if (= return-code 0)
                                                             :success
                                                             :error)}]))
               :refresh-interval-ms 5000}])))]}))

(reg-event-db
  ::set-apps
  (fn [db [_ apps]]
    (assoc db ::spec/apps apps
              ::spec/apps-loading? false)))

(reg-event-fx
  ::search-apps
  (fn [{{:keys [::spec/tab-new-apps
                ::session-spec/session] :as db} :db}]
    {:db (assoc db ::spec/apps-loading? true)
     ::cimi-api-fx/search
     [:module (->>
                {:select "id, name, description, parent-path"
                 :order  "parent-path:asc"
                 :filter (general-utils/join-and
                           (full-text-search/filter-text db [::spec/apps-search])
                           (case (::tab/active-tab tab-new-apps)
                             :my-apps (str "acl/owners='" (:active-claim session) "'")
                             :app-store "published=true"
                             nil)
                           "subtype!='project'")}
                (pagination/first-last-params db [::spec/apps-pagination]))
      #(dispatch [::set-apps %])]}))

(reg-event-db
  ::toggle-select-app
  (fn [{:keys [::spec/apps-selected] :as db} [_ id]]
    (let [op (if (contains? apps-selected id) disj conj)]
      (update db ::spec/apps-selected op id))))

(reg-event-db
  ::toggle-select-cred
  (fn [{:keys [::spec/creds-selected] :as db} [_ id cred-ids]]
    (let [op (if (contains? creds-selected id) disj conj)]
      (-> db
          (assoc ::spec/creds-selected (apply disj creds-selected cred-ids))
          (update ::spec/creds-selected op id)))))

(reg-event-fx
  ::set-credentials
  (fn [{db :db} [_ response]]
    {:db (assoc db ::spec/targets-loading? false
                   ::spec/credentials response)}))

(reg-event-fx
  ::search-credentials
  (fn [_ [_ filter-str]]
    {::cimi-api-fx/search
     [:credential {:last   10000
                   :select "id, name, description, parent"
                   :filter filter-str}
      #(dispatch [::set-credentials %])]}))

(reg-event-fx
  ::set-infrastructures
  (fn [{db :db} [_ {:keys [resources] :as response}]]
    (if (seq resources)
      (let [filter-str (->> resources
                            (map #(str "parent='" (:id %) "'"))
                            (apply general-utils/join-or)
                            (general-utils/join-and
                              (general-utils/join-or
                                "subtype='infrastructure-service-swarm'"
                                "subtype='infrastructure-service-kubernetes'"
                                )))]
        {:db (assoc db ::spec/infrastructures response)
         :fx [[:dispatch [::search-credentials filter-str]]]})
      {:db (assoc db ::spec/targets-loading? false
                     ::spec/infrastructures response
                     ::spec/credentials nil)})))

(reg-event-fx
  ::search-infrastructures
  (fn [_ [_ filter-str]]
    {::cimi-api-fx/search
     [:infrastructure-service
      {:last   10000
       :select "id, name, description, subtype, parent"
       :filter filter-str}
      #(dispatch [::set-infrastructures %])]}))

(reg-event-fx
  ::search-clouds
  (fn [{db :db}]
    {:db (assoc db ::spec/targets-loading? true)
     ::cimi-api-fx/search
     [:infrastructure-service
      (->> {:select "id, name, description, subtype, parent"
            :filter (general-utils/join-and
                      "tags!='nuvlabox=True'"
                      (general-utils/join-or
                        "subtype='swarm'"
                        "subtype='kubernetes'")
                      (full-text-search/filter-text
                        db [::spec/clouds-search]))}
           (pagination/first-last-params db [::spec/clouds-pagination]))
      #(dispatch [::set-infrastructures %])]}))

(reg-event-fx
  ::set-edges
  (fn [{db :db} [_ {:keys [resources] :as response}]]
    (if (seq resources)
      (let [filter-str (->> resources
                            (map #(str "parent='"
                                       (:infrastructure-service-group %)
                                       "'"))
                            (apply general-utils/join-or)
                            (general-utils/join-and
                              (general-utils/join-or
                                "subtype='swarm'"
                                "subtype='kubernetes'")))]
        {:db (assoc db ::spec/edges response)
         :fx [[:dispatch [::search-infrastructures filter-str]]]})
      {:db (assoc db ::spec/targets-loading? false
                     ::spec/edges response
                     ::spec/infrastrutures nil
                     ::spec/credentials nil)})))

(reg-event-fx
  ::search-edges
  (fn [{db :db}]
    {:db (assoc db ::spec/targets-loading? true)
     ::cimi-api-fx/search
     [:nuvlabox
      (->> {:select "id, name, description, infrastructure-service-group"
            :filter (general-utils/join-and
                      (full-text-search/filter-text db [::spec/edges-search])
                      "state='COMMISSIONED'"
                      "infrastructure-service-group!=null")}
           (pagination/first-last-params db [::spec/edges-pagination]))
      #(dispatch [::set-edges %])]}))