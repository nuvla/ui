(ns sixsq.nuvla.ui.deployment-sets-detail.events
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.apps.effects :as apps-fx]
            [sixsq.nuvla.ui.apps-store.events :as apps-store-events]
            [sixsq.nuvla.ui.apps-store.spec :as apps-store-spec]
            [sixsq.nuvla.ui.apps.spec :refer [nonblank-string]]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.deployment-sets-detail.spec :as spec]
            [sixsq.nuvla.ui.deployment-sets-detail.subs :as subs]
            [sixsq.nuvla.ui.deployments.events :as deployments-events]
            [sixsq.nuvla.ui.deployments.spec :as deployments-spec]
            [sixsq.nuvla.ui.deployments.utils :as deployments-utils]
            [sixsq.nuvla.ui.edges.spec :as edges-spec]
            [sixsq.nuvla.ui.edges.utils :as edge-utils]
            [sixsq.nuvla.ui.job.events :as job-events]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.plugins.events :as events-plugin]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search]
            [sixsq.nuvla.ui.plugins.module :as module-plugin :refer [get-version-id]]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :refer [ordering->order-string]]
            [sixsq.nuvla.ui.plugins.target-selector :as target-selector]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.deployment-sets-detail.utils :as utils]
            [sixsq.nuvla.ui.routing.utils :as routing-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.response :as response]))

(def refresh-action-depl-set-id :deployment-set)
(def refresh-action-deployments-id :deployment-set-get-deployments)

(defn uuid->depl-set-id [uuid]
  (if-not (str/starts-with? uuid "deployment-set/")
    (str "deployment-set/" uuid)
    uuid))

(defn get-target-fleet-ids
  [depl-set]
  (->> depl-set
       :applications-sets
       (mapcat :overwrites)
       (mapcat (juxt :targets :fleet))
       (remove nil?)
       flatten))

(reg-event-fx
  ::refresh
  (fn [{{:keys [current-route]} :db}]
    (let [uuid (-> current-route :path-params :uuid)]
      {:fx [[:dispatch
             [::main-events/action-interval-start
              {:id        refresh-action-depl-set-id
               :frequency 10000
               :event     [::get-deployment-set (uuid->depl-set-id uuid)]}]]
            [:dispatch
             [::main-events/action-interval-start
              {:id        refresh-action-deployments-id
               :frequency 10000
               :event     [::get-deployments-for-deployment-sets (uuid->depl-set-id uuid)]}]]]})))

(defn refresh
  []
  (dispatch [::refresh]))

(reg-event-db
  ::clear-target-edges
  (fn [db]
    (dissoc db ::spec/edges ::spec/edges-documents)))

(reg-event-fx
  ::init
  (fn []
    {:fx [[:dispatch [::clear-target-edges]]
          [:dispatch [::main-events/action-interval-delete {:id refresh-action-depl-set-id}]]
          [:dispatch [::main-events/action-interval-delete {:id refresh-action-deployments-id}]]
          [:dispatch [::refresh]]
          [:dispatch [::main-events/changes-protection? false]]]}))

(reg-event-fx
  ::clear-deployments
  (fn []
    {:fx [[:dispatch [::deployments-events/reset-deployments-summary-all]]]}))

(reg-event-fx
  ::init-create
  (fn [{db :db}]
    {:db (merge db spec/defaults)
     :fx [[:dispatch [::clear-deployments]]
          [:dispatch [::main-events/action-interval-delete
                      {:id refresh-action-depl-set-id}]]]}))

(reg-event-fx
  ::new
  (fn [{{:keys [current-route] :as db} :db}]
    (let [id (routing-utils/get-query-param current-route :applications-sets)]
      {:db (merge db spec/defaults)
       :fx [[:dispatch [::get-application-sets id]]]})))

