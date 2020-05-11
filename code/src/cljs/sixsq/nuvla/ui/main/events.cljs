(ns sixsq.nuvla.ui.main.events
  (:require
    [ajax.core :as ajax]
    [clojure.set :as set]
    [clojure.string :as str]
    [day8.re-frame.http-fx]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx subscribe]]
    [sixsq.nuvla.ui.cimi-api.effects :as api-fx]
    [sixsq.nuvla.ui.main.effects :as fx]
    [sixsq.nuvla.ui.main.spec :as spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.messages.spec :as messages-spec]
    [sixsq.nuvla.ui.session.events :as session-events]
    [sixsq.nuvla.ui.utils.general :as u]
    [sixsq.nuvla.ui.utils.time :as time]
    [taoensso.timbre :as log]))


(def notification-polling-id :notifications-polling)


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
  (fn [{:keys [::spec/device] :as db} [_ new-device]]
    (log/info "setting device:" new-device)
    (cond-> (assoc db ::spec/device new-device)
            (not= device new-device) (assoc ::spec/sidebar-open?
                                            (not (#{:mobile :tablet} new-device))))))


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
                                           ::action-interval-start
                                           ::action-interval-pause) actions-interval]}
            visible? (assoc :dispatch [::session-events/initialize]))))


(reg-event-fx
  ::set-navigation-info
  (fn [{{:keys [::spec/actions-interval
                ::spec/changes-protection?] :as db} :db} [_ path query-params]]
    (let [path-vec (vec (str/split path #"/"))]
      (when (not changes-protection?)
        {:db                        (assoc db ::spec/nav-path path-vec
                                              ::spec/nav-query-params query-params)
         ::fx/bulk-actions-interval [::action-interval-delete
                                     (dissoc actions-interval notification-polling-id)]}))))


(reg-event-fx
  ::action-interval-start
  (fn [{{:keys [::spec/actions-interval] :as db} :db}
       [_ {:keys [id] :as action-opts}]]
    (let [existing-action (get actions-interval id)
          {:keys [event timer frequency] :as action-opts} (or existing-action action-opts)
          next-refresh    (time/add-milliseconds (time/now) frequency)
          new-action-opts (-> action-opts
                              (assoc :next-refresh next-refresh)
                              (cond-> (nil? timer)
                                      (assoc :timer
                                             (js/setInterval
                                               #(dispatch [::action-interval-start action-opts])
                                               frequency))))]
      (cond
        (nil? existing-action) (log/info "Start action-interval: " action-opts)
        (nil? timer) (log/info "Resume action-interval: " id))
      {:dispatch event
       :db       (assoc-in db [::spec/actions-interval id] new-action-opts)})))


(reg-event-db
  ::action-interval-pause
  (fn [{:keys [::spec/actions-interval] :as db} [_ {:keys [id] :as action-opts}]]
    (log/info "Pause action-interval:" id)
    (let [{existing-timer :timer :as existing-action} (get actions-interval id)]
      (when existing-timer
        (js/clearInterval existing-timer))
      (cond-> db
              existing-action (update-in [::spec/actions-interval id] dissoc :timer)))))


(reg-event-db
  ::action-interval-delete
  (fn [{:keys [::spec/actions-interval] :as db} [_ {:keys [id] :as action-opts}]]
    (log/info "Delete action-interval: " id)
    (let [{existing-timer :timer} (get actions-interval id)]
      (when existing-timer
        (js/clearTimeout existing-timer))
      (assoc db ::spec/actions-interval (dissoc actions-interval id)))))


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

        (map? ignore-changes-modal)
        (cond-> {:db (cond-> close-modal-db
                             choice (assoc ::spec/changes-protection? false))}
                choice (merge ignore-changes-modal))

        (fn? ignore-changes-modal)
        (do (when choice (ignore-changes-modal))
            {:db close-modal-db})))))


(reg-event-db
  ::ignore-changes-modal
  (fn [db [_ callback-fn]]
    (assoc db ::spec/ignore-changes-modal callback-fn)))


#_(reg-event-fx
    ::set-bootsrap-message
    (fn [{:keys [db]} [_ {resources     :resources
                          resource-type :resource-type
                          element-count :count :as response}]]
      (if response

        (case resource-type

          "infrastructure-service-collection"
          (if (> element-count 0)
            {:db             (assoc db ::spec/bootstrap-message nil)
             ::api-fx/search [:credential
                              {:filter (u/join-and
                                         "subtype='infrastructure-service-swarm'"
                                         (apply u/join-or
                                                (map #(str "parent='" (:id %) "'") resources)))
                               :last   0}
                              #(dispatch [::set-bootsrap-message %])]}
            {:db (assoc db ::spec/bootstrap-message :no-swarm)})

          "credential-collection"
          (if (> element-count 0)
            {:db (assoc db ::spec/bootstrap-message nil)}
            {:db (assoc db ::spec/bootstrap-message :no-credential)}))

        {:db (assoc db ::spec/bootstrap-message nil)})))


#_(reg-event-fx
    ::check-bootstrap-message
    (fn [_ _]
      {::api-fx/search [:infrastructure-service
                        {:filter "subtype='swarm'"
                         :select "id"}
                        #(dispatch [::set-bootsrap-message %])]}))


(reg-event-fx
  ::set-notifications
  (fn [{{:keys [::messages-spec/messages] :as db} :db} [_ {:keys [resources]}]]
    (let [existing-notifs  (->> messages
                                (filter (fn [{message-type :type}] (= message-type :notif)))
                                (map :uuid)
                                (set))
          fetched-notifs   (->> resources (map :id) set)
          notifs-to-remove (set/difference existing-notifs fetched-notifs)
          notifs-to-add    (set/difference fetched-notifs existing-notifs)
          dispatch-adds    (map
                             (fn [{:keys [id message] :as notification}]
                               (when (contains? notifs-to-add id)
                                 [::messages-events/add
                                  {:header  (-> message
                                                (str/split #"\n")
                                                first)
                                   :content message
                                   :data    notification
                                   :type    :notif}
                                  id])) resources)
          dispatch-removes (map (fn [id] [::messages-events/remove id]) notifs-to-remove)]
      {:dispatch-n (concat dispatch-adds dispatch-removes)})))


(reg-event-fx
  ::check-notifications
  (fn [_ _]
    {::api-fx/search [:notification
                      {:filter (u/join-and
                                 "expiry>'now'"
                                 (u/join-or "not-before=null" "not-before<='now'"))}
                      #(dispatch [::set-notifications %])]}))


(reg-event-fx
  ::notifications-polling
  (fn [_ _]
    {:dispatch [::action-interval-start
                {:id        notification-polling-id
                 :frequency 60000
                 :event     [::check-notifications]}]}))


(reg-event-fx
  ::set-message
  (fn [{db :db} [_ type message]]
    (cond-> {:db (assoc db ::spec/message [type, message])}
            message (assoc :dispatch-later [{:ms       10000
                                             :dispatch [::set-message nil]}]))))


(reg-event-db
  ::force-refresh-content
  (fn [db]
    (assoc db ::spec/content-key (random-uuid))))


(reg-event-db
  ::get-ui-config-good
  (fn [db [_ result]]
    (assoc db ::spec/intercom-api-id (:intercom-app-id result))))


(reg-event-db
  ::get-ui-config-bad
  (fn [db [_ response]]
    (log/info "Failed to load UI configuration file")
    db))


(reg-event-fx
  ::get-ui-config
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/ui/config/config.json"
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-ui-config-good]
                  :on-failure      [::get-ui-config-bad]}}))
