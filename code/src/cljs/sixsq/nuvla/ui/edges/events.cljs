(ns sixsq.nuvla.ui.edges.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.edges.spec :as spec]
    [sixsq.nuvla.ui.edges.utils :as utils]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]))

(def refresh-id :nuvlabox-get-nuvlaboxes)
(def refresh-id-locations :nuvlabox-get-nuvlabox-locations)
(def refresh-id-inferred-locations :nuvlabox-get-nuvlabox-inferred-locations)
(def refresh-summary-id :nuvlabox-get-nuvlaboxes-summary)
(def refresh-id-cluster :nuvlabox-get-nuvlabox-cluster)
(def refresh-id-clusters :nuvlabox-get-nuvlabox-clusters)


(reg-event-fx
  ::refresh-root
  (fn [_ _]
    {:fx [[:dispatch [::main-events/action-interval-start {:id        refresh-id
                                                           :frequency 10000
                                                           :event     [::get-nuvlaboxes]}]]
          [:dispatch [::main-events/action-interval-start {:id        refresh-id-locations
                                                           :frequency 10000
                                                           :event     [::get-nuvlabox-locations]}]]
          [:dispatch [::main-events/action-interval-start {:id        refresh-summary-id
                                                           :frequency 10000
                                                           :event     [::get-nuvlaboxes-summary]}]]
          [:dispatch [::main-events/action-interval-start {:id        refresh-id-clusters
                                                           :frequency 10000
                                                           :event     [::get-nuvlabox-clusters]}]]]}))


(reg-event-fx
  ::refresh-clusters
  (fn [_ _]
    {:fx [[:dispatch [::main-events/action-interval-start {:id        refresh-id
                                                           :frequency 10000
                                                           :event     [::get-nuvlaboxes]}]]
          [:dispatch [::main-events/action-interval-start {:id        refresh-summary-id
                                                           :frequency 10000
                                                           :event     [::get-nuvlaboxes-summary]}]]
          [:dispatch [::main-events/action-interval-start {:id        refresh-id-clusters
                                                           :frequency 10000
                                                           :event     [::get-nuvlabox-clusters]}]]]}))


(reg-event-fx
  ::refresh-cluster
  (fn [_ [_ cluster-id]]
    {:fx [[:dispatch [::main-events/action-interval-start {:id        refresh-id
                                                           :frequency 10000
                                                           :event     [::get-nuvlaboxes]}]]
          [:dispatch [::main-events/action-interval-start {:id        refresh-id-cluster
                                                           :frequency 10000
                                                           :event     [::get-nuvlabox-cluster
                                                                       (str "nuvlabox-cluster/" cluster-id)]}]]]}))


(reg-event-fx
  ::set-page
  (fn [{db :db} [_ page]]
    {:db       (assoc db ::spec/page page)
     :dispatch [::refresh-root]}))


(reg-event-fx
  ::set-full-text-search
  (fn [{db :db} [_ full-text-search]]
    {:db       (assoc db ::spec/full-text-search full-text-search
                         ::spec/page 1)
     :dispatch [::refresh-root]}))


