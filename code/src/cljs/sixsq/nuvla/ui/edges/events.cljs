(ns sixsq.nuvla.ui.edges.events
  (:require [ajax.core :as ajax]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [re-frame.core :refer [dispatch inject-cofx reg-event-db
                                   reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.deployment-sets-detail.events :as depl-group-events]
            [sixsq.nuvla.ui.edges.spec :as spec]
            [sixsq.nuvla.ui.edges.utils :as utils :refer [get-dynamic-fleet-filter-string get-full-filter-string]]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :refer [ordering->order-string] :as table-plugin]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.utils :refer [get-query-param
                                                  get-stored-db-value-from-query-param]]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.session.utils :refer [get-active-claim]]
            [sixsq.nuvla.ui.utils.bulk-edit-tags-modal :refer [tags-modal-ids-set]]
            [sixsq.nuvla.ui.utils.general :as general-utils :refer [create-filter-for-read-only-resources]]
            [sixsq.nuvla.ui.utils.response :as response]
            [sixsq.nuvla.ui.utils.timeseries :as ts-utils]
            [sixsq.nuvla.ui.utils.time :as time]))

(def refresh-id :nuvlabox-get-nuvlaboxes)
(def refresh-id-locations :nuvlabox-get-nuvlabox-locations)
(def refresh-summary-id :nuvlabox-get-nuvlaboxes-summary)
(def refresh-id-cluster :nuvlabox-get-nuvlabox-cluster)
(def refresh-id-clusters :nuvlabox-get-nuvlabox-clusters)


(reg-event-fx
  ::init
  [(inject-cofx :storage/all)]
  (fn [{{:keys [current-route] :as db} :db
        storage                        :storage/all}
       [_ external-restriction-filter]]
    (let [db-path            ::spec/state-selector
          search-query       (get-stored-db-value-from-query-param current-route [db-path])
          filter-storage-key (get-query-param current-route :filter-storage-key)
          storage-filter     (get storage filter-storage-key)
          filter-query       (get-query-param current-route (keyword spec/resource-name))]
      {:db (-> db
               (merge spec/defaults)
               (assoc ::main-spec/loading? true)
               (assoc db-path search-query)
               (assoc ::spec/additional-filter (or storage-filter filter-query))
               (assoc ::spec/external-restriction-filter external-restriction-filter))
       :fx [[:dispatch [::init-view]]
            [:dispatch [::refresh-root]]
            [:dispatch [::get-nuvlabox-releases]]]})))

(reg-event-fx
  ::init-view
  [(inject-cofx :storage/get {:name spec/local-storage-key})]
  (fn [{{current-route :current-route} :db
        storage                        :storage/get}]
    (let [view-query (-> current-route :query-params :view)]
      (when-not view-query
        {:fx [[:dispatch [::routing-events/change-query-param
                          {:partial-query-params
                           {:view (or
                                    (:view (edn/read-string storage))
                                    spec/table-view)}}]]]}))))

