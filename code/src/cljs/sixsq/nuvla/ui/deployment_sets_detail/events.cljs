(ns sixsq.nuvla.ui.deployment-sets-detail.events
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.deployment-sets-detail.spec :as spec]
            [sixsq.nuvla.ui.deployment-sets-detail.subs :as subs]
            [sixsq.nuvla.ui.deployments.events :as deployments-events]
            [sixsq.nuvla.ui.deployments.utils :as deployments-utils]
            [sixsq.nuvla.ui.edges.utils :as edge-utils]
            [sixsq.nuvla.ui.job.events :as job-events]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.events :as events-plugin]
            [sixsq.nuvla.ui.plugins.module :as module-plugin]
            [sixsq.nuvla.ui.plugins.table :refer [ordering->order-string]]
            [sixsq.nuvla.ui.plugins.target-selector :as target-selector]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :as routing-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.response :as response]))

(def refresh-action-id :deployment-set-get-deployment-set)

(defn uuid->depl-set-id [uuid]
  (str "deployment-set/" uuid))

(defn refresh
  [uuid]
  (dispatch [::main-events/action-interval-start
             {:id        refresh-action-id
              :frequency 10000
              :event     [::get-deployment-set (uuid->depl-set-id uuid)]}]))

(reg-event-db
  ::clear-target-edges
  (fn [db]
    (assoc db ::spec/edges nil)))

(reg-event-fx
  ::init
  (fn [_ [_ uuid]]
    {:fx [[:dispatch [::clear-target-edges]]
          [:dispatch [::main-events/action-interval-delete {:id refresh-action-id}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-action-id
                       :frequency 10000
                       :event     [::get-deployment-set (uuid->depl-set-id uuid)]}]]] }))

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
          apps-sets (->> apps-sets-set
                      :content
                      :applications-sets
                      (map-indexed vector))
          merged-configs (mapv (fn [[idx app-set]]
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
          apps-urls  (->> apps-sets
                       :content
                       :applications-sets
                       (mapcat :applications)
                       (map :id)
                       distinct)
          filter-str (apply general-utils/join-or (map #(str "id='" % "'") apps-urls))
          params     {:filter filter-str
                      :last   1000}
          callback   #(if (instance? js/Error %)
                        (cimi-api-fx/default-error-message % "load applications sets failed")
                        (dispatch [::load-apps-sets-response {:apps-sets-set apps-sets
                                                              :total-apps-count (count apps-urls)
                                                              :apps (:resources %)
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
  (fn [{:keys [db]} [_ deployment-set]]
    (let [parent-ids (->> deployment-set
                          :applications-sets
                          (mapcat :overwrites)
                          (mapcat :targets))]
      {:db (assoc db ::spec/deployment-set-not-found? (nil? deployment-set)
             ::spec/deployment-set deployment-set
             ::main-spec/loading? false)
       :fx [[:dispatch [::resolve-to-ancestor {:ids parent-ids
                                               :storage-event ::set-edges}]]
            [:dispatch [::get-application-sets (-> deployment-set :applications-sets first :id)]]]})))

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
                        (on-success-fn %))
          on-error   #(do
                        (cimi-api-fx/default-operation-on-error resource-id operation %)
                        (on-error-fn))]
      {::cimi-api-fx/operation [resource-id operation on-success
                                :on-error on-error :data data]})))

(reg-event-fx
  ::get-deployment-set
  (fn [{{:keys [::spec/deployment-set] :as db} :db} [_ id]]
    {:db               (cond-> db
                         (not= (:id deployment-set) id) (merge spec/defaults))
     ::cimi-api-fx/get [id #(dispatch [::set-deployment-set %])
                        :on-error #(dispatch [::set-deployment-set nil])]
     :fx               [[:dispatch [::events-plugin/load-events [::spec/events] id]]
                        [:dispatch [::job-events/get-jobs id]]]}))

(def deployments-state-filter-key :depl-state)

(reg-event-fx
  ::get-deployments-for-deployment-sets
  (fn [{{:keys [current-route]} :db} [_ id]]
    (when id
      (let [query-filter      (routing-utils/get-query-param current-route deployments-state-filter-key)
            filter-constraint (str "deployment-set='" (uuid->depl-set-id id) "'")]
        {:fx [[:dispatch [::deployments-events/get-deployments
                          {:filter-external-arg   (general-utils/join-and
                                                    filter-constraint
                                                    (deployments-utils/state-filter query-filter))
                           :external-filter-only? true}]]
              [:dispatch [::deployments-events/get-deployments-summary-all filter-constraint]]]}))))

(reg-event-fx
  ::edit
  (fn [_ [_ resource-id data success-msg]]
    {::cimi-api-fx/edit
     [resource-id data
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
           (dispatch [::set-deployment-set %])))]}))

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
        applications-overwrites (map (partial application-overwrites db i)
                                  applications)]
    (cond-> {}
      (seq targets) (assoc :targets targets)
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
  ::resolve-to-ancestor
  (fn [{{:keys [::spec/edges]} :db} [_ {ids :ids storage-event :storage-event}]]
    (when-not edges
      (let [callback #(let [resources  (:resources %)
                            parent-ids (remove nil? (map :parent resources))
                            resolved?  (empty? parent-ids)]
                        (cond
                          (or (empty? resources) (instance? js/Error %))
                          (cimi-api-fx/default-error-message % "loading edges for credentials failed")

                          resolved?
                          (dispatch [storage-event %])

                          :else
                          (dispatch [::resolve-to-ancestor {:ids           parent-ids
                                                            :storage-event storage-event}])))
            resource-name (general-utils/id->resource-name (first ids))
            ids-filter    (general-utils/ids->filter-string ids)]
        (when (every? seq [resource-name ids-filter])
          {::cimi-api-fx/search [resource-name
                                 (cond->
                                   {:filter ids-filter
                                    :last   10000
                                    :select "id, parent"}
                                   (= "nuvlabox" resource-name)
                                   (merge {:aggregation "terms:online,terms:state"}))
                                 callback]})))))


(reg-event-fx
  ::set-edges
  (fn [{db :db} [_ response]]
    {:db (assoc db ::spec/edges
           (update response :resources #(mapv :id %)))
     :fx [[:dispatch [::get-edge-documents]]]}))

(def edges-state-filter-key :edges-state)

(reg-event-fx
  ::get-edge-documents
  (fn [{{:keys [::spec/edges
                ::spec/ordering
                current-route] :as db} :db} _]
    (let [ordering     (or ordering spec/default-ordering)
          query-filter (routing-utils/get-query-param current-route edges-state-filter-key)]
      (when edges
        {:db (assoc db ::spec/edge-documents nil)
         ::cimi-api-fx/search
         [:nuvlabox
          (->> {:orderby (ordering->order-string ordering)
                :filter  (general-utils/join-and
                           "id!=null"
                           (general-utils/ids->filter-string (-> edges
                                                                 :resources))
                           (when query-filter (edge-utils/state-filter query-filter)))}
               #_(pagination-plugin/first-last-params
                   db [::spec/pagination]))
          #(dispatch [::set-edge-documents %])]}))))

(reg-event-db
  ::set-edge-documents
  (fn [db [_ edges-response]]
    (assoc db ::spec/edges-documents edges-response)))