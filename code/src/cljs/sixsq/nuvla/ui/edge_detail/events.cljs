(ns sixsq.nuvla.ui.edge-detail.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.edge-detail.spec :as spec]
    [sixsq.nuvla.ui.edge.effects :as edge-fx]
    [sixsq.nuvla.ui.edge.events :as edge-events]
    [sixsq.nuvla.ui.edge.utils :as edge-utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-event-db
  ::set-nuvlabox-status
  (fn [db [_ nuvlabox-status]]
    (assoc db ::spec/nuvlabox-status nuvlabox-status)))


(reg-event-db
  ::set-nuvlabox-vulns
  (fn [db [_ nuvlabox-vulns]]
    (assoc db ::spec/nuvlabox-vulns {:summary (:summary nuvlabox-vulns)
                                     :items (into []
                                      (map
                                        (fn [{:keys [vulnerability-score] :as item}]
                                          (if vulnerability-score
                                            (cond
                                              (>= vulnerability-score 9.0) (assoc item
                                                                             :severity "CRITICAL"
                                                                             :color    edge-utils/vuln-critical-color)
                                              (and (< vulnerability-score 9.0)
                                                (>= vulnerability-score 7.0)) (assoc item
                                                                                :severity "HIGH"
                                                                                :color    edge-utils/vuln-high-color)
                                              (and (< vulnerability-score 7.0)
                                                (>= vulnerability-score 4.0)) (assoc item
                                                                                :severity "MEDIUM"
                                                                                :color    edge-utils/vuln-medium-color)
                                              (< vulnerability-score 4.0) (assoc item
                                                                            :severity "LOW"
                                                                            :color    edge-utils/vuln-low-color))
                                            (assoc item :severity "UNKNOWN" :color edge-utils/vuln-unknown-color)))
                                        (:items nuvlabox-vulns)))})))


(reg-event-db
  ::set-nuvlabox-ssh-keys
  (fn [db [_ ssh-keys]]
    (assoc db ::spec/nuvlabox-ssh-keys ssh-keys)))


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
  (fn [{:keys [db]} [_ {nb-status-id :nuvlabox-status id :id :as nuvlabox}]]
    {:db                             (assoc db ::spec/nuvlabox nuvlabox
                                               ::spec/loading? false)
     ::cimi-api-fx/get               [nb-status-id #(do
                                                      (dispatch [::set-nuvlabox-status %])
                                                      (dispatch [::set-nuvlabox-vulns (:vulnerabilities %)]))
                                      :on-error #(do
                                                   (dispatch [::set-nuvlabox-status nil])
                                                   (dispatch [::set-nuvlabox-vulns nil]))]
     ::edge-fx/get-status-nuvlaboxes [[id] #(dispatch [::edge-events/set-status-nuvlaboxes %])]}))


(reg-event-fx
  ::get-nuvlabox-ssh-keys
  (fn [_ [_ ssh-keys-ids]]
    (if (empty? ssh-keys-ids)
      (dispatch [::set-nuvlabox-ssh-keys {}])
      {::cimi-api-fx/search
       [:credential
        {:filter (cond-> (apply general-utils/join-or
                                (map #(str "id='" % "'") ssh-keys-ids)))
         :last   100}
        #(dispatch [::set-nuvlabox-ssh-keys {:associated-ssh-keys (:resources %)}])]})))


(reg-event-fx
  ::set-page
  (fn [{db :db} [_ page]]
    {:db       (assoc db ::spec/page page)
     :dispatch [::get-nuvlabox-events]}))


(reg-event-fx
  ::get-nuvlabox-events
  (fn [{{:keys [::spec/page
                ::spec/elements-per-page] :as db} :db} [_ href]]
    (let [filter-str   (str "content/resource/href='" href "'")
          order-by-str "created:desc"
          select-str   "id, content, severity, timestamp, category"
          first        (inc (* (dec page) elements-per-page))
          last         (* page elements-per-page)
          query-params {:filter   filter-str
                        :orderby  order-by-str
                        :select   select-str
                        :first    first
                        :last     last}]
      {::cimi-api-fx/search [:event
                             (general-utils/prepare-params query-params)
                             #(dispatch [::set-nuvlabox-events %])
                             ]})))


(reg-event-fx
  ::get-nuvlabox
  (fn [{{:keys [::spec/nuvlabox] :as db} :db} [_ id]]
    (cond-> {::cimi-api-fx/get    [id #(dispatch [::set-nuvlabox %])
                                   :on-error #(dispatch [::set-nuvlabox nil])]
             ::cimi-api-fx/search [:nuvlabox-peripheral
                                   {:filter  (str "parent='" id "'")
                                    :last    10000
                                    :orderby "id"}
                                   #(dispatch [::set-nuvlabox-peripherals %])]
             :dispatch-n          [[::get-nuvlabox-events id]]}
            (not= (:id nuvlabox) id) (assoc :db (merge db spec/defaults)))))


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
  ::check-custom-job-state
  (fn [_ [_ periph-id operation {:keys [id return-code progress status-message] :as job}]]
    (let [job-completed? (= progress 100)]
      (if job-completed?
        {:dispatch [::messages-events/add
                    {:header  (str (str/capitalize operation) " on " periph-id
                                   (if (= return-code 0) " completed." " failed!"))
                     :content status-message
                     :type    (if (= return-code 0) :success :error)}]}
        {:dispatch-later [{:ms 5000 :dispatch [::check-custom-action-job
                                               periph-id operation id]}]}))))


(reg-event-fx
  ::check-custom-action-job
  (fn [_ [_ periph-id operation job-id]]
    {::cimi-api-fx/get [job-id #(dispatch [::check-custom-job-state periph-id operation %])]}))


(reg-event-fx
  ::custom-action
  (fn [_ [_ resource-id operation success-msg]]
    {::cimi-api-fx/operation [resource-id operation
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
                                   (dispatch [::check-custom-action-job
                                              resource-id operation (:location %)])
                                   ))]}))


(reg-event-db
  ::set-active-tab-index
  (fn [db [_ active-tab-index]]
    (assoc db ::spec/active-tab-index active-tab-index)))
