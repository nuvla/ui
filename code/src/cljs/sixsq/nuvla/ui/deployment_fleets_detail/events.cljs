(ns sixsq.nuvla.ui.deployment-fleets-detail.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.deployments.events :as deployments-events]
    [sixsq.nuvla.ui.edges-detail.spec :as edges-detail-spec]
    [sixsq.nuvla.ui.deployment-fleets-detail.spec :as spec]
    [sixsq.nuvla.ui.edges.utils :as edges-utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.job.events :as job-events]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-event-fx
  ::new
  (fn [{db :db}]
    {:db (merge db spec/defaults)
     :fx [[:dispatch [::search-apps]]
          [:dispatch [::search-creds]]]}))


(reg-event-db
  ::set-nuvlabox-status
  (fn [db [_ nuvlabox-status]]
    (assoc db ::edges-detail-spec/nuvlabox-status nuvlabox-status)))


(reg-event-db
  ::set-nuvlabox-vulns
  (fn [db [_ nuvlabox-vulns]]
    (assoc db ::edges-detail-spec/nuvlabox-vulns
              {:summary (:summary nuvlabox-vulns)
               :items   (into []
                              (map
                                (fn [{:keys [vulnerability-score] :as item}]
                                  (if vulnerability-score
                                    (cond
                                      (>= vulnerability-score 9.0) (assoc item
                                                                     :severity "CRITICAL"
                                                                     :color edges-utils/vuln-critical-color)
                                      (and (< vulnerability-score 9.0)
                                           (>= vulnerability-score 7.0)) (assoc item
                                                                           :severity "HIGH"
                                                                           :color edges-utils/vuln-high-color)
                                      (and (< vulnerability-score 7.0)
                                           (>= vulnerability-score 4.0)) (assoc item
                                                                           :severity "MEDIUM"
                                                                           :color edges-utils/vuln-medium-color)
                                      (< vulnerability-score 4.0) (assoc item
                                                                    :severity "LOW"
                                                                    :color edges-utils/vuln-low-color))
                                    (assoc item :severity "UNKNOWN" :color edges-utils/vuln-unknown-color)))
                                (:items nuvlabox-vulns)))})))


(reg-event-db
  ::set-nuvlabox-associated-ssh-keys
  (fn [db [_ ssh-keys]]
    (assoc db ::edges-detail-spec/nuvlabox-associated-ssh-keys ssh-keys)))


(reg-event-db
  ::set-deployment-fleet-events
  (fn [db [_ events]]
    (assoc db ::spec/deployment-fleet-events events)))


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
  ::set-page
  (fn [{db :db} [_ page]]
    {:db       (assoc db ::edges-detail-spec/page page)
     :dispatch [::get-deployment-fleet-events]}))

