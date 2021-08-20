(ns sixsq.nuvla.ui.edge-detail.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.deployment.events :as deployment-events]
    [sixsq.nuvla.ui.edge-detail.spec :as spec]
    [sixsq.nuvla.ui.edge.utils :as edge-utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.job.events :as job-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-event-db
  ::set-nuvlabox-status
  (fn [db [_ nuvlabox-status]]
    (assoc db ::spec/nuvlabox-status nuvlabox-status)))


(reg-event-db
  ::set-loading
  (fn [db _]
    (assoc db ::spec/loading? true)))


(reg-event-db
  ::set-nuvlabox-vulns
  (fn [db [_ nuvlabox-vulns]]
    (assoc db ::spec/nuvlabox-vulns
              {:summary (:summary nuvlabox-vulns)
               :items   (into []
                              (map
                                (fn [{:keys [vulnerability-score] :as item}]
                                  (if vulnerability-score
                                    (cond
                                      (>= vulnerability-score 9.0) (assoc item
                                                                     :severity "CRITICAL"
                                                                     :color edge-utils/vuln-critical-color)
                                      (and (< vulnerability-score 9.0)
                                           (>= vulnerability-score 7.0)) (assoc item
                                                                           :severity "HIGH"
                                                                           :color edge-utils/vuln-high-color)
                                      (and (< vulnerability-score 7.0)
                                           (>= vulnerability-score 4.0)) (assoc item
                                                                           :severity "MEDIUM"
                                                                           :color edge-utils/vuln-medium-color)
                                      (< vulnerability-score 4.0) (assoc item
                                                                    :severity "LOW"
                                                                    :color edge-utils/vuln-low-color))
                                    (assoc item :severity "UNKNOWN" :color edge-utils/vuln-unknown-color)))
                                (:items nuvlabox-vulns)))})))


(reg-event-db
  ::set-nuvlabox-associated-ssh-keys
  (fn [db [_ ssh-keys]]
    (assoc db ::spec/nuvlabox-associated-ssh-keys ssh-keys)))


(reg-event-db
  ::set-matching-vulns-from-db
  (fn [db [_ vulns]]
    (assoc db ::spec/matching-vulns-from-db (zipmap (map :name vulns) vulns))))


(reg-event-db
  ::set-nuvlabox-peripherals
  (fn [db [_ nuvlabox-peripherals]]
    (assoc db ::spec/nuvlabox-peripherals (->> (get nuvlabox-peripherals :resources [])
                                               (map (juxt :id identity))
                                               (into {})))))


(reg-event-db
  ::set-nuvlabox-events
  (fn [db [_ nuvlabox-events]]
    (assoc db ::spec/nuvlabox-events nuvlabox-events)))


(reg-event-db
  ::set-vuln-severity-selector
  (fn [db [_ vuln-severity]]
    (assoc db ::spec/vuln-severity-selector vuln-severity)))


(reg-event-fx
  ::set-nuvlabox
  (fn [{:keys [db]} [_ {nb-status-id :nuvlabox-status :as nuvlabox}]]
    {:db               (assoc db ::spec/nuvlabox-not-found? (nil? nuvlabox)
                                 ::spec/nuvlabox nuvlabox
                                 ::spec/loading? false)
     ::cimi-api-fx/get [nb-status-id #(do
                                        (dispatch [::set-nuvlabox-status %])
                                        (dispatch [::set-nuvlabox-vulns (:vulnerabilities %)])
                                        (dispatch [::get-matching-vulns-from-db (map :vulnerability-id
                                                                                     (:items (:vulnerabilities %)))]))
                        :on-error #(do
                                     (dispatch [::set-nuvlabox-status nil])
                                     (dispatch [::set-nuvlabox-vulns nil]))]}))