(reg-event-fx
  ::get-application-sets
  (fn [_ [_ id]]
    {::cimi-api-fx/get [id #(dispatch [::set-applications-sets %])]}))


(defn restore-applications
  [db [i]]
  (assoc-in db [::spec/apps-sets i ::spec/targets]
            (target-selector/build-spec)))

(defn load-module-configurations
  [modules-by-id fx [id {:keys [applications]}]]
  (->> applications
       (map (fn [{module-id :id :keys [version
                                       environmental-variables
                                       registries-credentials]}]
              (when (get modules-by-id module-id)
                [:dispatch [::module-plugin/load-module
                            [::spec/apps-sets id]
                            (str module-id "_" version)
                            {:env                    (when (seq environmental-variables)
                                                       (->> environmental-variables
                                                            (map (juxt :name :value))
                                                            (into {})))
                             :registries-credentials registries-credentials}]])))
       (concat fx)))

(defn- merge-vector-of-maps
  [scn prm]
  (vals (merge
          (into {} (map (juxt :name identity) scn))
          (into {} (map (juxt :name identity) prm)))))

(defn- merge-app-overwrites
  [modul-id->app app]
  (let [app-from-depl-set (get modul-id->app (:id app))
        merged-env        (merge-vector-of-maps
                            (:environmental-variables app)
                            (:environmental-variables app-from-depl-set))]
    (assoc
      (merge app app-from-depl-set)
      :environmental-variables merged-env)))

(reg-event-fx
  ::load-apps-sets-response
  (fn [{:keys [db]} [_ {:keys [apps-sets-set total-apps-count apps apps-set-index->modul-id->app]}]]
    (let [modules-by-id     (->> apps (map (juxt :id identity)) (into {}))
          apps-sets         (->> apps-sets-set
                                 :content
                                 :applications-sets
                                 (map-indexed vector))
          merged-configs    (mapv (fn [[idx app-set]]
                                    [idx (assoc app-set :applications
                                                        (mapv
                                                          (partial merge-app-overwrites (apps-set-index->modul-id->app idx))
                                                          (:applications app-set)))])
                                  apps-sets)
          new-db            (reduce restore-applications
                                    db merged-configs)
          fx                (reduce (partial load-module-configurations modules-by-id)
                                    [] merged-configs)
          all-apps-visible? (= total-apps-count (count apps))]
      (if all-apps-visible?
        {:db new-db
         :fx fx}
        {:fx [[:dispatch [::messages-events/add
                          {:header  "Unable to load selected applications sets"
                           :content (str "Loaded " (count apps) " out of " total-apps-count ".")
                           :type    :error}]]]}))))

(reg-event-fx
  ::load-apps-sets
  (fn [{{:keys [::spec/deployment-set]} :db} [_ apps-sets]]
    (let [app-sets-by-app-set-id (->> deployment-set
                                      :applications-sets
                                      (map (juxt :id identity))
                                      (into {}))
          apps-urls              (->> apps-sets
                                      :content
                                      :applications-sets
                                      (mapcat :applications)
                                      (map :id)
                                      distinct)
          filter-str             (apply general-utils/join-or (map #(str "id='" % "'") apps-urls))
          params                 {:filter filter-str
                                  :last   1000}
          callback               #(if (instance? js/Error %)
                                    (cimi-api-fx/default-error-message % "load applications sets failed")
                                    (dispatch [::load-apps-sets-response {:apps-sets-set    apps-sets
                                                                          :total-apps-count (count apps-urls)
                                                                          :apps             (:resources %)
                                                                          :apps-set-index->modul-id->app
                                                                          (->> (app-sets-by-app-set-id
                                                                                 (:id apps-sets))
                                                                               :overwrites
                                                                               (map :applications)
                                                                               (map-indexed (fn [idx apps]
                                                                                              [idx (zipmap
                                                                                                     (map :id apps)
                                                                                                     apps)]))
                                                                               (into {}))}]))]
      (when (seq apps-urls)
        {::cimi-api-fx/search [:module params callback]}))))

(reg-event-fx
  ::set-applications-sets
  (fn [{:keys [db]} [_ {:keys [subtype] :as apps-sets}]]
    (if (apps-utils/applications-sets? subtype)
      {:db (assoc db ::spec/module-applications-sets apps-sets)
       :fx [[:dispatch [::load-apps-sets apps-sets]]]}
      {:dispatch [::messages-events/add
                  {:header  "Wrong module subtype"
                   :content (str "Selected module subtype:" subtype)
                   :type    :error}]})))

(reg-event-fx
  ::set-deployment-set
  (fn [{:keys [db]} [_ deployment-set fx]]
    (let [parent-ids (get-target-fleet-ids deployment-set)]
      {:db (assoc db ::spec/deployment-set-not-found? (nil? deployment-set)
                     ::spec/deployment-set deployment-set
                     ::main-spec/loading? false)
       :fx [fx
            [:dispatch [::resolve-to-ancestor-resource
                        {:ids                    parent-ids
                         :storage-event          ::set-edges
                         :ancestor-resource-name "nuvlabox"}]]
            [:dispatch [::get-application-sets (-> deployment-set :applications-sets first :id)]]]})))

(reg-event-fx
  ::operation
  (fn [_ [_ {:keys [resource-id operation data on-success-fn on-error-fn]
             :or   {data          {}
                    on-success-fn #(dispatch [::bulk-progress-plugin/monitor
                                              [::spec/bulk-jobs] (:location %)])
                    on-error-fn   #()}}]]
    (let [on-success #(do
                        (refresh)
                        (let [{:keys [status message]} (response/parse %)]
                          (dispatch [::messages-events/add
                                     {:header  (cond-> (str "operation " operation " will be executed soon")
                                                       status (str " (" status ")"))
                                      :content message
                                      :type    :success}]))
                        (on-success-fn %))
          on-error   #(do
                        (refresh)
                        (cimi-api-fx/default-operation-on-error resource-id operation %)
                        (on-error-fn))]
      {::cimi-api-fx/operation [resource-id operation on-success
                                :on-error on-error :data data]})))

(reg-event-fx
  ::get-deployment-set
  (fn [{{:keys [::spec/deployment-set] :as db} :db} [_ id fx]]
    {:db               (cond-> db
                               (not= (:id deployment-set) id) (merge spec/defaults))
     ::cimi-api-fx/get [id #(dispatch [::set-deployment-set % fx])
                        :on-error #(dispatch [::set-deployment-set nil])]
     :fx               [[:dispatch [::events-plugin/load-events [::spec/events] id]]
                        [:dispatch [::job-events/get-jobs id]]]}))

(def deployments-state-filter-key :depl-state)

(reg-event-fx
  ::get-deployments-for-deployment-sets
  (fn [{{:keys [current-route]} :db} [_ uuid]]
    (when uuid
      (let [query-filter      (routing-utils/get-query-param current-route deployments-state-filter-key)
            filter-constraint (str "deployment-set='" (uuid->depl-set-id uuid) "'")]
        {:fx [[:dispatch [::deployments-events/get-deployments
                          {:filter-external-arg   (general-utils/join-and
                                                    filter-constraint
                                                    (deployments-utils/state-filter query-filter))
                           :external-filter-only? true
                           :pagination-db-path    ::spec/pagination-deployments}]]
              [:dispatch [::deployments-events/get-deployments-summary-all filter-constraint]]]}))))

(reg-event-db
  ::set-deployment-set-edited
  (fn [db [_ deployment-set-edited]]
    (assoc db ::spec/deployment-set-edited deployment-set-edited)))

(reg-event-fx
  ::edit
  (fn [{{:keys [::spec/deployment-set
                ::spec/deployment-set-edited] :as db} :db} [_ key value]]
    (let [updated-deployment-set (-> deployment-set
                                     (merge deployment-set-edited)
                                     (assoc key value))]
      {:fx [[:dispatch [::set-deployment-set-edited updated-deployment-set]]
            [:dispatch [::main-events/changes-protection?
                        (utils/unsaved-changes?
                          deployment-set updated-deployment-set)]]]})))

(reg-event-fx
  ::persist!
  (fn [_ [_ {:keys [deployment-set success-msg]}]]
    (let [resource-id (:id deployment-set)]
      {::cimi-api-fx/edit
       [resource-id deployment-set
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
             (dispatch [::set-deployment-set-edited nil])
             (dispatch [::set-deployment-set %])
             (dispatch [::main-events/changes-protection? false])))]})))

(reg-event-fx
  ::delete
  (fn [{{:keys [::spec/deployment-set]} :db}]
    (let [id (:id deployment-set)]
      {::cimi-api-fx/delete [id #(dispatch [::routing-events/navigate routes/deployment-sets])]})))

(defn application-overwrites
  [db i {:keys [id version] :as _application}]
  (let [db-path     [::spec/apps-sets i]
        env-changed (module-plugin/db-changed-env-vars db db-path id)
        regs-creds  (module-plugin/db-module-registries-credentials
                      db db-path id)]
    (cond-> {:id      id
             :version version}
            (seq env-changed) (assoc :environmental-variables env-changed)
            (seq regs-creds) (assoc :registries-credentials regs-creds))))


(defn applications-sets->overwrites
  [db i {:keys [applications] :as _applications-sets}]
  (let [targets                 (subs/get-db-targets-selected-ids db i)
        fleet                   (get-target-fleet-ids (get db ::spec/deployment-set))
        applications-overwrites (map (partial application-overwrites db i)
                                     applications)]
    (cond-> {}
            (seq targets) (assoc :targets targets)
            (seq fleet) (assoc :fleet fleet)
            (seq applications-overwrites) (assoc :applications applications-overwrites))))

(reg-event-fx
  ::save-start
  (fn [{{:keys [::spec/create-name
                ::spec/create-description
                ::spec/module-applications-sets] :as db} :db} [_ start?]]
    (let [body (cond->
                 {:name              create-name
                  :applications-sets [{:id         (:id module-applications-sets)
                                       :version    (apps-utils/module-version module-applications-sets)
                                       :overwrites (map-indexed (partial applications-sets->overwrites db)
                                                                (-> module-applications-sets :content :applications-sets))}]
                  :start             start?}
                 (not (str/blank? create-description)) (assoc :description create-description))]
      {::cimi-api-fx/add
       [:deployment-set body
        #(dispatch [::routing-events/navigate routes/deployment-sets-details
                    {:uuid (general-utils/id->uuid (:resource-id %))}])]})))

(reg-event-fx
  ::create
  (fn [{{:keys [current-route
                ::spec/deployment-set-edited] :as db} :db}]
    (let [edges-path (subs/current-route->edges-db-path current-route)
          apps-path  (subs/create-apps-creation-db-path current-route)
          body       (merge {:fleet   (:resources (get-in db edges-path))
                             :modules (map
                                        (fn [app] (str (:id app) "_" (:version app)))
                                        (get-in db apps-path))}
                            deployment-set-edited)]
      {:fx [[:dispatch [::main-events/changes-protection? false]]
            [::cimi-api-fx/add
             [:deployment-set body
              #(do
                 (dispatch [::set-deployment-set-edited nil])
                 (dispatch [::routing-events/navigate routes/deployment-sets-details
                            {:uuid (general-utils/id->uuid (:resource-id %))}]))
              :on-error #(dispatch [::main-events/changes-protection? true])]]]})))

(reg-event-db
  ::set
  (fn [db [_ k v]]
    (assoc db k v)))

(reg-event-db
  ::remove-target
  (fn [db [_ i target-id]]
    (update-in db [::spec/apps-sets i ::spec/targets-selected] dissoc target-id)))

(reg-event-fx
  ::set-targets-selected
  (fn [{db :db} [_ i db-path]]
    (let [selected (target-selector/db-selected db db-path)]
      {:db (->> selected
                (map (juxt :id identity))
                (into {})
                (assoc-in db [::spec/apps-sets i ::spec/targets-selected]))})))

(reg-event-fx
  ::resolve-to-ancestor-resource
  (fn [{{:keys [::spec/edges]} :db} [_ {:keys [ids storage-event
                                               ancestor-resource-name]}]]
    (when-not edges
      (let [ancestor-ids              (filterv #(str/starts-with? % ancestor-resource-name) ids)
            descendant-ids            (vec (remove (set ancestor-ids) ids))
            resolved?                 (empty? descendant-ids)
            callback                  (fn [response]
                                        (let [resources  (:resources response)
                                              parent-ids (remove nil? (map :parent resources))]
                                          (cond
                                            (or (empty? resources) (instance? js/Error response))
                                            (cimi-api-fx/default-error-message response "loading edges for credentials failed")

                                            resolved?
                                            (dispatch [storage-event response])

                                            :else
                                            (dispatch [::resolve-to-ancestor-resource
                                                       {:ids                    (into ancestor-ids parent-ids)
                                                        :storage-event          storage-event
                                                        :ancestor-resource-name ancestor-resource-name}]))))
            ids-to-query              (if resolved? ancestor-ids descendant-ids)
            next-parent-resource-name (general-utils/id->resource-name (first ids-to-query))
            ids-filter                (general-utils/ids->inclusion-filter-string ids-to-query)]
        (when (every? seq [next-parent-resource-name ids-filter])
          {::cimi-api-fx/search [next-parent-resource-name
                                 (cond->
                                   {:filter ids-filter
                                    :last   10000
                                    :select "id, parent"}
                                   (= "nuvlabox" next-parent-resource-name)
                                   (merge {:aggregation edges-spec/state-summary-agg-term}))
                                 callback]})))))

(reg-event-fx
  ::set-edges
  (fn [{{:keys [current-route] :as db} :db} [_ response]]
    (let [path (subs/current-route->edges-db-path current-route)]
      {:db (assoc-in db path
                     (update response :resources #(mapv :id %)))
       :fx [[:dispatch [::get-edge-documents]]]})))

(def edges-state-filter-key :edges-state)

(reg-event-fx
  ::get-edge-documents
  (fn [{{:keys [::spec/ordering
                current-route] :as db} :db} _]
    (let [edges        (get-in db
                               (subs/current-route->edges-db-path
                                 current-route))
          ordering     (or ordering spec/default-ordering)
          query-filter (routing-utils/get-query-param current-route edges-state-filter-key)]
      (cond-> {:db (assoc db ::spec/edge-documents nil)}
        edges
        (assoc ::cimi-api-fx/search
          [:nuvlabox
           (->> {:orderby (ordering->order-string ordering)
                 :filter  (general-utils/join-and
                            "id!=null"
                            (general-utils/ids->inclusion-filter-string (-> edges
                                                                            :resources))
                            (when (seq query-filter) (edge-utils/state-filter query-filter)))}
                (pagination-plugin/first-last-params
                  db [::spec/pagination-edges]))
           #(dispatch [::set-edges-documents %])])))))

(reg-event-db
  ::set-opened-modal
  (fn [db [_ id]]
    (assoc db ::spec/opened-modal id)))

(reg-event-db
  ::set-edges-documents
  (fn [db [_ edges-response]]
    (assoc db ::spec/edges-documents edges-response)))


(reg-event-fx
  ::fetch-app-picker-apps
  (fn [{{:keys [current-route] :as db} :db} [_ pagination-db-path]]
    (let [current-selection (get-in db (subs/create-apps-creation-db-path current-route))]
      {:fx [[:dispatch [::apps-store-events/get-modules
                        apps-store-spec/allapps-key
                        {:external-filter
                         (general-utils/join-and
                           (general-utils/ids->exclude-filter-str
                             (map :id current-selection))
                           (when (seq current-selection)
                             "subtype!='applications_sets'"))
                         :order-by "name:asc"
                         :pagination-db-path [pagination-db-path]}]]]})))

(defn enrich-app
  [app]
  (let [versions (:versions app)
        version-id (-> app :content :id)
        version-no (get-version-id (map-indexed vector versions) version-id)]
    (assoc app :version version-no)))

(reg-event-fx
  ::do-add-app-from-picker
  (fn [{{:keys [current-route] :as db} :db} [_ app]]
    (let [db-path                 (subs/create-apps-creation-db-path current-route)
          app-with-version-number (enrich-app app)]
      {:db (update-in db db-path (fnil conj []) app-with-version-number)
       :fx [[:dispatch [::fetch-app-picker-apps
                        ::spec/pagination-apps-picker]]]})))

(defn version-id-to-add
  [app]
  (let [versions (:versions app)
        published-versions (filter (comp true? :published) (:versions app))]
    (:href (or (last published-versions) (last versions)))))

(reg-event-fx
  ::add-app-from-picker
  (fn [_ [_ app]]
    (if (= (:subtype app)
           "applications_sets")
      {::cimi-api-fx/get [(:id app) #(dispatch [::add-apps-set-apps %])]}
      (let [{:keys [path versions]} app
            version-id (version-id-to-add app)]
        {::apps-fx/get-module [path
                               (get-version-id (map-indexed vector versions) version-id)
                               #(dispatch [::do-add-app-from-picker %])]
         :fx [[:dispatch [::fetch-app-picker-apps
                          ::spec/pagination-apps-picker]]]}))))

(reg-event-fx
  ::add-apps-set-apps
  (fn [_ [_ apps-sets]]
    (let [app-ids (->> apps-sets
                       :content
                       :applications-sets
                       (mapcat :applications)
                       (map :id))]
      {::cimi-api-fx/search
       [:module
        {:filter (general-utils/ids->inclusion-filter-string
                   app-ids)}
        #(dispatch [::add-apps-to-selection (:resources %)])]})))

(reg-event-fx
  ::add-apps-to-selection
  (fn [{db :db} [_ apps]]
    (let [db-path  (subs/create-apps-creation-db-path (:current-route db))
          apps (mapv enrich-app apps)]
      {:db (update-in db db-path (fnil into []) apps)
       :fx [[:dispatch [::fetch-app-picker-apps
                        ::spec/pagination-apps-picker]]]})))

(reg-event-fx
  ::remove-app-from-creation-data
  (fn [{{:keys [current-route] :as db} :db} [_ app]]
    (let [db-path (subs/create-apps-creation-db-path current-route)]
      {:db (update-in db db-path (fn [apps]
                                   (vec (remove #(= (:id %) (:href app)) apps))))
       :fx [[:dispatch [::fetch-app-picker-apps
                        ::spec/pagination-apps-picker]]]})))

(reg-event-fx
  ::edit-config
  (fn [{{:keys [::spec/module-applications-sets] :as db} :db}]
    {:fx [[:dispatch [::edit :applications-sets
                      [{:id         (:id module-applications-sets)
                        :version    (apps-utils/module-version module-applications-sets)
                        :overwrites (map-indexed (partial applications-sets->overwrites db)
                                                 (-> module-applications-sets :content :applications-sets))}]]]]}))
