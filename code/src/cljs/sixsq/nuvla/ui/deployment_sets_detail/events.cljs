(ns sixsq.nuvla.ui.deployment-sets-detail.events
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.apps-store.events :as apps-store-events]
            [sixsq.nuvla.ui.apps-store.spec :as apps-store-spec]
            [sixsq.nuvla.ui.apps.effects :as apps-fx]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.deployment-sets-detail.spec :as spec]
            [sixsq.nuvla.ui.deployment-sets-detail.subs :as subs :refer [get-target-fleet-ids]]
            [sixsq.nuvla.ui.deployment-sets-detail.utils :as utils]
            [sixsq.nuvla.ui.deployments.events :as deployments-events]
            [sixsq.nuvla.ui.deployments.utils :as deployments-utils]
            [sixsq.nuvla.ui.edges.spec :as edges-spec]
            [sixsq.nuvla.ui.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.job.events :as job-events]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.plugins.events :as events-plugin]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.module :as module-plugin :refer [get-version-id]]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :as table-plugin :refer [ordering->order-string]]
            [sixsq.nuvla.ui.plugins.target-selector :as target-selector]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :as routing-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.response :as response]))

(def refresh-action-depl-set-id :deployment-set)
(def refresh-action-deployments-id :deployment-set-get-deployments)

(defn uuid->depl-set-id [uuid]
  (if-not (str/starts-with? uuid "deployment-set/")
    (str "deployment-set/" uuid)
    uuid))

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

(reg-event-fx
  ::init
  (fn [{:keys [db]}]
    {:db (merge db spec/defaults)
     :fx [[:dispatch [::main-events/action-interval-delete {:id refresh-action-depl-set-id}]]
          [:dispatch [::main-events/action-interval-delete {:id refresh-action-deployments-id}]]
          [:dispatch [::refresh]]
          [:dispatch [::main-events/changes-protection? false]]
          [:dispatch [::disable-form-validation]]]}))

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
  [db modules-by-id fx [id {:keys [applications]}]]
  (->> applications
       (map (fn [{module-id :id :keys [version
                                       environmental-variables
                                       registries-credentials]}]
              (when (and (get modules-by-id module-id)
                         (nil? (module-plugin/db-module db [::spec/apps-sets id] module-id)))
                [:dispatch [::module-plugin/load-module
                            [::spec/apps-sets id]
                            (str module-id "_" version)
                            {:env                    (when (seq environmental-variables)
                                                       (->> environmental-variables
                                                            (map (juxt :name :value))
                                                            (into {})))
                             :registries-credentials registries-credentials}
                            [::init-app-row-data false]]])))
       (concat fx)))

(reg-event-fx
  ::load-module-configuration
  (fn [{:keys [db]} [_ app-set-idx {module-id :id :keys [version]}]]
    (when (nil? (module-plugin/db-module db [::spec/apps-sets app-set-idx] module-id))
      {:fx [[:dispatch [::module-plugin/load-module
                        [::spec/apps-sets app-set-idx]
                        (str module-id "_" version)
                        {}
                        [::init-app-row-data false]]]]})))

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
          fx                (reduce (partial load-module-configurations db modules-by-id)
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
    (let [deployment-set-edited (get db ::spec/deployment-set-edited)]
      {:db (assoc db ::spec/deployment-set-not-found? (nil? deployment-set)
                     ::spec/deployment-set deployment-set
                     ::main-spec/loading? false
                     ::spec/deployment-set-edited (if (some? deployment-set-edited)
                                                    deployment-set-edited
                                                    deployment-set)
                     ::spec/fleet-filter (-> deployment-set :applications-sets first :overwrites first :fleet-filter))
       :fx [fx
            [:dispatch [::get-edges]]
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
  (fn [_ [_ id fx]]
    {::cimi-api-fx/get [id #(dispatch [::set-deployment-set % fx])
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
             (dispatch [::set-deployment-set-edited %])
             (dispatch [::set-apps-edited false])
             (dispatch [::set-deployment-set %])
             (dispatch [::main-events/changes-protection? false])
             (dispatch [::disable-form-validation])))]})))

