(ns sixsq.nuvla.ui.edges.events
  (:require [clojure.edn :as edn]
            [re-frame.core :refer [dispatch inject-cofx reg-event-db
                                   reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.edges.spec :as spec]
            [sixsq.nuvla.ui.edges.utils :as utils]
            [sixsq.nuvla.ui.i18n.spec :as i18n-spec]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :refer [ordering->order-string] :as table-plugin]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [get-query-param
                                                  get-stored-db-value-from-query-param] :as route-utils]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.session.utils :refer [get-active-claim] :as session-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.response :as response]))

(def refresh-id :nuvlabox-get-nuvlaboxes)
(def refresh-id-non-edit :edges-without-edit-rights)
(def refresh-id-locations :nuvlabox-get-nuvlabox-locations)
(def refresh-id-inferred-locations :nuvlabox-get-nuvlabox-inferred-locations)
(def refresh-summary-id :nuvlabox-get-nuvlaboxes-summary)
(def refresh-id-cluster :nuvlabox-get-nuvlabox-cluster)
(def refresh-id-clusters :nuvlabox-get-nuvlabox-clusters)


(reg-event-fx
  ::init
  [(inject-cofx :storage/all)]
  (fn [{{:keys [current-route] :as db} :db
        storage                        :storage/all}]
    (let [db-path            ::spec/state-selector
          search-query       (get-stored-db-value-from-query-param current-route [db-path])
          filter-storage-key (get-query-param current-route :filter-storage-key)
          storage-filter     (get storage filter-storage-key)
          filter-query       (get-query-param current-route (keyword spec/resource-name))]
      {:db (-> db
               (merge spec/defaults)
               (assoc ::main-spec/loading? true)
               (assoc db-path search-query)
               (assoc ::spec/additional-filter (or storage-filter filter-query)))
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

(defn- get-full-filter-string
  [{:keys [::spec/state-selector
           ::spec/additional-filter] :as db}]
  (general-utils/join-and
    "id!=null"
    (when state-selector (utils/state-filter state-selector))
    additional-filter
    (full-text-search-plugin/filter-text
      db [::spec/edges-search])))

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
          filter          (general-utils/join-and
                            (apply
                              general-utils/join-and
                              (map (fn [role]
                                     (str "acl/edit-meta!='" role "'"))
                                   (session-utils/get-roles session)))
                            selected-filter)]
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
                   {:header  (cond-> (str "failure getting nuvlaboxes")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      {:db (assoc db ::spec/edges-without-edit-rights nuvlaboxes)})))


(reg-event-fx
  ::get-edges-tags
  (fn [_ _]
    {::cimi-api-fx/search
     [:nuvlabox
      {:first       0
       :last        0
       :aggregation "terms:tags"}
      (fn [response]
        (dispatch [::set-edges-tags
                   (->> response
                        :aggregations
                        :terms:tags
                        :buckets
                        (map :key))]))]}))

(reg-event-db
  ::set-edges-tags
  (fn [db [_ tags]]
    (assoc db ::spec/edges-tags tags)))


(reg-event-fx
  ::update-tags
  (fn [{{:keys [::spec/select
                ::i18n-spec/tr] :as db} :db}
       [_ edit-mode {:keys [tags call-back-fn text]}]]
    (let [edit-mode->operation {spec/modal-tags-add-id     "add-tags"
                                spec/modal-tags-remove-all "set-tags"
                                spec/modal-tags-set-id     "set-tags"
                                spec/modal-tags-remove-id  "remove-tags"}
          filter               (table-plugin/build-bulk-filter select (get-full-filter-string db))
          operation            (edit-mode->operation edit-mode)
          updated-tags         (if (= spec/modal-tags-remove-all edit-mode) [] tags)]
      {::cimi-api-fx/operation-bulk [:nuvlabox
                                     (fn [result]
                                       (let [updated     (-> result :updated)
                                             success-msg (str updated " " (tr [(if (< 1 updated) :edges :edge)]) " updated with operation: " text)]
                                         (dispatch [::messages-events/add
                                                    {:header  "Bulk edit operation successful"
                                                     :content success-msg
                                                     :type    :success}])
                                         (dispatch [::table-plugin/set-bulk-edit-success-message
                                                    success-msg
                                                    [::spec/select]])
                                         (dispatch [::table-plugin/reset-bulk-edit-selection [::spec/select]])
                                         (dispatch [::get-nuvlaboxes])
                                         (when (fn? call-back-fn) (call-back-fn (-> result :updated)))))
                                     operation
                                     (when (seq filter) filter)
                                     {:doc {:tags updated-tags}}]})))

(reg-event-fx
  ::set-additional-filter
  (fn [{db :db} [_ filter]]
    {:db (-> db
             (assoc ::spec/additional-filter filter)
             (assoc-in [::spec/pagination :active-page] 1))
     :fx [[:dispatch [::get-nuvlaboxes]]
          [:dispatch [::table-plugin/reset-bulk-edit-selection [::spec/select]]]]}))

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
                     ::main-spec/loading? false)
       :fx [[:dispatch [::get-nuvlaedges-status nuvlaboxes]]]})))


