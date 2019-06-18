(ns sixsq.nuvla.ui.main.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx subscribe]]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.main.effects :as fx]
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
    (update db ::spec/sidebar-open? not sidebar-open?)))


(reg-event-fx
  ::visible
  (fn [{{:keys [::spec/actions-interval] :as db} :db} [_ visible?]]
    (cond-> {:db                        (assoc db ::spec/visible? visible?)
             ::fx/bulk-actions-interval [(if visible?
                                           ::action-interval-resume
                                           ::action-interval-pause) actions-interval]}
            visible? (assoc ::cimi-api-fx/session [#(dispatch [::authn-events/set-session %])]))))


(reg-event-fx
  ::set-navigation-info
  (fn [{{:keys [::spec/actions-interval] :as db} :db} [_ path query-params]]
    (let [path-vec (vec (str/split path #"/"))]
      {:db                        (assoc db ::spec/nav-path path-vec
                                            ::spec/nav-query-params query-params)
       ::fx/bulk-actions-interval [::action-interval-delete actions-interval]})))


(def TICK_INTERVAL 1000)


(reg-event-db
  ::action-interval-tick
  (fn [{:keys [::spec/actions-interval] :as db} [_ action-id]]
    (let [{:keys [frequency event refresh-in]} (get actions-interval action-id)
          new-refresh-in (- (or refresh-in frequency) TICK_INTERVAL)]
      (if (pos-int? new-refresh-in)
        (assoc-in db [::spec/actions-interval action-id :refresh-in] new-refresh-in)
        (do
          (dispatch event)
          (assoc-in db [::spec/actions-interval action-id :refresh-in] frequency))))))


(reg-event-db
  ::action-interval-start
  (fn [{:keys [::spec/actions-interval] :as db} [_ {:keys [id event frequency] :as action-opts}]]
    (log/info "Start action-interval: " action-opts)
    (let [existing-action (get actions-interval id)
          timer           (or (:timer existing-action)
                              (js/setInterval #(dispatch [::action-interval-tick id]) TICK_INTERVAL))]
      (dispatch event)
      (assoc-in db [::spec/actions-interval id] (assoc action-opts :timer timer
                                                                   :refresh-in frequency)))))


(reg-event-db
  ::action-interval-pause
  (fn [{:keys [::spec/actions-interval] :as db} [_ action-id]]
    (log/info "Pause action-interval:" action-id)
    (let [{existing-timer :timer :as existing-action} (get actions-interval action-id)]
      (when existing-timer
        (js/clearInterval existing-timer))
      (cond-> db
              existing-action (update-in [::spec/actions-interval action-id] dissoc :timer)))))


(reg-event-db
  ::action-interval-resume
  (fn [{:keys [::spec/actions-interval] :as db} [_ action-id]]
    (log/info "Resume action-interval: " action-id)
    (if-let [{:keys [event frequency] :as existing-action} (get actions-interval action-id)]
      (let [timer (or (:timer existing-action)
                      (js/setInterval #(dispatch [::action-interval-tick action-id]) TICK_INTERVAL))]
        (dispatch event)
        (update-in db [::spec/actions-interval action-id] assoc
                   :timer timer
                   :refresh-in frequency))
      db)))


(reg-event-db
  ::action-interval-delete
  (fn [{:keys [::spec/actions-interval] :as db} [_ action-id]]
    (log/info "Delete action-interval: " action-id)
    (let [{existing-timer :timer} (get actions-interval action-id)]
      (when existing-timer
        (js/clearInterval existing-timer))
      (assoc db ::spec/actions-interval (dissoc actions-interval action-id)))))


(reg-event-fx
  ::open-link
  (fn [_ [_ uri]]
    {::fx/open-new-window [uri]}))


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
  (fn [{:keys [db]} [_ {resources     :resources
                        resource-type :resource-type
                        element-count :count :as response}]]
    (if response

      (case resource-type

        "infrastructure-service-collection"
        (if (> element-count 0)
          {:db                  (assoc db ::spec/bootstrap-message nil)
           ::cimi-api-fx/search [:credential
                                 {:filter (str "subtype='infrastructure-service-swarm' and ("
                                               (str/join " or " (map #(str "parent='" (:id %) "'") resources)) ")")
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
  (fn [_ _]
    {::cimi-api-fx/search [:infrastructure-service
                           {:filter "subtype='swarm'"
                            :select "id"}
                           #(dispatch [::set-bootsrap-message %])]}))


(reg-event-db
  ::set-welcome-message
  (fn [db [_ welcome-message]]
    (assoc db ::spec/welcome-message welcome-message)))