(reg-event-fx
  ::recompute-fleet
  (fn [{{:keys [::spec/deployment-set] :as db} :db} [_ on-complete]]
    (let [id         (:id deployment-set)
          on-success (fn []
                       (when on-complete (on-complete))
                       (refresh))]
      {:db (dissoc db ::spec/deployment-set-edited)
       ::cimi-api-fx/operation
       [id "recompute-fleet" on-success
        :on-error #(cimi-api-fx/default-error-message
                     %
                     "Failed to recompute fleet")]})))

(reg-event-fx
  ::delete
  (fn [{{:keys [::spec/deployment-set]} :db} [_ {:keys [forceable? deletable?]}]]
    (let [id (:id deployment-set)
          cb (fn [response]
               (dispatch
                 [::job-events/wait-job-to-complete
                  {:job-id              (:location response)
                   :refresh-interval-ms 1000
                   :on-complete
                   #(do
                      (dispatch [::routing-events/navigate routes/deployment-sets])
                      (when-not (= "SUCCESS" (:state %))
                        (cimi-api-fx/default-error-message
                          %
                          "Failed to delete deployment set")) ())}]))]
      (cond
        deletable?
        {::cimi-api-fx/delete [id cb]}
        forceable?
        {::cimi-api-fx/operation [id "force-delete" cb]}))))

(defn merge-env-vars
  [env-vars1 env-vars2]
  (let [m1 (->> env-vars1
                (map (juxt :name :value))
                (into {}))
        m2 (->> env-vars2
                (map (juxt :name :value))
                (into {}))]
    (->> (merge m1 m2)
         (mapv (fn [[k v]] {:name k :value v})))))

(defn application-overwrites
  [db i {:keys [id version] :as _application} current-overwrites]
  (let [db-path         [::spec/apps-sets i]
        version-changed (module-plugin/db-new-version db db-path id)
        env-changed     (merge-env-vars
                          (:environmental-variables current-overwrites)
                          (module-plugin/db-changed-env-vars db db-path id))
        regs-creds      (module-plugin/db-module-registries-credentials
                          db db-path id)]
    (cond-> {:id      id
             :version (or version-changed (:version current-overwrites) version)}
            (seq env-changed) (assoc :environmental-variables env-changed)
            (seq regs-creds) (assoc :registries-credentials regs-creds))))


(defn applications-sets->overwrites
  [db i {:keys [applications] :as _applications-sets} current-overwrites]
  (let [targets                 (subs/get-db-targets-selected-ids db i)
        fleet                   (get-target-fleet-ids (get db ::spec/deployment-set))
        fleet-filter            (get db ::spec/fleet-filter)
        applications-overwrites (map (fn [[app current-app-overwrites]]
                                       (application-overwrites db i app current-app-overwrites))
                                     (map vector
                                          applications
                                          (concat (:applications current-overwrites) (repeat nil))))]
    (cond-> {}
            (seq targets) (assoc :targets targets)
            (seq fleet) (assoc :fleet fleet)
            fleet-filter (assoc :fleet-filter fleet-filter)
            (seq applications-overwrites) (assoc :applications applications-overwrites))))

