(ns sixsq.nuvla.ui.main.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.client.spec :as client-spec]
    [sixsq.nuvla.ui.main.effects :as main-fx]
    [sixsq.nuvla.ui.main.spec :as spec]
    [taoensso.timbre :as log]))


(reg-event-db
  ::check-iframe
  (fn [db _]
    (let [location        (.-location js/window)
          parent-location (.-location (.-parent js/window))
          iframe?         (and location parent-location (not= location parent-location))]
      (log/info "running within iframe?" iframe?)
      (assoc db ::spec/iframe? iframe?))))


(reg-event-db
  ::set-device
  (fn [db [_ device]]
    (log/info "setting device:" device)
    (cond-> (assoc db ::spec/device device)
            (not (#{:mobile :table} device)) (assoc ::spec/sidebar-open? true))))


(reg-event-db
  ::close-sidebar
  (fn [db _]
    (assoc db ::spec/sidebar-open? false)))


(reg-event-db
  ::toggle-sidebar
  (fn [{:keys [::spec/sidebar-open?] :as db} _]
    (update db ::spec/sidebar-open? not ::spec/sidebar-open?)))


(reg-event-fx
  ::visible
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ v]]
    (cond-> {:db                       (assoc db ::spec/visible? v)
             ::main-fx/action-interval (if v [{:action :resume}] [{:action :pause}])}
            v (assoc ::cimi-api-fx/session [client #(dispatch [::authn-events/set-session %])]))))


(reg-event-fx
  ::set-navigation-info
  (fn [{:keys [db]} [_ path query-params]]
    (let [path-vec (vec (str/split path #"/"))]
      {:db                       (assoc db ::spec/nav-path path-vec
                                           ::spec/nav-query-params query-params)
       ::main-fx/action-interval [{:action :clean}]})))


(reg-event-fx
  ::action-interval
  (fn [_ [_ opts]]
    {::main-fx/action-interval [opts]}))


(reg-event-fx
  ::open-link
  (fn [_ [_ uri]]
    {::main-fx/open-new-window [uri]}))


(reg-event-db
  ::changes-protection?
  (fn [db [_ choice]]
    (assoc db ::spec/changes-protection? choice)))


(reg-event-fx
  ::ignore-changes
  (fn [{{:keys [::spec/ignore-changes-modal] :as db} :db} [_ choice]]
    (let [close-modal-db (assoc db ::spec/ignore-changes-modal nil)]
      (cond
        (map? ignore-changes-modal) (cond-> {:db (cond-> close-modal-db
                                                         choice (assoc ::spec/changes-protection? false))}
                                            choice (merge ignore-changes-modal))
        (fn? ignore-changes-modal) (do (when choice (ignore-changes-modal))
                                       {:db close-modal-db})))))


(reg-event-db
  ::ignore-changes-modal
  (fn [db [_ callback-fn]]
    (assoc db ::spec/ignore-changes-modal callback-fn)))


(reg-event-fx
  ::set-bootsrap-message
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ {resources     :resources
                                                       resource-type :resource-type
                                                       element-count :count :as response}]]
    (if response

      (case resource-type
        "infrastructure-service-collection"
        (if (> element-count 0)
          {:db (assoc db ::spec/bootstrap-message nil)
           ::cimi-api-fx/search
               [client
                :credential
                {:filter (str "type='infrastructure-service-swarm' and ("
                              (str/join " or " (map #(str "infrastructure-services='" (:id %) "'") resources)) ")")
                 :last   0}
                #(dispatch [::set-bootsrap-message %])]}
          {:db (assoc db ::spec/bootstrap-message :no-swarm)})

        "credential-collection"
        (if (> element-count 0)
          {:db (assoc db ::spec/bootstrap-message nil)}
          {:db (assoc db ::spec/bootstrap-message :no-credential)}))

      {:db (assoc db ::spec/bootstrap-message nil)})))


(reg-event-fx
  ::check-bootstrap-message
  (fn [{{:keys [::client-spec/client] :as db} :db} _]
    {::cimi-api-fx/search [client
                           :infrastructure-service
                           {:filter "type='swarm'"
                            :select "id"}
                           #(dispatch [::set-bootsrap-message %])]}))