(reg-event-fx
  ::get-nuvlaboxes
  (fn [{{:keys [::spec/state-selector
                ::spec/page
                ::spec/elements-per-page
                ::spec/full-text-search] :as _db} :db} _]
    {::cimi-api-fx/search [:nuvlabox
                           (utils/get-query-params full-text-search page elements-per-page
                                                   state-selector)
                           #(dispatch [::set-nuvlaboxes %])]}))


(reg-event-fx
  ::set-nuvlaboxes
  (fn [{:keys [db]} [_ nuvlaboxes]]
    (if (instance? js/Error nuvlaboxes)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info nuvlaboxes)]
                   {:header  (cond-> (str "failure getting nuvlaboxes")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      {:db (assoc db ::spec/nuvlaboxes nuvlaboxes
                     ::main-spec/loading? false)})))


(reg-event-fx
  ::get-nuvlabox-locations
  (fn [{{:keys [::spec/state-selector
                ::spec/full-text-search] :as _db} :db} _]
    {::cimi-api-fx/search [:nuvlabox
                           {:first  1
                            :last   10000
                            :select "id,name,online,location,inferred-location"
                            :filter (general-utils/join-and
                                      "(location!=null or inferred-location!=null)"
                                      (when state-selector (utils/state-filter state-selector))
                                      (general-utils/fulltext-query-string full-text-search))}
                           #(dispatch [::set-nuvlabox-locations %])]}))


(reg-event-fx
  ::set-nuvlabox-locations
  (fn [{:keys [db]} [_ nuvlaboxes]]
    (if (instance? js/Error nuvlaboxes)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info nuvlaboxes)]
                   {:header  (cond-> (str "failure getting nuvlabox locations")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      {:db (assoc db ::spec/nuvlabox-locations nuvlaboxes
                     ::main-spec/loading? false)})))


(reg-event-fx
  ::set-nuvlaboxes-summary
  (fn [{db :db} [_ nuvlaboxes-summary]]
    {:db (assoc db ::spec/nuvlaboxes-summary nuvlaboxes-summary)}))


(reg-event-fx
  ::get-nuvlaboxes-summary
  (fn [{{:keys [::spec/full-text-search] :as _db} :db} _]
    {::cimi-api-fx/search [:nuvlabox
                           (utils/get-query-aggregation-params
                             full-text-search
                             "terms:online,terms:state"
                             nil)
                           #(dispatch [::set-nuvlaboxes-summary %])]}))


(reg-event-fx
  ::set-nuvlabox-cluster-summary
  (fn [{db :db} [_ nuvlaboxes-summary]]
    {:db (assoc db ::spec/nuvlabox-cluster-summary nuvlaboxes-summary)}))


(reg-event-fx
  ::get-nuvlabox-cluster-summary
  (fn [{{:keys [::spec/full-text-search
                ::spec/nuvlabox-cluster] :as _db} :db} _]
    {::cimi-api-fx/search [:nuvlabox
                           (utils/get-query-aggregation-params
                             full-text-search
                             "terms:online,terms:state"
                             (->> (concat (:nuvlabox-managers nuvlabox-cluster) (:nuvlabox-workers nuvlabox-cluster))
                                  (map #(str "id='" % "'"))
                                  (apply general-utils/join-or)))
                           #(dispatch [::set-nuvlabox-cluster-summary %])]}))


(reg-event-fx
  ::set-nuvlabox-clusters
  (fn [{:keys [db]} [_ nuvlabox-clusters]]
    (let [not-found? (nil? nuvlabox-clusters)]
      (if (instance? js/Error nuvlabox-clusters)
        (dispatch [::messages-events/add
                   (let [{:keys [status message]} (response/parse-ex-info nuvlabox-clusters)]
                     {:header  (cond-> (str "failure getting nuvlabox clusters")
                                       status (str " (" status ")"))
                      :content message
                      :type    :error})])
        (cond->
          {:db (assoc db ::spec/nuvlabox-clusters nuvlabox-clusters
                         ::main-spec/loading? false
                         ::spec/nuvlabox-not-found? not-found?)})))))


(reg-event-fx
  ::get-nuvlabox-clusters
  (fn [{{:keys [::spec/page
                ::spec/elements-per-page
                ::spec/full-text-search] :as _db} :db} _]
    {::cimi-api-fx/search [:nuvlabox-cluster
                           (utils/get-query-params full-text-search page elements-per-page nil)
                           #(do
                              (dispatch [::set-nuvlabox-clusters %])
                              (dispatch [::get-nuvlaboxes-in-clusters %]))]}))


(reg-event-fx
  ::set-nuvlaboxes-summary-all
  (fn [{db :db} [_ nuvlaboxes-summary]]
    {:db (assoc db ::spec/nuvlaboxes-summary-all nuvlaboxes-summary)}))


(reg-event-fx
  ::get-nuvlaboxes-summary-all
  (fn [{_db :db} _]
    {::cimi-api-fx/search [:nuvlabox
                           (utils/get-query-aggregation-params nil "terms:online,terms:state" nil)
                           #(dispatch [::set-nuvlaboxes-summary-all %])]}))

(reg-event-fx
  ::set-state-selector
  (fn [{db :db} [_ state-selector]]
    {:db (assoc db ::spec/state-selector state-selector
                   ::spec/page 1)
     :fx [[:dispatch [::get-nuvlaboxes]]
          [:dispatch [::get-nuvlabox-locations]]]}))


(reg-event-db
  ::open-modal
  (fn [db [_ modal-id]]
    (assoc db ::spec/open-modal modal-id)))


(reg-event-fx
  ::create-ssh-key
  (fn [_ [_ ssh-template dispatch-vector]]
    {::cimi-api-fx/add [:credential ssh-template
                        #(do
                           (dispatch [::set-nuvlabox-ssh-keys {:ids         [(:resource-id %)]
                                                               :public-keys [(:public-key %)]}])
                           (dispatch [::set-nuvlabox-created-private-ssh-key (:private-key %)])
                           (dispatch dispatch-vector))]}))


(reg-event-fx
  ::find-nuvlabox-ssh-keys
  (fn [_ [_ ssh-keys-ids dispatch-vector]]
    {::cimi-api-fx/search
     [:credential
      {:filter (cond-> (apply general-utils/join-or
                              (map #(str "id='" % "'") ssh-keys-ids)))
       :select "public-key"
       :last   10000}
      #(do
         (dispatch [::set-nuvlabox-ssh-keys {:ids         ssh-keys-ids
                                             :public-keys (into [] (map :public-key
                                                                        (:resources %)))}])
         (dispatch dispatch-vector))]}))


(reg-event-db
  ::set-nuvlabox-ssh-keys
  (fn [db [_ ssh-key-list]]
    (assoc db ::spec/nuvlabox-ssh-key ssh-key-list)))


(reg-event-db
  ::set-nuvlabox-created-private-ssh-key
  (fn [db [_ private-key]]
    (assoc db ::spec/nuvlabox-private-ssh-key private-key)))


(reg-event-fx
  ::assign-ssh-keys
  (fn [_ [_ {:keys [ids]} nuvlabox-id]]
    {::cimi-api-fx/edit [nuvlabox-id {:ssh-keys ids}
                         #(when (instance? js/Error %)
                            (let [{:keys [status message]} (response/parse-ex-info %)]
                              (dispatch [::messages-events/add
                                         {:header  (cond-> (str "error editing " nuvlabox-id)
                                                           status (str " (" status ")"))
                                          :content message
                                          :type    :error}])))]}))


(reg-event-fx
  ::create-nuvlabox
  (fn [_ [_ creation-data]]
    {::cimi-api-fx/add [:nuvlabox creation-data
                        #(dispatch [::set-created-nuvlabox-id %])]}))


(reg-event-db
  ::set-created-nuvlabox-id
  (fn [db [_ {:keys [resource-id]}]]
    (dispatch [::get-nuvlaboxes])
    (assoc db ::spec/nuvlabox-created-id resource-id)))


(reg-event-fx
  ::create-nuvlabox-usb-api-key
  (fn [_ [_ ttl-days]]
    (let [creation-data {:description "Auto-generated for NuvlaEdge self-registration USB trigger"
                         :name        "NuvlaEdge self-registration USB trigger"
                         :template    {:method "generate-api-key"
                                       :ttl    (* ttl-days 24 60 60)
                                       :href   "credential-template/generate-api-key"}}]
      {::cimi-api-fx/add [:credential creation-data
                          #(dispatch [::set-nuvlabox-usb-api-key {:resource-id (:resource-id %)
                                                                  :secret-key  (:secret-key %)}])]})))


(reg-event-db
  ::set-nuvlabox-usb-api-key
  (fn [db [_ apikey]]
    (assoc db ::spec/nuvlabox-usb-api-key apikey)))


(reg-event-db
  ::set-vpn-infra
  (fn [db [_ {:keys [resources]}]]
    (assoc db ::spec/vpn-infra resources)))


(reg-event-fx
  ::get-vpn-infra
  (fn [{:keys [db]} _]
    {:db                  (assoc db ::spec/vpn-infra nil)
     ::cimi-api-fx/search [:infrastructure-service
                           {:filter "subtype='vpn' and vpn-scope='nuvlabox'"
                            :select "id, name, description"
                            :last   10000}
                           #(dispatch [::set-vpn-infra %])]}))


(reg-event-db
  ::set-nuvlabox-releases
  (fn [db [_ {:keys [resources]}]]
    (assoc db ::spec/nuvlabox-releases resources)))


(reg-event-fx
  ::get-nuvlabox-releases
  (fn [{:keys [db]} _]
    {:db                  (assoc db ::spec/nuvlabox-releases nil)
     ::cimi-api-fx/search [:nuvlabox-release
                           {:select  "id, release, pre-release, release-notes, url, compose-files"
                            :orderby "release-date:desc"
                            :last    10000}
                           #(dispatch [::set-nuvlabox-releases %])]}))


(reg-event-fx
  ::set-nuvlabox-cluster
  (fn [{:keys [db]} [_ nuvlabox-cluster]]
    {:db (assoc db ::spec/nuvlabox-cluster nuvlabox-cluster
                   ::spec/nuvlabox-not-found? (nil? nuvlabox-cluster))}))


(reg-event-fx
  ::get-nuvlabox-cluster
  (fn [_ [_ cluster-id]]
    {::cimi-api-fx/get [cluster-id #(dispatch [::set-nuvlabox-cluster %])
                        :on-error #(dispatch [::set-nuvlabox-cluster nil])]}))


(reg-event-fx
  ::get-nuvlaboxes-in-clusters
  (fn [_ [_ selected-clusters]]
    {::cimi-api-fx/search [:nuvlabox
                           {:filter (apply general-utils/join-or
                                           (map #(str "id='" % "'") (flatten
                                                                      (map
                                                                        (fn [c]
                                                                          (concat
                                                                            (:nuvlabox-managers c)
                                                                            (get c :nuvlabox-workers [])))
                                                                        (:resources selected-clusters)))))
                            :last   10000}
                           #(dispatch [::set-nuvlaboxes-in-clusters %])]}))


(reg-event-fx
  ::set-nuvlaboxes-in-clusters
  (fn [{:keys [db]} [_ nuvlaboxes]]
    (if (instance? js/Error nuvlaboxes)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info nuvlaboxes)]
                   {:header  (cond-> (str "failure getting nuvlaboxes")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      {:db (assoc db ::spec/nuvlaboxes-in-clusters nuvlaboxes
                     ::main-spec/loading? false)})))

(reg-event-db
  ::set-ssh-keys-available
  (fn [db [_ {:keys [resources]}]]
    (assoc db ::spec/ssh-keys-available resources)))


(reg-event-fx
  ::get-ssh-keys-available
  (fn [{:keys [db]} [_ subtypes additional-filter]]
    {:db                  (assoc db ::spec/ssh-keys-available nil)
     ::cimi-api-fx/search [:credential
                           {:filter (cond-> (apply general-utils/join-or
                                                   (map #(str "subtype='" % "'") subtypes))
                                            additional-filter (general-utils/join-and
                                                                additional-filter))
                            :last   10000}
                           #(dispatch [::set-ssh-keys-available %])]}))


(reg-event-db
  ::set-active-tab-index
  (fn [db [_ active-tab-index]]
    (assoc db ::spec/active-tab-index active-tab-index)))


(reg-event-fx
  ::enable-host-level-management
  (fn [_ [_ nuvlabox-id]]
    {::cimi-api-fx/operation
     [nuvlabox-id
      "enable-host-level-management"
      #(if (instance? js/Error %)
         (let [{:keys [status message]} (response/parse-ex-info %)]
           (dispatch [::messages-events/add
                      {:header  (cond-> (str "error enabling host level management for " nuvlabox-id)
                                        status (str " (" status ")"))
                       :content message
                       :type    :error}]))
         (dispatch [::set-nuvlabox-playbooks-cronjob %]))
      nil]}))


(reg-event-db
  ::set-nuvlabox-playbooks-cronjob
  (fn [db [_ cronjob]]
    (assoc db ::spec/nuvlabox-playbooks-cronjob cronjob)))