(reg-event-fx
  ::save-start
  (fn [{{:keys [::spec/create-name
                ::spec/create-description
                ::spec/module-applications-sets
                ::spec/deployment-set] :as db} :db} [_ start?]]
    (let [body (cond->
                 {:name              create-name
                  :applications-sets [{:id         (:id module-applications-sets)
                                       :version    (apps-utils/module-version module-applications-sets)
                                       :overwrites (map-indexed
                                                     (fn [i [app-set current-overwrites]]
                                                       (applications-sets->overwrites db i app-set current-overwrites))
                                                     (map vector
                                                          (-> module-applications-sets :content :applications-sets)
                                                          (concat (get-in deployment-set [:applications-sets 0 :overwrites])
                                                                  (repeat nil))))}]
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
    (let [edges-path   (subs/current-route->edges-db-path current-route)
          fleet-filter (get-in db (subs/current-route->fleet-filter-db-path current-route))
          apps-path    (subs/create-apps-creation-db-path current-route)
          body         (merge {:fleet   (:resources (get-in db edges-path))
                               :modules (map
                                          (fn [app] (str (:id app) "_" (:version app)))
                                          (get-in db apps-path))}
                              (when fleet-filter {:fleet-filter fleet-filter})
                              deployment-set-edited)]
      {:fx [[:dispatch [::main-events/changes-protection? false]]
            [::cimi-api-fx/add
             [:deployment-set body
              #(do
                 (dispatch [::routing-events/navigate routes/deployment-sets-details
                            {:uuid (general-utils/id->uuid (:resource-id %))}]))
              :on-error #(dispatch [::main-events/changes-protection? true])]]]})))

(defn overwritten-app-version
  [deployment-set app]
  (->> (get-in deployment-set [:applications-sets 0 :overwrites])
       first
       :applications
       (filter #(= (:id %) (:id app)))
       first
       :version))

(defn overwritten-app-env-vars
  [deployment-set app]
  (tap> [:overwritten-app-env-vars
         app
         deployment-set
         (->> (get-in deployment-set [:applications-sets 0 :overwrites])
              first
              :applications
              (filter #(= (:id %) (:id app)))
              first
              :environmental-variables)])
  (->> (get-in deployment-set [:applications-sets 0 :overwrites])
       first
       :applications
       (filter #(= (:id %) (:id app)))
       first
       :environmental-variables))

(defn new-overwrites
  [deployment-set apps]
  (map (fn [app]
         (let [env-vars (overwritten-app-env-vars deployment-set app)]
           (cond->
             {:id      (:id app)
              :version (or (:version app)
                           (overwritten-app-version deployment-set app))}
             (seq env-vars) (assoc :environmental-variables env-vars))))
       apps))

(defn new-modules
  [deployment-set apps]
  (map
    (fn [app] (str (:id app) "_" (or (:version app)
                                     (overwritten-app-version deployment-set app))))
    apps))

(reg-event-fx
  ::do-edit
  (fn [{{:keys [current-route ::spec/edges ::spec/apps-edited? ::spec/deployment-set-edited] :as db} :db} [_ {:keys [deployment-set success-msg]}]]
    (let [apps-path    (subs/create-apps-creation-db-path current-route)
          apps         (get-in db apps-path)
          fleet-filter (get-in db (subs/current-route->fleet-filter-db-path current-route))
          body         (merge (when apps-edited?
                                (merge
                                  {:fleet      (:resources edges)
                                   :modules    (new-modules deployment-set-edited apps)
                                   :overwrites (new-overwrites deployment-set-edited apps)}
                                  (when fleet-filter {:fleet-filter fleet-filter})))
                              deployment-set)]
      {:fx [[:dispatch [::persist! {:deployment-set body
                                    :success-msg    success-msg}]]]})))

(reg-event-db
  ::init-app-row-data
  (fn [{:keys [current-route ::spec/module-applications-sets ::spec/apps-edited?] :as db} [_ creating?]]
    (when (and (not creating?) (not apps-edited?))
      (let [apps-path (subs/create-apps-creation-db-path current-route)]
        (update-in db apps-path (constantly (->> module-applications-sets
                                                 :content
                                                 :applications-sets
                                                 (map-indexed
                                                   (fn [i {:keys [applications]}]
                                                     (map (fn [{:keys [id]}]
                                                            (module-plugin/db-module db [::spec/apps-sets i] id)
                                                            ) applications)))
                                                 (apply concat)
                                                 vec)))))))

(reg-event-db
  ::set-apps-edited
  (fn [db [_ apps-edited?]]
    (assoc db ::spec/apps-edited? apps-edited?)))

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
  ::get-edges
  (fn [{{:keys [::spec/edges-ordering
                ::spec/deployment-set
                ::spec/deployment-set-edited
                ::spec/edges-additional-filter] :as db} :db} _]
    (let [callback   (fn [response]
                       (dispatch [::set-edges response]))
          ids-filter (general-utils/ids->inclusion-filter-string
                      (set (into (get-target-fleet-ids deployment-set) (get-target-fleet-ids deployment-set-edited))))]
      {::cimi-api-fx/search [:nuvlabox
                             {:filter     (general-utils/join-and
                                            ids-filter
                                            edges-additional-filter
                                            (full-text-search-plugin/filter-text
                                              db [::spec/edges-full-text-search]))
                              :last        10000
                              :select      "id"
                              :aggregation edges-spec/state-summary-agg-term
                              :orderby (ordering->order-string edges-ordering)}
                             callback]})))

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
  (fn [{{:keys [::spec/edges-ordering
                current-route
                ::spec/changed-edges
                ::spec/edges-additional-filter] :as db} :db} _]
    (let [edges        (get-in db
                         (subs/current-route->edges-db-path
                           current-route))
          ordering     (or edges-ordering spec/default-ordering)
          state-filter (routing-utils/get-query-param current-route edges-state-filter-key)
          changed-ids  (remove nil? (flatten (vals changed-edges)))]
      (cond-> {:db (assoc db ::spec/edge-documents nil)}
              edges
              (assoc ::cimi-api-fx/search
                     [:nuvlabox
                      (->> {:orderby (ordering->order-string ordering)
                            :filter  (if (seq changed-ids)
                                       (general-utils/join-and
                                         (general-utils/ids->inclusion-filter-string
                                           changed-ids)
                                         edges-additional-filter
                                         (full-text-search-plugin/filter-text
                                           db [::spec/edges-full-text-search])
                                         (when (seq state-filter)
                                           (edges-utils/state-filter state-filter)))
                                       (general-utils/join-and
                                         "id!=null"
                                         (general-utils/ids->inclusion-filter-string (-> edges
                                                                                         :resources))
                                         (when (seq state-filter)
                                           (edges-utils/state-filter state-filter))))}
                           (pagination-plugin/first-last-params
                             db [::spec/edges-pagination]))
                      #(dispatch [::set-edges-documents %])])))))

(reg-event-fx
  ::set-fleet-filter
  (fn [{db :db} [_ fleet-filter deployment-set-id]]
    (let [path (subs/create-db-path [::spec/fleet-filter] (str deployment-set-id))]
      {:db (assoc-in db path fleet-filter)})))





(reg-event-fx
  ::init-edges-tab
  (fn [{db :db}]
    {:db (assoc db ::spec/changed-edges nil)
     :fx [[:dispatch [::get-edge-documents]]]}))

(def apps-picker-modal-id :modal/add-apps)
(def edges-picker-modal-id :modal/add-edges)

(reg-event-fx
  ::set-opened-modal
  (fn [{db :db} [_ id]]
    {:db (assoc db ::spec/opened-modal id)
     :fx (if (= id edges-picker-modal-id)
           [[:dispatch [::get-edges-for-edge-picker-modal]]]
           [[:dispatch [::reset-edge-picker]]])}))

(reg-event-db
  ::set-edges-documents
  (fn [db [_ edges-response]]
    (assoc db ::spec/edges-documents edges-response)))

(reg-event-fx
  ::set-edges-additional-filter
  (fn [{db :db} [_ filter]]
    {:db (-> db
             (assoc ::spec/edges-additional-filter filter)
             (assoc-in [::spec/edges-pagination :active-page] 1))
     :fx [[:dispatch [::get-edges]]
          [:dispatch [::table-plugin/reset-bulk-edit-selection [::spec/edges-select]]]]}))

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
                         :order-by           "name:asc"
                         :pagination-db-path [pagination-db-path]}]]]})))

(defn enrich-app
  [app]
  (let [versions   (:versions app)
        version-id (-> app :content :id)
        version-no (get-version-id (map-indexed vector versions) version-id)]
    (assoc app :version version-no)))

(reg-event-fx
  ::do-add-app
  (fn [{{:keys [current-route] :as db} :db} [_ app]]
    (let [db-path                 (subs/create-apps-creation-db-path current-route)
          app-with-version-number (enrich-app app)]
      {:db (update-in db db-path (fnil conj []) app-with-version-number)
       :fx [[:dispatch [::fetch-app-picker-apps
                        ::spec/pagination-apps-picker]]
            [:dispatch [::set-apps-edited true]]
            [:dispatch [::main-events/changes-protection? true]]
            [:dispatch [::load-module-configuration 0 app-with-version-number]]]})))

(defn version-id-to-add
  [app]
  (let [versions           (:versions app)
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
                               #(dispatch [::do-add-app %])]
         :fx                  [[:dispatch [::fetch-app-picker-apps
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
    (let [db-path (subs/create-apps-creation-db-path (:current-route db))
          apps    (mapv enrich-app apps)]
      {:fx (into
             [[:db (update-in db db-path (fnil into []) [])]
              [:dispatch [::fetch-app-picker-apps
                          ::spec/pagination-apps-picker]]]
             (map (fn [{:keys [path versions] :as app}]
                    (let [version-id (version-id-to-add app)]
                      [::apps-fx/get-module [path
                                             (get-version-id (map-indexed vector versions) version-id)
                                             #(dispatch [::do-add-app %])]]))
                  apps))})))

(reg-event-fx
  ::remove-app-from-creation-data
  (fn [{{:keys [current-route] :as db} :db} [_ app]]
    (let [db-path (subs/create-apps-creation-db-path current-route)]
      {:db (update-in db db-path (fn [apps]
                                   (vec (remove #(= (:id %) (:href app)) apps))))
       :fx [[:dispatch [::fetch-app-picker-apps
                        ::spec/pagination-apps-picker]]
            [:dispatch [::set-apps-edited true]]
            [:dispatch [::main-events/changes-protection? true]]]})))

(reg-event-fx
  ::edit-config
  (fn [{{:keys [::spec/module-applications-sets
                ::spec/deployment-set] :as db} :db}]
    {:fx [[:dispatch [::edit :applications-sets
                      [{:id         (:id module-applications-sets)
                        :version    (apps-utils/module-version module-applications-sets)
                        :overwrites (map-indexed
                                      (fn [i [app-set current-overwrites]]
                                        (applications-sets->overwrites db i app-set current-overwrites))
                                      (map vector
                                           (-> module-applications-sets :content :applications-sets)
                                           (concat (get-in deployment-set [:applications-sets 0 :overwrites])
                                                   (repeat nil))))}]]]]}))



(reg-event-db
  ::enable-form-validation
  (fn [db]
    (assoc db ::spec/validate-form? true)))

(reg-event-db
  ::disable-form-validation
  (fn [db]
    (assoc db ::spec/validate-form? false)))

(reg-event-fx
  ::navigate-internal
  (fn [{:keys [db]} [_ route-data]]
    {:db (assoc db ::main-spec/changes-protection? false)
     :fx [[:dispatch (into [::routing-events/navigate-partial
                            (assoc route-data
                              :change-event [::main-events/changes-protection? true])])]]}))

(defn get-full-filter-string
  [{:keys [::spec/edge-picker-state-selector
           ::spec/edge-picker-additional-filter] :as db}]
  (general-utils/join-and
    "id!=null"
    (when edge-picker-state-selector (edges-utils/state-filter edge-picker-state-selector))
    edge-picker-additional-filter
    (full-text-search-plugin/filter-text
      db [::spec/edge-picker-full-text-search])))

(reg-event-fx
  ::get-edges-for-edge-picker-modal
  (fn []
    {:fx [[:dispatch [::get-picker-edges]]
          [:dispatch [::get-edge-picker-edges-summary]]]}))

(reg-event-fx
  ::get-picker-edges
  (fn [{{:keys [::spec/edge-picker-ordering
                ::spec/edges] :as db} :db} _]
    (let [ordering (or edge-picker-ordering spec/default-ordering)]
      {::cimi-api-fx/search
       [:nuvlabox
        (->> {:orderby (ordering->order-string ordering)
              :filter  (general-utils/join-and
                         (get-full-filter-string db)
                         (general-utils/ids->exclude-filter-str (:resources edges)))}
             (pagination-plugin/first-last-params
               db [::spec/edge-picker-pagination]))
        #(dispatch [::set-edge-picker-edges %])]})))

(reg-event-db
  ::set-edge-picker-edges
  (fn [db [_ response]]
    (assoc db ::spec/edge-picker-edges response)))


(reg-event-fx
  ::get-edge-picker-edges-summary
  (fn [{{:keys [::spec/edge-picker-additional-filter
                ::spec/edges] :as db} :db}]
    {::cimi-api-fx/search
     [:nuvlabox
      (edges-utils/get-query-aggregation-params
        (full-text-search-plugin/filter-text
          db [::spec/edge-picker-full-text-search])
        edges-spec/state-summary-agg-term
        (general-utils/join-and edge-picker-additional-filter
          (general-utils/ids->exclude-filter-str
            (:resources edges))))
      #(dispatch [::set-edge-picker-edges-summary %])]}))

(reg-event-db
  ::set-edge-picker-edges-summary
  (fn [db [_ nuvlaboxes-summary]]
    (assoc db ::spec/edge-picker-edges-summary nuvlaboxes-summary)))

(reg-event-fx
  ::set-edge-picker-additional-filter
  (fn [{db :db} [_ filter]]
    {:db (-> db
             (assoc ::spec/edge-picker-additional-filter filter)
             (assoc-in [::spec/edge-picker-pagination :active-page] 1))
     :fx [[:dispatch [::get-edges-for-edge-picker-modal]]
          [:dispatch [::table-plugin/reset-bulk-edit-selection [::spec/edge-picker-select]]]]}))

(reg-event-fx
  ::set-edge-picker-selected-state
  (fn [{db :db} [_ state]]
    {:db (assoc db ::spec/edge-picker-state-selector state)
     :fx [[:dispatch [::get-picker-edges]]]}))

(reg-event-db
  ::reset-edge-picker
  (fn [db]
    (assoc db
      ::spec/edge-picker-additional-filter ""
      ::spec/edge-picker-state-selector nil
      ::spec/edge-picker-select nil
      ::spec/edge-picker-full-text-search nil
      ::spec/edge-picker-ordering spec/default-ordering
      ::spec/edge-picker-pagination spec/pagination-default)))

(reg-event-fx
  ::add-edges
  (fn [{db :db} [_ response]]
    (let [edge-ids (map :id (:resources response))
          apps-set (get-in db [::spec/deployment-set-edited :applications-sets])]
      {:fx [[:dispatch [::set-opened-modal nil]]
            [:dispatch [::edit :applications-sets
                        (mapv
                          (fn [app-set]
                            (update app-set :overwrites
                              (partial mapv
                                (fn [app-set-overwrite]
                                  (update app-set-overwrite :fleet into edge-ids)))))
                          apps-set)]]
            [:dispatch [::refresh]]]})))

(reg-event-fx
  ::get-selected-edge-ids
  (fn [{{:keys [:sixsq.nuvla.ui.deployment-sets-detail.spec/edge-picker-select] :as db} :db} _]
    {::cimi-api-fx/search
     [:nuvlabox
      {:filter (table-plugin/build-bulk-filter
                 edge-picker-select
                 (get-full-filter-string db))
       :select "id"}
      #(dispatch [::add-edges %])]}))

(reg-event-fx
  ::show-fleet-changes-only
  (fn [{{:keys [::spec/changed-edges] :as db} :db} [_ changed-fleet]]
    {:db (assoc db ::spec/changed-edges
           (if changed-edges nil changed-fleet))
     :fx [[:dispatch [::get-edge-documents]]]}))
