(ns sixsq.nuvla.ui.deployment-sets-detail.events
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.deployment-sets-detail.spec :as spec]
            [sixsq.nuvla.ui.deployments.events :as deployments-events]
            [sixsq.nuvla.ui.job.events :as job-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.events :as events-plugin]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search]
            [sixsq.nuvla.ui.plugins.module :as module-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination]
            [sixsq.nuvla.ui.plugins.nav-tab :as nav-tab]
            [sixsq.nuvla.ui.plugins.target-selector :as target-selector]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.utils :as routing-utils]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.session.utils :refer [get-active-claim]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.utils.response :as response]))

(reg-event-fx
  ::new
  (fn [{{:keys [current-route] :as db} :db}]
    (let [id (routing-utils/get-query-param current-route :applications-sets)]
      {:db               (merge db spec/defaults)
       ::cimi-api-fx/get [id #(dispatch [::set-applications-sets %])]})))


(defn restore-applications
  [modules-by-id db [i {:keys [applications]}]]
  (-> db
      (assoc-in [::spec/apps-sets i ::spec/applications]
                (->> applications
                     (map (fn [{module-id :id}]
                            [module-id (get modules-by-id module-id
                                            {:subtype "unknown" :id module-id})]))
                     (into {})))
      (assoc-in [::spec/apps-sets i ::spec/targets]
                (target-selector/build-spec))))

(defn load-module-configurations
  [modules-by-id fx [id {:keys [applications]}]]
  (->> applications
       (map (fn [{module-id :id :keys [version environmental-variables]}]
              (when (get modules-by-id module-id)
                [:dispatch [::module-plugin/load-module
                            [::spec/apps-sets id]
                            (str module-id "_" version)
                            (when (seq environmental-variables)
                              {:env (->> environmental-variables
                                         (map (juxt :name :value))
                                         (into {}))})]])))
       (concat fx)))
(reg-event-fx
  ::load-apps-sets-response
  (fn [{:keys [db]} [_ module apps-count {:keys [resources]}]]
    (let [modules-by-id     (->> resources (map (juxt :id identity)) (into {}))
          indexed-apps-sets (->> module
                                 :content
                                 :applications-sets
                                 (map-indexed vector))
          new-db            (reduce (partial restore-applications modules-by-id)
                                    db indexed-apps-sets)
          fx                (reduce (partial load-module-configurations modules-by-id)
                                    [] indexed-apps-sets)
          all-apps-visible? (= apps-count (count resources))]
      (if all-apps-visible?
        {:db new-db
         :fx fx}
        {:fx [[:dispatch [::messages-events/add
                          {:header  "Unable to load selected applications sets"
                           :content (str "Loaded " (count resources) " out of " apps-count ".")
                           :type    :error}]]]}))))

(reg-event-fx
  ::load-apps-sets
  (fn [_ [_ module]]
    (let [apps-urls  (->> module
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
                        (dispatch [::load-apps-sets-response module (count apps-urls) %]))]
      (when (seq apps-urls)
        {::cimi-api-fx/search [:module params callback]}))))

(reg-event-fx
  ::set-applications-sets
  (fn [{:keys [db]} [_ {:keys [subtype] :as module}]]
    (if (apps-utils/applications-sets? subtype)
      {:db (assoc db ::spec/module-applications-sets module)
       :fx [[:dispatch [::load-apps-sets module]]]}
      {:dispatch [::messages-events/add
                  {:header  "Wrong module subtype"
                   :content (str "Selected module subtype:" subtype)
                   :type    :error}]})))

(reg-event-fx
  ::set-deployment-set
  (fn [{:keys [db]} [_ deployment-set]]
    {:db (assoc db ::spec/deployment-set-not-found? (nil? deployment-set)
                   ::spec/deployment-set deployment-set
                   ::main-spec/loading? false)}))

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
                        [:dispatch [::job-events/get-jobs id]]
                        [:dispatch [::get-deployments-for-deployment-sets id]]]}))

(reg-event-fx
  ::get-deployments-for-deployment-sets
  (fn [_ [_ id]]
    (when id
      {:fx [:dispatch [::deployments-events/get-deployments
                       {:filter-external-arg   (str "deployment-set='" id "'")
                        :external-filter-only? true}]]})))

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

(reg-event-db
  ::set-apps
  (fn [db [_ apps]]
    (assoc db ::spec/apps apps
              ::spec/apps-loading? false)))

(reg-event-fx
  ::search-apps
  (fn [{{:keys [::session-spec/session] :as db} :db}]
    {:db (assoc db ::spec/apps-loading? true)
     ::cimi-api-fx/search
     [:module (->>
                {:select  "id, name, description, parent-path, subtype"
                 :orderby "path:asc"
                 :filter  (general-utils/join-and
                            (full-text-search/filter-text db [::spec/apps-search])
                            (case (nav-tab/get-active-tab db [::spec/tab-new-apps])
                              :my-apps (str "acl/owners='" (get-active-claim session) "'")
                              :app-store "published=true"
                              nil)
                            "subtype!='project'")}
                (pagination/first-last-params db [::spec/apps-pagination]))
      #(dispatch [::set-apps %])]}))

(reg-event-fx
  ::toggle-select-app
  (fn [{{:keys [::spec/apps-selected] :as db} :db} [_ {:keys [id] :as module}]]
    (let [select? (nil? (apps-selected module))
          op      (if select? conj disj)]
      (cond-> {:db (update db ::spec/apps-selected op module)}
              select? (assoc :fx [[:dispatch [::module-plugin/load-module [::spec/module-versions] id]]])))))

(reg-event-db
  ::toggle-select-target
  (fn [{:keys [::spec/targets-selected] :as db} [_ credential credentials]]
    (let [select? (nil? (targets-selected credential))
          op      (if select? conj disj)]
      (-> db
          (assoc ::spec/targets-selected
                 (apply disj targets-selected credentials))
          (update ::spec/targets-selected op credential)))))

(reg-event-fx
  ::set-credentials
  (fn [{db :db} [_ response]]
    {:db (assoc db ::spec/targets-loading? false
                   ::spec/credentials response)}))

(reg-event-fx
  ::search-credentials
  (fn [_ [_ filter-str]]
    {::cimi-api-fx/search
     [:credential {:last   10000
                   :select "id, name, description, parent, subtype"
                   :filter filter-str}
      #(dispatch [::set-credentials %])]}))

(reg-event-fx
  ::set-infrastructures
  (fn [{db :db} [_ {:keys [resources] :as response}]]
    (if (seq resources)
      (let [filter-str (->> resources
                            (map #(str "parent='" (:id %) "'"))
                            (apply general-utils/join-or)
                            (general-utils/join-and
                              (general-utils/join-or
                                "subtype='infrastructure-service-swarm'"
                                "subtype='infrastructure-service-kubernetes'"
                                )))]
        {:db (assoc db ::spec/infrastructures response)
         :fx [[:dispatch [::search-credentials filter-str]]]})
      {:db (assoc db ::spec/targets-loading? false
                     ::spec/infrastructures response
                     ::spec/credentials nil)})))

(reg-event-fx
  ::search-infrastructures
  (fn [_ [_ filter-str]]
    {::cimi-api-fx/search
     [:infrastructure-service
      {:last   10000
       :select "id, name, description, subtype, parent"
       :filter filter-str}
      #(dispatch [::set-infrastructures %])]}))

(reg-event-fx
  ::search-clouds
  (fn [{db :db}]
    {:db (assoc db ::spec/targets-loading? true)
     ::cimi-api-fx/search
     [:infrastructure-service
      (->> {:select  "id, name, description, subtype, parent"
            :orderby "name:asc,id:asc"
            :filter  (general-utils/join-and
                       (general-utils/join-or
                         "tags!='nuvlabox=True'"
                         "tags!='nuvlaedge=True'")
                       (general-utils/join-or
                         "subtype='swarm'"
                         "subtype='kubernetes'")
                       (full-text-search/filter-text
                         db [::spec/clouds-search]))}
           (pagination/first-last-params db [::spec/clouds-pagination]))
      #(dispatch [::set-infrastructures %])]}))

(reg-event-fx
  ::set-edges
  (fn [{db :db} [_ {:keys [resources] :as response}]]
    (if (seq resources)
      (let [filter-str (->> resources
                            (map #(str "parent='"
                                       (:infrastructure-service-group %)
                                       "'"))
                            (apply general-utils/join-or)
                            (general-utils/join-and
                              (general-utils/join-or
                                "subtype='swarm'"
                                "subtype='kubernetes'")))]
        {:db (assoc db ::spec/edges response)
         :fx [[:dispatch [::search-infrastructures filter-str]]]})
      {:db (assoc db ::spec/targets-loading? false
                     ::spec/edges response
                     ::spec/infrastrutures nil
                     ::spec/credentials nil)})))

(reg-event-fx
  ::search-edges
  (fn [{db :db}]
    {:db (assoc db ::spec/targets-loading? true)
     ::cimi-api-fx/search
     [:nuvlabox
      (->> {:select  "id, name, description, infrastructure-service-group"
            :orderby "name:asc,id:asc"
            :filter  (general-utils/join-and
                       (full-text-search/filter-text db [::spec/edges-search])
                       "state='COMMISSIONED'"
                       "infrastructure-service-group!=null")}
           (pagination/first-last-params db [::spec/edges-pagination]))
      #(dispatch [::set-edges %])]}))

(defn changed-env-vars
  [application env-vars]
  (keep (fn [{:keys [::module-plugin/new-value :value :name]}]
          (when (some-> new-value (not= value))
            {:name        name
             :value       new-value
             :application application})
          ) env-vars))


(reg-event-fx
  ::create
  (fn [{{:keys [::spec/targets-selected
                ::spec/apps-selected
                ::spec/create-name
                ::spec/create-description
                ::spec/create-start] :as db} :db}]
    {::cimi-api-fx/add
     [:deployment-set
      (cond->
        {:spec {:applications (map #(str (:id %) "_"
                                         (module-plugin/db-selected-version
                                           db [::spec/module-versions] (:id %)))
                                   apps-selected)
                :targets      (map :id targets-selected)
                :env          (mapcat (fn [{:keys [id]}]
                                        (->> id
                                             (module-plugin/db-environment-variables
                                               db [::spec/module-versions])
                                             (changed-env-vars id)))
                                      apps-selected)
                :coupons      (keep (fn [{:keys [id]}]
                                      (when-let [coupon (->> id
                                                             (module-plugin/db-coupon
                                                               db [::spec/module-versions]))]
                                        {:application id
                                         :code        coupon}))
                                    apps-selected)
                :start        create-start}}
        (not (str/blank? create-name)) (assoc :name create-name)
        (not (str/blank? create-description)) (assoc :description create-description))
      #(dispatch [::routing-events/navigate routes/deployment-sets-details {:uuid (general-utils/id->uuid (:resource-id %))}])]}))

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