(reg-event-fx
  ::get-nuvlaedges-status
  (fn [_ [_ {nuvlaboxes :resources}]]
    (when (seq nuvlaboxes)
      {::cimi-api-fx/search
       [:nuvlabox-status
        {:select "parent,next-heartbeat,id,nuvlabox-engine-version,online"
         :filter (apply general-utils/join-or
                        (map #(str "parent='" (:id %) "'") nuvlaboxes))}
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
  (fn [{{:keys [::spec/state-selector] :as db} :db} _]
    {::cimi-api-fx/search [:nuvlabox
                           {:first  1
                            :last   10000
                            :select "id,name,online,location,inferred-location"
                            :filter (general-utils/join-and
                                      "(location!=null or inferred-location!=null)"
                                      (when state-selector (utils/state-filter state-selector))
                                      (full-text-search-plugin/filter-text
                                        db [::spec/edges-search]))}
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
  (fn [{db :db}]
    {::cimi-api-fx/search [:nuvlabox
                           (utils/get-query-aggregation-params
                             (full-text-search-plugin/filter-text
                               db [::spec/edges-search])
                             "terms:online,terms:state"
                             nil)
                           #(dispatch [::set-nuvlaboxes-summary %])]}))


(reg-event-fx
  ::set-nuvlabox-cluster-summary
  (fn [{db :db} [_ nuvlaboxes-summary]]
    {:db (assoc db ::spec/nuvlabox-cluster-summary nuvlaboxes-summary)}))


(reg-event-fx
  ::get-nuvlabox-cluster-summary
  (fn [{{:keys [::spec/nuvlabox-cluster] :as db} :db} _]
    {::cimi-api-fx/search [:nuvlabox
                           (utils/get-query-aggregation-params
                             (full-text-search-plugin/filter-text
                               db [::spec/edges-search])
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
                             nil "terms:online,terms:state" nil)
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
            [:dispatch [::table-plugin/reset-bulk-edit-selection [::spec/select]]
             ]]})))


(reg-event-fx
  ::open-modal
  (fn [{db :db} [_ modal-id]]
    (let [fx (when (and ((set spec/tags-modal-ids) modal-id)
                        (not ((set spec/tags-modal-ids) (::spec/open-modal db))))
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
      {:filter (apply general-utils/join-or
                      (map #(str "id='" % "'")
                           (flatten
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

;; TODO: Refactor/move to additional filter or main fx
(reg-event-fx
  ::store-filter-and-open-in-new-tab
  (fn [_ [_ filter-string]]
    (let [uuid (random-uuid)]
      {:storage/set {:session? false
                     :name     uuid
                     :value    filter-string}
       :fx          [[:dispatch
                      [::main-events/open-link
                       (route-utils/name->href
                         {:route-name   ::routes/edges
                          :query-params {:filter-storage-key uuid}})]]]})))