;; FIXME duplicated in multiple places, build an event reusable component
(reg-event-fx
  ::get-deployment-fleet-events
  (fn [{{:keys [::spec/page
                ::spec/elements-per-page]} :db} [_ href]]
    (let [filter-str   (str "content/resource/href='" href "'")
          order-by-str "created:desc"
          select-str   "id, content, severity, timestamp, category"
          first        (inc (* (dec page) elements-per-page))
          last         (* page elements-per-page)
          query-params {:filter  filter-str
                        :orderby order-by-str
                        :select  select-str
                        :first   first
                        :last    last}]
      {::cimi-api-fx/search [:event
                             (general-utils/prepare-params query-params)
                             #(dispatch [::set-deployment-fleet-events %])]})))


(reg-event-fx
  ::get-deployment-fleet
  (fn [{{:keys [::spec/deployment-fleet] :as db} :db} [_ id]]
    {:db               (cond-> db
                               (not= (:id deployment-fleet) id) (merge spec/defaults))
     ::cimi-api-fx/get [id #(dispatch [::set-deployment-fleet %])
                        :on-error #(dispatch [::set-deployment-fleet nil])]
     :fx               [[:dispatch [::get-deployment-fleet-events id]]
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
  (fn [{{:keys [::edges-detail-spec/nuvlabox]} :db} _]
    (let [nuvlabox-id (:id ::edges-detail-spec/nuvlabox)]
      {::cimi-api-fx/delete [nuvlabox-id #(dispatch [::history-events/navigate "edges"])]})))


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
  ::set-active-tab
  (fn [db [_ key]]
    (assoc db ::spec/active-tab key)))


(reg-event-db
  ::set-apps
  (fn [db [_ apps]]
    (assoc db ::spec/apps (:resources apps))))


(reg-event-fx
  ::set-apps-fulltext-search
  (fn [{db :db} [_ search]]
    {:db (assoc db ::spec/apps-fulltext-search search)
     :fx [[:dispatch [::search-apps]]]}))

(reg-event-fx
  ::search-apps
  (fn [{{:keys [::spec/apps-fulltext-search]} :db}]
    {::cimi-api-fx/search
     [:module {:last   10000
               :select "id, name, description, parent-path"
               :filter (general-utils/join-and
                         (general-utils/fulltext-query-string apps-fulltext-search)
                         "subtype!='project'")}
      #(dispatch [::set-apps %])]}))


(reg-event-db
  ::toggle-select-app
  (fn [{:keys [::spec/apps-selected] :as db} [_ id]]
    (let [op (if (contains? apps-selected id) disj conj)]
      (update db ::spec/apps-selected op id))))


(reg-event-fx
  ::set-creds-fulltext-search
  (fn [{db :db} [_ search]]
    {:db (assoc db ::spec/creds-fulltext-search search)
     :fx [[:dispatch [::search-creds]]]}))

(reg-event-fx
  ::set-creds
  (fn [{db :db} [_ {:keys [resources] :as infras} creds-by-parent]]
    (if (instance? js/Error infras)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info infras)]
                   {:header  (cond-> (str "failure getting infrastructure-services")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      (let [infra-creds (->> resources
                             (map #(-> %
                                       (select-keys [:id :name :description :tags :subtype])
                                       (assoc :credentials
                                              (->> (get creds-by-parent (:id %))
                                                   (map (fn [cred]
                                                          (select-keys cred [:id :name :description :tags])))
                                                   )))))]

        {:db (assoc db ::spec/creds infra-creds)}))))

(reg-event-fx
  ::search-infras
  (fn [{db :db} [_ {:keys [resources] :as creds}]]
    (if (instance? js/Error creds)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info creds)]
                   {:header  (cond-> (str "failure getting credentials")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      (let [creds-by-parent (group-by :parent resources)]
        (if (pos? (count resources))
          {::cimi-api-fx/search
           [:infrastructure-service {:last   10000
                                     :select "id, name, description, tags, subtype"
                                     :order  "name:asc, id:asc"
                                     :filter (->> creds-by-parent
                                                  keys
                                                  (map #(str "id='" % "'"))
                                                  (apply general-utils/join-or))}
            #(dispatch [::set-creds % creds-by-parent])]}
          {:db (assoc db ::spec/creds nil)})))))

(reg-event-fx
  ::search-creds
  (fn [{{:keys [::spec/creds-fulltext-search]} :db}]
    {::cimi-api-fx/search
     [:credential {:last   10000
                   :select "id, name, description, tags, parent"
                   :filter (general-utils/join-and
                             (general-utils/fulltext-query-string creds-fulltext-search)
                             (general-utils/join-or
                               "subtype='infrastructure-service-swarm'"
                               "subtype='infrastructure-service-kubernetes'"))}
      #(dispatch [::search-infras %])]}))


(reg-event-db
  ::toggle-select-cred
  (fn [{:keys [::spec/creds-selected] :as db} [_ id cred-ids]]
    (let [op (if (contains? creds-selected id) disj conj)]
      (-> db
          (assoc ::spec/creds-selected (apply disj creds-selected cred-ids))
          (update ::spec/creds-selected op id)))))
