(ns sixsq.nuvla.ui.edges-detail.events
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.deployments.events :as deployments-events]
            [sixsq.nuvla.ui.edges-detail.spec :as spec]
            [sixsq.nuvla.ui.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.job.events :as job-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.events :as events-plugin]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.response :as response]))

(reg-event-db
  ::set-nuvlabox-status
  (fn [db [_ nuvlabox-status]]
    (assoc db ::spec/nuvlabox-status nuvlabox-status)))

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
  ::set-vuln-severity-selector
  (fn [db [_ vuln-severity]]
    (assoc db ::spec/vuln-severity-selector vuln-severity)))

(reg-event-fx
  ::set-nuvlabox
  (fn [{:keys [db]} [_ {nb-status-id     :nuvlabox-status
                        infra-srv-grp-id :infrastructure-service-group
                        :as              nuvlabox}]]
    {:db               (assoc db ::spec/nuvlabox-not-found? (nil? nuvlabox)
                         ::spec/nuvlabox nuvlabox
                         ::main-spec/loading? false)
     ::cimi-api-fx/get [nb-status-id #(do
                                        (dispatch [::set-nuvlabox-status %])
                                        (dispatch [::get-nuvlaedge-release %])
                                        (dispatch [::set-nuvlabox-vulns (:vulnerabilities %)])
                                        (dispatch [::get-matching-vulns-from-db (map :vulnerability-id
                                                                                     (:items (:vulnerabilities %)))]))
                        :on-error #(do
                                     (dispatch [::set-nuvlabox-status nil])
                                     (dispatch [::set-nuvlabox-vulns nil]))]
     :fx               [(when infra-srv-grp-id [:dispatch [::get-infra-services infra-srv-grp-id]])]}))

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
    (let [on-success #(do
                        (let [{:keys [status message]} (response/parse %)]
                          (dispatch [::messages-events/add
                                     {:header  (cond-> (str "operation " operation " will be executed soon")
                                                       status (str " (" status ")"))
                                      :content message
                                      :type    :success}]))
                        (on-success-fn (:message %))
                        (dispatch [::get-nuvlabox resource-id]))]
      {::cimi-api-fx/operation
       [resource-id operation on-success :data data :on-error on-error-fn]})))

(reg-event-fx
  ::operation-text-response
  (fn [_ [_ operation resource-id on-success-fn on-error-fn]]
    (let [on-success #(do
                        (dispatch [::messages-events/add
                                   {:header  (str "operation " operation " successful")
                                    :content (or (:cronjob %) %)
                                    :type    :success}])
                        (on-success-fn (:message %))
                        (dispatch [::get-nuvlabox resource-id]))
          on-error   #(do
                        (cimi-api-fx/default-operation-on-error resource-id operation %)
                        (on-error-fn))]
      {::cimi-api-fx/operation
       [resource-id operation on-success :on-error on-error]})))

(reg-event-fx
  ::get-nuvlabox
  (fn [{{:keys [::spec/nuvlabox ::spec/nuvlabox-current-playbook] :as db} :db} [_ id]]
    {:db                  (cond-> db
                                  (not= (:id nuvlabox) id)
                                  (merge spec/defaults))
     ::cimi-api-fx/get    [id #(dispatch [::set-nuvlabox %])
                           :on-error #(dispatch [::set-nuvlabox nil])]
     ::cimi-api-fx/search [:nuvlabox-peripheral
                           {:filter  (str "parent='" id "'")
                            :last    10000
                            :orderby "id"}
                           #(dispatch [::set-nuvlabox-peripherals %])]
     :fx                  [[:dispatch [::events-plugin/load-events
                                       [::spec/events] id false]]
                           [:dispatch [::job-events/get-jobs id]]
                           [:dispatch [::deployments-events/get-deployments
                                       {:filter-external-arg (str "nuvlabox='" id "'")
                                        :pagination-db-path  ::spec/deployment-pagination}]]
                           [:dispatch [::get-nuvlabox-playbooks id]]
                           [:dispatch [::get-nuvlabox-current-playbook (if (= id (:parent nuvlabox-current-playbook))
                                                                         (:id nuvlabox-current-playbook)
                                                                         nil)]]]}))

(reg-event-fx
  ::decommission
  (fn [{{:keys [::spec/nuvlabox]} :db} _]
    (let [nuvlabox-id (:id nuvlabox)
          on-success  #(dispatch [::get-nuvlabox nuvlabox-id])]
      {::cimi-api-fx/operation [nuvlabox-id "decommission" on-success]})))

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
      {::cimi-api-fx/delete [nuvlabox-id #(dispatch [::routing-events/navigate routes/edges])]})))

(reg-event-fx
  ::custom-action
  (fn [_ [_ resource-id operation success-msg]]
    (let [on-job-complete (fn [{:keys [status-message return-code]}]
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
          on-success      #(when success-msg
                             (dispatch [::messages-events/add
                                        {:header  success-msg
                                         :content success-msg
                                         :type    :success}])
                             (dispatch
                               [::job-events/wait-job-to-complete
                                {:job-id              (:location %)
                                 :on-complete         on-job-complete
                                 :refresh-interval-ms 5000}]))]
      {::cimi-api-fx/operation [resource-id operation on-success]})))

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
      #(dispatch [::set-nuvlabox-managers
                  (into {}
                        (for [status statuses]
                          (let [id (:parent status)]
                            {id
                             {:id     id
                              :name   (:name (first (get (group-by :id (:resources %)) (:parent status))))
                              :status status}})))])]}))

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

