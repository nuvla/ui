(ns sixsq.nuvla.ui.deployment-sets-detail.events
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.deployment-sets-detail.spec :as spec]
            [sixsq.nuvla.ui.deployments.events :as deployments-events]
            [sixsq.nuvla.ui.job.events :as job-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.events :as events-plugin]
            [sixsq.nuvla.ui.plugins.module :as module-plugin]
            [sixsq.nuvla.ui.plugins.target-selector :as target-selector]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :as routing-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.response :as response]))

(reg-event-fx
  ::new
  (fn [{{:keys [current-route] :as db} :db}]
    (let [id (routing-utils/get-query-param current-route :applications-sets)]
      {:db               (merge db spec/defaults)
       ::cimi-api-fx/get [id #(dispatch [::set-applications-sets %])]})))


(defn restore-applications
  [db [i]]
  (assoc-in db [::spec/apps-sets i ::spec/targets]
            (target-selector/build-spec)))

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
          new-db            (reduce restore-applications
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
      {:fx [[:dispatch [::deployments-events/get-deployments
                        {:filter-external-arg   (str "deployment-set='" id "'")
                         :external-filter-only? true}]]]})))

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

(defn application-overwrites
  [db i {:keys [id version] :as _application}]
  (when-let [env-changed (->> id
                              (module-plugin/db-environment-variables db [::spec/apps-sets i])
                              module-plugin/changed-env-vars
                              seq)]
    {:id                      id
     :version                 version
     :environmental-variables env-changed}))

(defn applications-sets->overwrites
  [db i {:keys [applications] :as _applications-sets}]
  (let [targets                 (map :id (target-selector/db-selected db [::spec/apps-sets i ::spec/targets]))
        applications-overwrites (->> applications
                                     (map (partial application-overwrites db i))
                                     (remove nil?))]
    (cond-> {}
            (seq targets) (assoc :targets targets)
            (seq applications-overwrites) (assoc :applications applications-overwrites))))

(reg-event-fx
  ::create-start
  (fn [{{:keys [::spec/create-name
                ::spec/create-description
                ::spec/module-applications-sets] :as db} :db} [_ start?]]
    (let [body (cond->
                 {:name              create-name
                  :applications-sets [{:id         (:id module-applications-sets)
                                       :version    (apps-utils/module-version module-applications-sets)
                                       :overwrites (map-indexed (partial applications-sets->overwrites db)
                                                                (-> module-applications-sets :content :applications-sets))
                                       }]
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