(reg-event-fx
  ::refresh-root
  (fn []
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
  ::get-nuvlaboxes
  (fn [{{:keys [::spec/ordering] :as db} :db} _]
    (let [ordering (or ordering spec/default-ordering)]
      {::cimi-api-fx/search
       [:nuvlabox
        (->> {:orderby (ordering->order-string ordering)
              :filter  (get-full-filter-string db)}
             (pagination-plugin/first-last-params
               db [::spec/pagination]))
        #(dispatch [::set-nuvlaboxes %])]})))

(reg-event-fx
  ::get-edges-without-edit-rights
  (fn [{{:keys [::spec/select
                ::session-spec/session] :as db} :db} _]
    (let [selected-filter (table-plugin/build-bulk-filter
                            select
                            (get-full-filter-string db))
          filter          (create-filter-for-read-only-resources session selected-filter)]
      {::cimi-api-fx/search
       [:nuvlabox
        {:filter filter :select "id"}
        #(dispatch [::set-edges-without-edit-rights %])]})))

(reg-event-fx
  ::set-edges-without-edit-rights
  (fn [{:keys [db]} [_ nuvlaboxes]]
    (if (instance? js/Error nuvlaboxes)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info nuvlaboxes)]
                   {:header  (cond-> (str "failure getting nuvlaedges")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      {:db (assoc db ::spec/edges-without-edit-rights nuvlaboxes)})))

(reg-event-fx
  ::bulk-deploy-dynamic
  (fn [{db :db} [_ id]]
    (let [filter-string (get-dynamic-fleet-filter-string db)]
      {:fx [[:dispatch [::depl-group-events/set-fleet-filter
                        filter-string id]]
            [:dispatch [::get-selected-edge-ids [::depl-group-events/set-edges] filter-string]]]})))

(reg-event-fx
  ::bulk-deploy-static
  (fn [{{:keys [::spec/select] :as db} :db} _]
    (let [filter-string (table-plugin/build-bulk-filter
                          select
                          (get-full-filter-string db))]
      {:fx [[:dispatch [::get-selected-edge-ids [::depl-group-events/set-edges] filter-string]]]})))

(reg-event-fx
  ::get-selected-edge-ids
  (fn [_ [_ event filter-string]]
    {::cimi-api-fx/search
     [:nuvlabox
      {:filter      filter-string
       :select      "id"
       :aggregation spec/state-summary-agg-term}
      #(dispatch (conj event %))]}))


(reg-event-fx
  ::set-additional-filter
  (fn [{db :db} [_ filter]]
    {:db (-> db
             (assoc ::spec/additional-filter filter)
             (assoc-in [::spec/pagination :active-page] 1))
     :fx [[:dispatch [::get-nuvlaboxes]]
          [:dispatch [::get-nuvlaboxes-summary]]
          [:dispatch [::table-plugin/reset-bulk-edit-selection [::spec/select]]]
          [:dispatch [::get-nuvlabox-locations]]
          [:dispatch [::fetch-fleet-stats]]]}))

(def fleet-availability-stats ["availability-stats"])

(reg-event-fx
  ::set-nuvlaboxes
  (fn [{db :db} [_ nuvlaboxes]]
    (if (instance? js/Error nuvlaboxes)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info nuvlaboxes)]
                   {:header  (cond-> (str "failure getting nuvlaedges")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      {:db (assoc db ::spec/nuvlaboxes nuvlaboxes
                     ::main-spec/loading? false)
       :fx [[:dispatch [::get-nuvlaedges-status nuvlaboxes]]]})))


(reg-event-fx
  ::get-nuvlaedges-status
  (fn [_ [_ {nuvlaboxes :resources}]]
    (when (seq nuvlaboxes)
      {::cimi-api-fx/search
       [:nuvlabox-status
        {:select "id,parent,next-heartbeat,nuvlabox-engine-version,online,last-heartbeat"
         :filter (general-utils/filter-eq-parent-vals (mapv :id nuvlaboxes))}
        #(dispatch [::set-nuvlaedges-status %])]})))


(reg-event-fx
  ::set-nuvlaedges-status
  (fn [{:keys [db]} [_ {:keys [resources] :as nuvlaboxes-status}]]
    (if (instance? js/Error nuvlaboxes-status)
      {:fx [[:dispatch [::messages-events/add
                        (let [{:keys [status message]} (response/parse-ex-info nuvlaboxes-status)]
                          {:header  (cond-> (str "failure getting status for nuvla edges")
                                            status (str " (" status ")"))
                           :content message
                           :type    :error})]]]}
      {:db (assoc db ::spec/nuvlaedges-select-status (zipmap
                                                       (map :parent resources)
                                                       resources)
                     ::main-spec/loading? false)})))

(reg-event-fx
  ::get-nuvlabox-locations
  (fn [{db :db}]
    {::cimi-api-fx/search [:nuvlabox
                           {:first  1
                            :last   10000
                            :select "id,name,online,location,inferred-location"
                            :filter (general-utils/join-and
                                      "(location!=null or inferred-location!=null)"
                                      (get-full-filter-string db))}
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


(reg-event-db
  ::set-nuvlaboxes-summary
  (fn [db [_ nuvlaboxes-summary]]
    (assoc db ::spec/nuvlaboxes-summary nuvlaboxes-summary)))


(reg-event-fx
  ::get-nuvlaboxes-summary
  (fn [{{:keys [::spec/additional-filter] :as db} :db}]
    {::cimi-api-fx/search [:nuvlabox
                           (utils/get-query-aggregation-params
                             (full-text-search-plugin/filter-text
                               db [::spec/edges-search])
                             spec/state-summary-agg-term
                             additional-filter)
                           #(dispatch [::set-nuvlaboxes-summary %])]}))

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
  (fn [{db :db}]
    {::cimi-api-fx/search [:nuvlabox-cluster
                           (->> {:orderby "created:desc"
                                 :filter  (full-text-search-plugin/filter-text
                                            db [::spec/edges-search])}
                                (pagination-plugin/first-last-params
                                  db [::spec/pagination]))
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
                           (utils/get-query-aggregation-params
                             nil spec/state-summary-agg-term nil)
                           #(dispatch [::set-nuvlaboxes-summary-all %])]}))

(reg-event-fx
  ::set-state-selector
  (fn [{db :db} [_ state-selector]]
    (let [db-path ::spec/state-selector]
      {:db (assoc db db-path state-selector)
       :fx [[:dispatch [::pagination-plugin/change-page [::spec/pagination] 1]]
            [:dispatch [::get-nuvlabox-locations]]
            [:dispatch [::routing-events/store-in-query-param {:db-path [db-path]
                                                               :value   state-selector}]]
            [:dispatch [::table-plugin/reset-bulk-edit-selection [::spec/select]]]
            [:dispatch [::fetch-fleet-stats]]]})))


(reg-event-fx
  ::open-modal
  (fn [{db :db} [_ modal-id]]
    (let [fx (when (and (tags-modal-ids-set modal-id)
                        (not (tags-modal-ids-set (::spec/open-modal db))))
               [:dispatch [::get-edges-without-edit-rights]])]
      {:db (assoc db ::spec/open-modal modal-id)
       :fx [fx]})))

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
      {:filter (general-utils/filter-eq-ids ssh-keys-ids)
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
                        (fn [res]
                          (dispatch [::set-created-nuvlabox-id res])
                          (dispatch [::get-nuvlaboxes]))]}))


(reg-event-db
  ::set-created-nuvlabox-id
  (fn [db [_ {:keys [resource-id]}]]
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
    (let [session (::session-spec/session db)]
      {:db                  (assoc db ::spec/nuvlabox-releases nil)
       ::cimi-api-fx/search [:nuvlabox-release
                             {:select  "id, release, pre-release, release-notes, url, compose-files, published"
                              :filter  (general-utils/join-or "published=true" "published=null" (str "acl/view-data='" (get-active-claim session) "'"))
                              :orderby "release:desc"
                              :last    10000}
                             #(dispatch [::set-nuvlabox-releases %])]})))


(reg-event-fx
  ::set-nuvlabox-cluster
  (fn [{:keys [db]} [_ nuvlabox-cluster not-found]]
    {:db (assoc db ::spec/nuvlabox-cluster nuvlabox-cluster
                   ::spec/nuvlabox-not-found? not-found)}))


(reg-event-fx
  ::get-nuvlabox-cluster
  (fn [_ [_ cluster-id]]
    {::cimi-api-fx/get [cluster-id #(dispatch [::set-nuvlabox-cluster %])
                        :on-error #(dispatch [::set-nuvlabox-cluster nil true])]}))


(reg-event-fx
  ::get-nuvlaboxes-in-clusters
  (fn [_ [_ selected-clusters]]
    {::cimi-api-fx/search
     [:nuvlabox
      {:filter (->> (:resources selected-clusters)
                    (reduce
                      (fn [set-ids c]
                        (set/union
                          set-ids
                          (set (:nuvlabox-managers c))
                          (set (get c :nuvlabox-workers #{}))))
                      #{})
                    general-utils/filter-eq-ids)
       :last   10000}
      #(dispatch [::set-nuvlaboxes-in-clusters %])]}))


(reg-event-fx
  ::set-nuvlaboxes-in-clusters
  (fn [{:keys [db]} [_ nuvlaboxes]]
    (if (instance? js/Error nuvlaboxes)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info nuvlaboxes)]
                   {:header  (cond-> (str "failure getting nuvlaedges")
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
    {:db (assoc db ::spec/ssh-keys-available nil)
     ::cimi-api-fx/search
     [:credential
      {:filter (cond-> (general-utils/filter-eq-subtypes subtypes)
                       additional-filter (general-utils/join-and additional-filter))
       :last   10000}
      #(dispatch [::set-ssh-keys-available %])]}))


(reg-event-fx
  ::enable-host-level-management
  (fn [_ [_ nuvlabox-id]]
    (let [on-success #(dispatch [::set-nuvlabox-playbooks-cronjob %])]
      {::cimi-api-fx/operation [nuvlabox-id "enable-host-level-management" on-success]})))


(reg-event-db
  ::set-nuvlabox-playbooks-cronjob
  (fn [db [_ cronjob]]
    (assoc db ::spec/nuvlabox-playbooks-cronjob cronjob)))

;;
(reg-event-fx
  ::change-view-type
  (fn [{{:keys [current-route]} :db} [_ new-view-type]]
    (let [current-view   (keyword (-> current-route :query-params :view))
          preferred-view {:view new-view-type}]
      {:fx [(when (#{new-view-type current-view} spec/cluster-view)
              [:dispatch [::pagination-plugin/change-page
                          [::spec/pagination] 1]])
            [:dispatch [::routing-events/change-query-param {:partial-query-params preferred-view}]]
            [:dispatch [::store-preferences preferred-view]]]})))

(reg-event-fx
  ::store-preferences
  [(inject-cofx :storage/get {:name spec/local-storage-key})]
  (fn [{storage :storage/get} [_ preference]]
    {:storage/set {:session? false
                   :name     spec/local-storage-key
                   :value    (merge (edn/read-string storage) preference)}}))
(reg-event-fx
  ::set-selected-fleet-timespan
  (fn [{db :db} [_ timespan]]
    {:db (assoc db ::spec/fleet-timespan timespan)
     :fx [[:dispatch [::fetch-fleet-stats]]]}))

(reg-event-fx
  ::fetch-fleet-stats
  (fn [{{:keys [::spec/fleet-timespan current-route] :as db} :db}]
    (let [{:keys [from to]} fleet-timespan
          filter-str (get-full-filter-string db)]
      (when (= (get-query-param current-route :view) (name spec/history-view))
        {:db         (assoc db ::spec/loading? true)
        :http-xhrio {:method          :patch
                     :headers         {:bulk true}
                     :uri             "/api/nuvlabox/data"
                     :format          (ajax/json-request-format)
                     :params          {:filter      filter-str
                                       :dataset     fleet-availability-stats
                                       :from        (time/time->utc-str from)
                                       :to          (time/time->utc-str to)
                                       :granularity (ts-utils/granularity-for-timespan fleet-timespan)}
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success      [::fetch-fleet-stats-success]
                     :on-failure      [::fetch-fleet-stats-failure]}}))))

(reg-event-fx
  ::fetch-fleet-stats-by-edge
  (fn [{db :db} [_ {:keys [from to granularity]}]]
    (let [filter-str (get-full-filter-string db)]
      {:db (assoc db ::spec/loading? true)
       :http-xhrio {:method          :patch
                    :headers         {:bulk true}
                    :uri             "/api/nuvlabox/data"
                    :format          (ajax/json-request-format)
                    :params          {:filter      filter-str
                                      :dataset     ["availability-by-edge"]
                                      :from        (time/time->utc-str from)
                                      :to          (time/time->utc-str to)
                                      :granularity granularity}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::fetch-fleet-stats-success]
                    :on-failure      [::fetch-fleet-stats-failure]}})))

(reg-event-fx
  ::fetch-fleet-stats-success
  (fn [{db :db} [_ {:keys [availability-stats availability-by-edge] :as _response}]]
    {:db (cond-> (assoc db ::spec/loading? false)
                 availability-stats (assoc-in [::spec/fleet-stats :availability-stats] availability-stats)
                 availability-by-edge (assoc-in [::spec/fleet-stats :availability-by-edge] availability-by-edge))}))

(reg-event-fx
  ::fetch-fleet-stats-failure
  (fn [{db :db} [_ response]]
    (let [{:keys [message]} (response/parse response)]
      {:db (assoc db ::spec/loading? false)
       :fx [[:dispatch [::messages-events/add
                        {:header  "Could not fetch NuvlaEdge fleet statistics"
                         :content message
                         :type    :error}]]]})))