(reg-event-fx
  ::get-nuvlabox-playbooks
  (fn [_ [_ nuvlabox-id]]
    {::cimi-api-fx/search [:nuvlabox-playbook
                           {:filter  (str "parent='" nuvlabox-id "'")
                            :select  "id, run, enabled, type, output, name, description"
                            :orderby "type:desc"
                            :last    1000}
                           #(dispatch [::set-nuvlabox-playbooks (:resources %)])]}))

(reg-event-db
  ::set-nuvlabox-playbooks
  (fn [db [_ nuvlabox-playbooks]]
    (assoc db ::spec/nuvlabox-playbooks nuvlabox-playbooks)))


(reg-event-fx
  ::get-infra-services
  (fn [_ [_ group-id]]
    {::cimi-api-fx/search [:infrastructure-service
                           {:filter  (str "parent='" group-id "'")
                            :select  "id, name, description, subtype"
                            :orderby "subtype:asc, name:asc"
                            :last    1000}
                           #(dispatch [::set-infra-services (:resources %)])]}))

(reg-event-db
  ::set-infra-services
  (fn [db [_ infra-services]]
    (assoc db ::spec/infra-services infra-services)))

(reg-event-fx
  ::edit-playbook
  (fn [_ [_ playbook new-body]]
    (let [nuvlabox-id (:parent playbook)
          playbook-id (:id playbook)]
      {::cimi-api-fx/edit [playbook-id new-body
                           #(if (instance? js/Error %)
                              (let [{:keys [status message]} (response/parse-ex-info %)]
                                (dispatch [::messages-events/add
                                           {:header  (cond-> "Failed to update the playbook's run"
                                                             status (str " (" status ")"))
                                            :content message
                                            :type    :error}]))
                              (do
                                (dispatch [::messages-events/add
                                           {:header  "Playbook updated"
                                            :content "The Playbook's run script has been updated."
                                            :type    :info}])
                                (dispatch [::get-nuvlabox-playbooks nuvlabox-id])))]})))

(reg-event-fx
  ::add-nuvlabox-playbook
  (fn [_ [_ data]]
    {::cimi-api-fx/add [:nuvlabox-playbook data
                        #(dispatch [::get-nuvlabox-playbooks (:parent data)])]}))

(reg-event-fx
  ::get-emergency-playbooks
  (fn [_ [_ nuvlabox-id]]
    {::cimi-api-fx/search [:nuvlabox-playbook
                           {:filter  (str "parent='" nuvlabox-id "' and type='EMERGENCY'")
                            :select  "id, enabled, type, name"
                            :orderby "enabled:desc"
                            :last    1000}
                           #(dispatch [::set-emergency-playbooks (:resources %)])]}))

(reg-event-db
  ::set-emergency-playbooks
  (fn [db [_ nuvlabox-playbooks]]
    (assoc db ::spec/nuvlabox-emergency-playbooks nuvlabox-playbooks)))

(reg-event-fx
  ::get-nuvlabox-current-playbook
  (fn [_ [_ nuvlabox-playbook-id]]
    {::cimi-api-fx/get [nuvlabox-playbook-id #(dispatch [::set-nuvlabox-current-playbook %])
                        :on-error #(dispatch [::set-nuvlabox-current-playbook nil])]}))

(reg-event-db
  ::set-nuvlabox-current-playbook
  (fn [db [_ nuvlabox-playbook]]
    (assoc db ::spec/nuvlabox-current-playbook nuvlabox-playbook)))


(reg-event-db
  ::set-nuvlaedge-release
  (fn [db [_ nuvlaedge-release]]
    (assoc db ::spec/nuvlaedge-release nuvlaedge-release)))


(reg-event-fx
  ::get-nuvlaedge-release
  (fn [{:keys [db]} [_ {:keys [nuvlabox-engine-version]}]]
    (when nuvlabox-engine-version
      {:db                  (assoc db ::spec/nuvlaedge-release nil)
       ::cimi-api-fx/search [:nuvlabox-release
                             {:filter   (str "release='" nuvlabox-engine-version "'")
                              :select  "id, pre-release"
                              :orderby "release-date:desc"
                              :last    10000}
                             #(dispatch [::set-nuvlaedge-release (first (:resources %))])]})))