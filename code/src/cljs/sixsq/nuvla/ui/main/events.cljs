(ns sixsq.nuvla.ui.main.events
  (:require [ajax.core :as ajax]
            [clojure.set :as set]
            [clojure.string :as str]
            [day8.re-frame.http-fx]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as api-fx]
            [sixsq.nuvla.ui.main.effects :as fx]
            [sixsq.nuvla.ui.main.spec :as spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.messages.spec :as messages-spec]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.session.events :as session-events]
            [sixsq.nuvla.ui.utils.general :as u]
            [sixsq.nuvla.ui.utils.time :as time]
            [taoensso.timbre :as log]))

(def notification-polling-id :notifications-polling)
(def check-ui-version-polling-id :check-ui-version)

(reg-event-fx
  ::bulk-actions-interval-after-navigation
  (fn [{{:keys [::spec/actions-interval
                ::spec/changes-protection?]} :db}]
    (when-not changes-protection?
      {::fx/bulk-actions-interval [::action-interval-delete
                                   (dissoc actions-interval
                                           notification-polling-id
                                           check-ui-version-polling-id)]})))

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

(defn- dispatch-close-modal-by-back-button-event
  []
  (dispatch [::do-not-ignore-changes]))

(def clear-close-modal-on-back-fx [::fx/clear-popstate-event-listener dispatch-close-modal-by-back-button-event])
(def clear-openend-by-browser-back-event [:dispatch [::unset-opened-by-browser-back]])

(reg-event-fx
  ::reset-changes-protection
  (fn [{{protected? ::spec/changes-protection? :as db} :db} [_ after-clear-event]]
    (if protected?
      {:db (assoc db ::spec/changes-protection? false)
       :fx [[::fx/on-unload-protection false]
            [::fx/enable-browser-back (when after-clear-event {:cb-fn (fn [] (dispatch after-clear-event))})]]}
      {:fx [(when after-clear-event [:dispatch after-clear-event])]})))

(reg-event-fx
  ::changes-protection?
  (fn [{db :db} [_ protect?]]
    (let [protected? (get db ::spec/changes-protection?)
          changed?   (not= protect? protected?)]
      (when changed?
        {:db (assoc db ::spec/changes-protection? protect?)
         :fx [[::fx/on-unload-protection protect?]
              [:dispatch (if protect? [::disable-browser-back] [::enable-browser-back])]]}))))


(reg-event-fx
  ::clear-changes-protections
  (fn [{db :db}]
    {:db (assoc db ::routing-events/ignore-changes-protection true)
     :fx [clear-close-modal-on-back-fx
          [::fx/on-unload-protection false]
          [::fx/enable-browser-back {:cb-fn     #(dispatch [::after-clear-event])
                                     :nav-back? (::opened-by-browser-back db)}]
          clear-openend-by-browser-back-event]}))

(reg-event-db
  ::close-ignore-modal
  (fn [db]
    (assoc db ::spec/ignore-changes-modal nil
              ::spec/do-not-ignore-changes-modal nil)))

(reg-event-fx
  ::do-not-ignore-changes
  (fn [{{:keys [::spec/do-not-ignore-changes-modal] :as db} :db} _]
    (let [base-fx        (if (::opened-by-browser-back db)
                           {:fx [[:dispatch [::routing-events/reset-ignore-changes-protection]]]}
                           {:fx (or (:fx do-not-ignore-changes-modal) [])})]
      (update base-fx :fx
        conj
        [:dispatch [::close-ignore-modal]]
        clear-openend-by-browser-back-event
        clear-close-modal-on-back-fx))))

(reg-event-fx
  ::ignore-changes
  (fn [{{:keys [::spec/ignore-changes-modal] :as db} :db} _]
    (let [db-chng-unprtd (assoc db
                           ::spec/changes-protection? false)
          clear-fx       [[:dispatch [::close-ignore-modal]]
                          [:dispatch [::clear-changes-protections]]]]
      (cond
        (map? ignore-changes-modal)
        {:db (merge (:db ignore-changes-modal)
                    (assoc db-chng-unprtd ::spec/after-clear-event (dissoc ignore-changes-modal :db)))
         :fx clear-fx}

        (fn? ignore-changes-modal)
        (do (ignore-changes-modal)
          {:db db-chng-unprtd
           :fx clear-fx})))))

(reg-event-fx
  ::after-clear-event
  (fn [{{after-clear-event ::spec/after-clear-event :as db} :db} [_ clear-event]]
    {:db (assoc db ::spec/after-clear-event nil)
     :fx (if clear-event [[:dispatch clear-event]] (:fx after-clear-event))}))

(reg-event-db
  ::set-opened-by-browser-back
  (fn [db]
   (assoc db ::opened-by-browser-back true)))

(reg-event-db
  ::unset-opened-by-browser-back
  (fn [db]
   (dissoc db ::opened-by-browser-back)))

(reg-event-fx
  ::opening-protection-modal
  (fn [_ _]
    {:fx [[::fx/add-pop-state-listener-close-modal-event dispatch-close-modal-by-back-button-event]]}))

(reg-event-fx
  ::disable-browser-back
  (fn [_ _]
    {:fx [[::fx/disable-browser-back #(dispatch [::set-opened-by-browser-back])]]}))

(reg-event-fx
  ::enable-browser-back
  (fn [_ [_ payload]]
    {:fx [[::fx/enable-browser-back payload]]}))



(reg-event-db
  ::ignore-changes-modal
  (fn [db [_ callback-fn]]
    (assoc db ::spec/ignore-changes-modal callback-fn)))

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
    {:dispatch [::action-interval-start `{:id        notification-polling-id
                                          :frequency 60000
                                          :event     [::check-notifications]}]}))

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
                 :frequency 120000
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

(reg-event-fx
  ::navigate
  (fn [{{:keys [::spec/device]} :db} [_ url]]
    {:fx [(when (#{:mobile :tablet} device)
            [:dispatch [::close-sidebar]])
          [:dispatch [::routing-events/navigate url]]]}))