(reg-event-fx
  ::get-nuvlabox-associated-ssh-keys
  (fn [_ [_ ssh-keys-ids]]
    (if (empty? ssh-keys-ids)
      (dispatch [::set-nuvlabox-associated-ssh-keys {}])
      {::cimi-api-fx/search
       [:credential
        {:filter (->> ssh-keys-ids
                      (map #(str "id='" % "'"))
                      (apply general-utils/join-or))
         :last   100}
        #(dispatch [::set-nuvlabox-associated-ssh-keys (:resources %)])]})))


(reg-event-fx
  ::get-ssh-keys-not-associated
  (fn [{{{:keys [ssh-keys]} ::spec/nuvlabox} :db} [_ callback-fn]]
    (let [keys (if (nil? ssh-keys) [] ssh-keys)]
      (callback-fn [])
      {::cimi-api-fx/search
       [:credential
        {:filter (->> keys
                      (map #(str "id!='" % "'"))
                      (apply general-utils/join-and)
                      (general-utils/join-and "subtype=\"ssh-key\""))
         :last   100}
        #(callback-fn (get % :resources []))]})))


(reg-event-fx
  ::get-matching-vulns-from-db
  (fn [_ [_ vuln-ids]]
    (if (empty? vuln-ids)
      (dispatch [::set-matching-vulns-from-db {}])
      {::cimi-api-fx/search
       [:vulnerability
        {:filter (->> vuln-ids
                      (map #(str "name='" % "'"))
                      (apply general-utils/join-or))
         :last   110}
        #(dispatch [::set-matching-vulns-from-db (:resources %)])]})))


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
    {:db       (assoc db ::spec/page page)
     :dispatch [::get-nuvlabox-events]}))


(reg-event-fx
  ::get-nuvlabox-events
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
                             #(dispatch [::set-nuvlabox-events %])
                             ]})))


(reg-event-db
  ::nuvlabox-not-found?
  (fn [db [_ e]]
    (let [{:keys [_status _message]} (response/parse-ex-info e)]
      (assoc db ::spec/nuvlabox-not-found? (instance? js/Error e)))))


(reg-event-fx
  ::get-nuvlabox
  (fn [{{:keys [::spec/nuvlabox] :as db} :db} [_ id]]
    {:db                  (if (= (:id nuvlabox) id) db (merge db spec/defaults))
     ::cimi-api-fx/get    [id #(dispatch [::set-nuvlabox %])
                           :on-error #(dispatch [::set-nuvlabox nil])]
     ::cimi-api-fx/search [:nuvlabox-peripheral
                           {:filter  (str "parent='" id "'")
                            :last    10000
                            :orderby "id"}
                           #(dispatch [::set-nuvlabox-peripherals %])]
     :fx                  [[:dispatch [::get-nuvlabox-events id]]
                           [:dispatch [::job-events/get-jobs id]]
                           [:dispatch [::deployment-events/get-nuvlabox-deployments id]]]}))


(reg-event-fx
  ::decommission
  (fn [{{:keys [::spec/nuvlabox]} :db} _]
    (let [nuvlabox-id (:id nuvlabox)]
      {::cimi-api-fx/operation [nuvlabox-id "decommission"
                                #(dispatch [::get-nuvlabox nuvlabox-id])]})))

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
                              (dispatch [::set-nuvlabox %])))]}))


(reg-event-fx
  ::delete
  (fn [{{:keys [::spec/nuvlabox]} :db} _]
    (let [nuvlabox-id (:id nuvlabox)]
      {::cimi-api-fx/delete [nuvlabox-id #(dispatch [::history-events/navigate "edge"])]})))


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
  ::set-active-tab-index
  (fn [db [_ active-tab-index]]
    (assoc db ::spec/active-tab-index active-tab-index)))


(reg-event-db
  ::set-nuvlabox-managers
  (fn [db [_ status-per-manager]]
    (assoc db ::spec/nuvlabox-managers status-per-manager)))


(reg-event-db
  ::set-join-token
  (fn [db [_ token]]
    (assoc db ::spec/join-token token)))


(reg-event-fx
  ::get-join-token
  (fn [_ [_ nuvlabox-id scope]]
    {::cimi-api-fx/get [nuvlabox-id #(dispatch [::get-join-token-from-isg (:infrastructure-service-group %) scope])]}))


(reg-event-fx
  ::get-join-token-from-isg
  (fn [_ [_ isg-id scope]]
    {::cimi-api-fx/search
     [:infrastructure-service
      {:filter (str "parent='" isg-id "' and subtype='swarm'")
       :select "id"
       :last   1}
      #(dispatch [::get-join-token-from-is (:id (first (:resources %))) scope])]}))


(reg-event-fx
  ::get-join-token-from-is
  (fn [_ [_ is-id scope]]
    {::cimi-api-fx/search
     [:credential
      {:filter (str "parent='" is-id "' and subtype='swarm-token' and scope='" scope "'")
       :select "id, token"
       :last   1}
      #(dispatch [::set-join-token (first (:resources %))])]}))


(reg-event-fx
  ::get-nuvlabox-managers
  (fn [_ [_ self-id]]
    {::cimi-api-fx/search
     [:nuvlabox-status
      {:filter (str "cluster-node-role='manager' and parent!='" self-id "'")
       :select "id, parent, cluster-id, cluster-join-address"
       :last   100}
      #(dispatch [::get-nuvlabox-manager-by-status (:resources %)])]}))


(reg-event-fx
  ::get-nuvlabox-manager-by-status
  (fn [_ [_ statuses]]
    {::cimi-api-fx/search
     [:nuvlabox
      {:filter (->> (map :parent statuses)
                    (map #(str "id='" % "'"))
                    (apply general-utils/join-or))
       :select "id, name, nuvlabox-status"
       :last   100}
      #(dispatch [::set-nuvlabox-managers (let [status-per-manager (atom [])]
                                            (doseq [status statuses]
                                              (swap! status-per-manager
                                                     conj
                                                     {
                                                      :name
                                                      (:name (first (get (group-by :id (:resources %)) (:parent status))))
                                                      :status
                                                      status
                                                      }))
                                            @status-per-manager)])]}))


(reg-event-fx
  ::get-nuvlabox-cluster
  (fn [_ [_ nuvlabox-id]]
    {::cimi-api-fx/search [:nuvlabox-cluster
                           {:filter (str "nuvlabox-managers='" nuvlabox-id "'")
                            :select "id, cluster-id, managers, workers, orchestrator, name"
                            :last   1}
                           #(dispatch [::set-nuvlabox-cluster (first (:resources %))])]}))


(reg-event-db
  ::set-nuvlabox-cluster
  (fn [db [_ nuvlabox-cluster]]
    (assoc db ::spec/nuvlabox-cluster nuvlabox-cluster)))

