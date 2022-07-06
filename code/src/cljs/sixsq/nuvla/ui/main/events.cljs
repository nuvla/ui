(ns sixsq.nuvla.ui.main.events
  (:require
    [ajax.core :as ajax]
    [clojure.set :as set]
    [clojure.string :as str]
    [day8.re-frame.http-fx]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
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
(def check-ui-version-polling-id :check-ui-version)


(reg-event-db
  ::set-loading?
  (fn [db [_ loading?]]
    (assoc db ::spec/loading? loading?)))


(reg-event-db
  ::not-found?
  (fn [db [_ not-found?]]
    (assoc db ::spec/not-found? not-found?)))


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
  (fn [{:keys [::spec/device] :as db}]
    (let [width      (.-innerWidth js/window)
          new-device (cond
                       (< width 768) :mobile
                       (< width 991) :tablet
                       (< width 1199) :computer
                       (< width 1919) :large-screen
                       :else :wide-screen)
          changed?   (not= device new-device)]
      (when changed? (log/info "setting device:" new-device))
      (cond-> (assoc db ::spec/device new-device)
              changed? (assoc ::spec/sidebar-open?
                              (not (#{:mobile :tablet} new-device)))))))


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

(def page-alias {"nuvlabox"        "edges"
                 "edge"            "edges"
                 "infrastructures" "clouds"})

(defn split-path-alias
  [path]
  (let [[page :as path-vec] (vec (str/split path #"/"))
        real-page (get page-alias page)]
    (if (and page real-page)
      (assoc path-vec 0 real-page)
      path-vec)))

(reg-event-fx
  ::set-navigation-info
  (fn [{{:keys [::spec/actions-interval
                ::spec/changes-protection?] :as db} :db} [_ path query-params]]
    (when (not changes-protection?)
      {:db                        (assoc db ::spec/nav-path (split-path-alias path)
                                            ::spec/nav-query-params query-params)
       ::fx/bulk-actions-interval [::action-interval-delete
                                   (dissoc actions-interval
                                           notification-polling-id
                                           check-ui-version-polling-id)]})))


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
  (fn [{:keys [::spec/actions-interval] :as db} [_ {:keys [id] :as _action-opts}]]
    (log/info "Pause action-interval:" id)
    (let [{existing-timer :timer :as existing-action} (get actions-interval id)]
      (when existing-timer
        (js/clearInterval existing-timer))
      (cond-> db
              existing-action (update-in [::spec/actions-interval id] dissoc :timer)))))


(reg-event-db
  ::action-interval-delete
  (fn [{:keys [::spec/actions-interval] :as db} [_ {:keys [id] :as _action-opts}]]
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
  (fn [{{:keys [::messages-spec/messages]} :db} [_ {:keys [resources]}]]
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


(reg-event-fx
  ::get-ui-config-success
  (fn [{db :db} [_ {:keys [stripe] :as result}]]
    (cond-> {:db (assoc db ::spec/config result)}
            stripe (assoc :dispatch [::load-stripe]))))


(reg-event-db
  ::log-failed-response
  (fn [db [_ msg response]]
    (log/error msg ": " response)
    db))


(reg-event-db
  ::get-ui-version-success
  (fn [{{:keys [current-version notify?]
         :as   ui-version} ::spec/ui-version
        :as                db} [_ response]]
    (let [init-current-version? (nil? current-version)
          new-version-detected? (and notify?
                                     (some? current-version)
                                     (not= current-version response))
          should-open-modal?    (and notify?
                                     new-version-detected?)]
      (when init-current-version?
        (log/info "Init ui version to: " response))
      (when new-version-detected?
        (log/info "New ui version detected: " response))
      (assoc db ::spec/ui-version
                (cond-> ui-version
                        init-current-version? (assoc :current-version response)
                        new-version-detected? (assoc :new-version response
                                                     :notify? false)
                        should-open-modal? (assoc :open-modal? true))))))


(reg-event-db
  ::new-version-open-modal?
  (fn [db [_ open]]
    (assoc-in db [::spec/ui-version :open-modal?] open)))


(reg-event-fx
  ::get-ui-config
  (fn []
    (let [force-no-cache (int (time/timestamp))
          url            (str "/ui/config/config.json?" force-no-cache)]
      {:http-xhrio {:method          :get
                    :uri             url
                    :timeout         8000
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::get-ui-config-success]
                    :on-failure      [::log-failed-response
                                      "Failed to load UI configuration file"]}})))


(reg-event-fx
  ::get-ui-version
  (fn []
    (let [force-no-cache (int (time/timestamp))
          url            (str "/ui/css/version.css?" force-no-cache)]
      {:http-xhrio {:method          :get
                    :uri             url
                    :timeout         8000
                    :response-format (ajax/text-response-format)
                    :on-success      [::get-ui-version-success]
                    :on-failure      [::log-failed-response
                                      "Failed to load UI version file"]}})))


(reg-event-fx
  ::check-ui-version-polling
  (fn []
    {:dispatch [::action-interval-start
                {:id        check-ui-version-polling-id
                 :frequency 20000
                 :event     [::get-ui-version]}]}))

(reg-event-db
  ::open-modal
  (fn [db [_ modal-key]]
    (assoc db ::spec/open-modal modal-key)))


(reg-event-db
  ::close-modal
  (fn [db _]
    (assoc db ::spec/open-modal nil)))


(reg-event-fx
  ::load-stripe
  (fn [{{:keys [::spec/config
                ::spec/stripe]} :db} _]
    (when-let [stripe-public-key (:stripe config)]
      (when-not stripe
        {::fx/load-stripe [stripe-public-key #(dispatch [::stripe-loaded %])]}))))


(reg-event-db
  ::stripe-loaded
  (fn [db [_ stripe]]
    ;;^js type hint needed with externs
    ;; inference to not break with advanced optimizations
    (assoc db ::spec/stripe stripe)))


(reg-event-fx
  ::subscription-required-dispatch
  (fn [{{:keys [::spec/stripe
                :sixsq.nuvla.ui.profile.spec/subscription]} :db} [_ dispatch-vector]]
    (let [subs-status (:status subscription)
          active?     (boolean
                        (or (and (some? stripe)
                                 (#{"trialing" "active" "past_due"} subs-status))
                            (nil? stripe)))
          unpaid?     (and (some? stripe)
                           (= subs-status "unpaid"))]
      {:dispatch (if active?
                   dispatch-vector
                   (if unpaid?
                     [::open-modal :subscription-unpaid]
                     [::open-modal :subscription-required]))})))

