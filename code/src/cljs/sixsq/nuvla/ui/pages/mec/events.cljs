(ns sixsq.nuvla.ui.pages.mec.events
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.messages.events :as messages-events]
            [sixsq.nuvla.ui.pages.mec.spec :as spec]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.response :as response]))

 (defn- error-message
   [header err]
   (let [{:keys [status message]} (response/parse-ex-info err)]
     [::messages-events/add
      {:header  (cond-> header
                        status (str " (" status ")"))
       :content message
       :type    :error}]))

(defn- mec-subscriptions-url
  ([] (str @cimi-api-fx/NUVLA_URL "/api/mec/mm1/app_lcm/v1/subscriptions"))
  ([subscription-id]
   ;; Ids are "subscription/<uuid>"; keep the slash as two path segments
   ;; (do not encodeURIComponent the whole id — %2F breaks the route).
   (str (mec-subscriptions-url) "/" subscription-id)))

(defn- response-text->edn
  [text]
  (when-not (str/blank? text)
    (try
      (general-utils/json->edn text)
      (catch :default _
        {:message text}))))

(defn- normalize-subscription
  [subscription]
  (when (map? subscription)
    {:id                    (or (:id subscription)
                                (:subscription-id subscription))
     :subscription-type     (or (:subscriptionType subscription)
                                (:subscription-type subscription))
     :callback-uri          (or (:callbackUri subscription)
                                (:callback-uri subscription))
     :app-instance-filter   (or (:appInstanceFilter subscription)
                                (:app-instance-filter subscription))
     :app-lcm-op-occ-filter (or (:appLcmOpOccFilter subscription)
                                (:app-lcm-op-occ-filter subscription))
     :created               (:created subscription)
     :updated               (:updated subscription)
     :owner                 (:owner subscription)
     :active                (:active subscription)}))

(defn- subscription-id-from-link
  "Extract API subscription id (subscription/<uuid>) from a link-list entry or resource."
  [{:keys [href id] :as _entry}]
  (or id
      (when (and (string? href) (not (str/blank? href)))
        (some-> (re-find #"/subscriptions/(.+?)/?$" href) second))))

(defn- subscription-links-from-body
  [body]
  (or (get-in body [:_links :subscriptions])
      (get-in body [:links :subscriptions])
      (get-in body ["_links" "subscriptions"])
      (get-in body ["_links" :subscriptions])))

(defn- subscription-list-entries
  "Mm1 GET /subscriptions returns {_links: {subscriptions: [{href, subscriptionType}]}}.
   Tolerate CIMI-style collections as well. Never treat a plain map as an entry list."
  [body]
  (cond
    (vector? body) body
    (sequential? (:items body)) (vec (:items body))
    (sequential? (:resources body)) (vec (:resources body))
    (sequential? (subscription-links-from-body body)) (vec (subscription-links-from-body body))
    :else []))

(defn- link-list-entry?
  "True when the entry is a link stub without full subscription fields."
  [entry]
  (and (map? entry)
       (string? (or (:href entry) (get entry "href")))
       (nil? (or (:callbackUri entry) (:callback-uri entry)))))

(defn- link-entry->partial-subscription
  "Enough fields to render a row before detail hydrate completes."
  [entry]
  (let [href (or (:href entry) (get entry "href"))
        stype (or (:subscriptionType entry)
                  (:subscription-type entry)
                  (get entry "subscriptionType"))]
    {:id               (subscription-id-from-link (assoc entry :href href))
     :subscriptionType stype
     :callbackUri      nil
     :owner            nil
     :active           nil
     :href             href}))

(defn- ui-subscription->api
  [{:keys [subscription-type callback-uri app-instance-filter app-lcm-op-occ-filter]}]
  (cond-> {:subscriptionType subscription-type
           :callbackUri      callback-uri}
    (seq app-instance-filter) (assoc :appInstanceFilter app-instance-filter)
    (seq app-lcm-op-occ-filter) (assoc :appLcmOpOccFilter app-lcm-op-occ-filter)))

(defn- normalize-edge
  [edge]
  {:id     (:id edge)
   :name   (:name edge)
   :online (:online edge)
   :state  (:state edge)})

(defn- mepm-managed-edges
  [mepm]
  (let [managed-edges (:managed-edges mepm)]
    (if (contains? mepm :managed-edges)
      (vec managed-edges)
      (cond-> []
        (:mec-host-id mepm) (conj (:mec-host-id mepm))))))

(defn- managed-edge-update
  [managed-edges]
  (cond-> {:managed-edges managed-edges}
    (seq managed-edges) (assoc :mec-host-id (first managed-edges))))

(reg-fx
  ::mec-subscriptions-request
  (fn [{:keys [method subscription-id body on-success on-error]}]
    (let [url      (if subscription-id
                     (mec-subscriptions-url subscription-id)
                     (mec-subscriptions-url))
          opts     (cond-> {:credentials "same-origin"
                            :method      method}
                     body (assoc :headers {"Content-Type" "application/json"}
                                 :body (js/JSON.stringify (clj->js body))))
          callback (fn [resp]
                     (-> (.text resp)
                         (.then (fn [text]
                                  (let [payload {:status (.-status resp)
                                                 :body   (response-text->edn text)}]
                                    (if (.-ok resp)
                                      (on-success payload)
                                      (on-error payload)))))))]
      (-> (js/fetch url (clj->js opts))
          (.then callback)
          (.catch (fn [e]
                    (on-error {:status nil
                               :body   {:message (.-message e)}})))))))

(reg-fx
  ::mec-subscriptions-hydrate
  (fn [{:keys [entries on-success on-error]}]
    (let [fetch-one
          (fn [entry]
            (let [href (or (:href entry)
                           (get entry "href")
                           (some-> (subscription-id-from-link entry) mec-subscriptions-url))
                  id   (or (subscription-id-from-link entry) href)]
              (-> (js/fetch href #js {:credentials "same-origin"
                                      :method      "GET"})
                  (.then (fn [resp]
                           (-> (.text resp)
                               (.then (fn [text]
                                        (when-not (.-ok resp)
                                          (throw (js/Error.
                                                   (str "GET subscription " id
                                                        " failed (" (.-status resp) ")"))))
                                        (or (response-text->edn text) {})))))))))]
      (if (seq entries)
        (-> (js/Promise.all (into-array (mapv fetch-one entries)))
            (.then (fn [results]
                     (on-success (vec (array-seq results)))))
            (.catch (fn [e]
                      (on-error {:status nil
                                 :body   {:message (.-message e)}}))))
        (on-success [])))))

 (reg-event-fx
   ::init
   (fn [{db :db} _]
     {:db (merge db spec/defaults)
      :fx [[:dispatch [::refresh]]]}))

 (reg-event-fx
   ::refresh
   (fn [_ _]
     {:fx [[:dispatch [::get-mepms]]
           [:dispatch [::get-available-edges]]
           [:dispatch [::get-subscriptions]]]}))

 (reg-event-db
   ::set-selected-mepm-id
   (fn [db [_ id]]
     (assoc db ::spec/selected-mepm-id id)))

 (reg-event-db
   ::set-selected-mepm
   (fn [db [_ mepm]]
     (assoc db
       ::spec/selected-mepm mepm
       ::spec/loading-selected-mepm? false)))

 (reg-event-fx
   ::get-mepm
   (fn [{db :db} [_ id]]
     (if id
       {:db               (assoc db ::spec/loading-selected-mepm? true
                                    ::spec/selected-mepm-id id)
        ::cimi-api-fx/get [id
                           #(dispatch [::set-selected-mepm %])
                           :on-error #(do
                                        (dispatch (error-message (str "error getting " id) %))
                                        (dispatch [::set-selected-mepm nil]))]}
       {:db (assoc db
              ::spec/selected-mepm-id nil
              ::spec/selected-mepm nil
              ::spec/loading-selected-mepm? false)})))

 (reg-event-fx
   ::set-mepms
   (fn [{db :db} [_ mepms]]
     (if (instance? js/Error mepms)
       {:db       (assoc db ::spec/mepms [] ::spec/loading-mepms? false)
        :dispatch (error-message "failure getting MEPMs" mepms)}
       (let [resources    (:resources mepms)
             selected-id  (::spec/selected-mepm-id db)
             next-id      (cond
                            (some #(= selected-id (:id %)) resources) selected-id
                            (seq resources) (:id (first resources))
                            :else nil)]
         {:db (assoc db
                ::spec/mepms resources
                ::spec/loading-mepms? false
                ::spec/selected-mepm-id next-id)
          :fx [(if next-id
                 [:dispatch [::get-mepm next-id]]
                 [:dispatch [::set-selected-mepm nil]])]}))))

 (reg-event-fx
   ::get-mepms
   (fn [{db :db} _]
     {:db                  (assoc db ::spec/loading-mepms? true)
      ::cimi-api-fx/search [:mepm {:orderby "created:desc"}
                            #(dispatch [::set-mepms %])]}))

(reg-event-fx
  ::set-available-edges
  (fn [{db :db} [_ edges]]
    (if (instance? js/Error edges)
      {:db       (assoc db ::spec/available-edges [] ::spec/loading-available-edges? false)
       :dispatch (error-message "failure getting Nuvla Edges" edges)}
      {:db (assoc db
             ::spec/available-edges (mapv normalize-edge (or (:resources edges) []))
             ::spec/loading-available-edges? false)})))

(reg-event-fx
  ::get-available-edges
  (fn [{db :db} _]
    {:db                  (assoc db ::spec/loading-available-edges? true)
     ::cimi-api-fx/search [:nuvlabox {:orderby "name:asc"
                                      :last    1000
                                      :select  "id,name,online,state"}
                           #(dispatch [::set-available-edges %])]}))

 (reg-event-fx
   ::add-mepm
   (fn [_ [_ data]]
     (let [on-success #(let [{:keys [status message resource-id]} (response/parse %)]
                         (dispatch [::messages-events/add
                                    {:header  (cond-> (str "added " resource-id)
                                                      status (str " (" status ")"))
                                     :content message
                                     :type    :success}])
                         (dispatch [::get-mepms])
                         (dispatch [::get-mepm resource-id]))
           on-error   #(dispatch (error-message "failure adding MEPM" %))]
       {::cimi-api-fx/add [:mepm data on-success :on-error on-error]})))

 (reg-event-fx
   ::delete-mepm
   (fn [{db :db} [_ id]]
     (let [on-success #(let [{:keys [status message]} (response/parse %)]
                         (dispatch [::messages-events/add
                                    {:header  (cond-> (str "deleted " id)
                                                      status (str " (" status ")"))
                                     :content message
                                     :type    :success}])
                         (when (= id (::spec/selected-mepm-id db))
                           (dispatch [::set-selected-mepm nil]))
                         (dispatch [::get-mepms]))
           on-error   #(dispatch (error-message (str "error deleting " id) %))]
       {::cimi-api-fx/delete [id on-success :on-error on-error]})))

(reg-event-fx
  ::update-mepm
  (fn [_ [_ id data success-label]]
    (let [on-result #(if (instance? js/Error %)
                       (dispatch (error-message (str "failure updating " id) %))
                       (let [{:keys [status]} (response/parse %)]
                         (dispatch [::messages-events/add
                                    {:header  (cond-> (or success-label (str "updated " id))
                                                      status (str " (" status ")"))
                                     :content "MEPM updated."
                                     :type    :success}])
                         (dispatch [::get-mepms])
                         (dispatch [::get-mepm id])))]
      {::cimi-api-fx/edit [id data on-result]})))

(reg-event-fx
  ::add-managed-edge
  (fn [{db :db} [_ mepm-id edge-id]]
    (let [mepm          (or (::spec/selected-mepm db)
                            (some #(when (= mepm-id (:id %)) %) (::spec/mepms db)))
          managed-edges (->> (conj (mepm-managed-edges mepm) edge-id)
                             (remove str/blank?)
                             distinct
                             vec)]
      {:fx [[:dispatch [::update-mepm mepm-id
                        (managed-edge-update managed-edges)
                        (str "added managed Edge " edge-id)]]]})))

(reg-event-fx
  ::remove-managed-edge
  (fn [{db :db} [_ mepm-id edge-id]]
    (let [mepm          (or (::spec/selected-mepm db)
                            (some #(when (= mepm-id (:id %)) %) (::spec/mepms db)))
          managed-edges (->> (mepm-managed-edges mepm)
                             (remove #(= edge-id %))
                             vec)]
      {:fx [[:dispatch [::update-mepm mepm-id
                        (managed-edge-update managed-edges)
                        (str "removed managed Edge " edge-id)]]]})))

 (reg-event-fx
   ::run-mepm-action
   (fn [_ [_ id action]]
     (let [on-success #(let [{:keys [status message]} (response/parse %)]
                         (dispatch [::messages-events/add
                                    {:header  (cond-> (str "success executing operation " action)
                                                      status (str " (" status ")"))
                                     :content (or message (str action " completed"))
                                     :type    :success}])
                         (dispatch [::get-mepms])
                         (dispatch [::get-mepm id]))
           on-error   #(dispatch (error-message (str "error executing " action " on " id) %))]
       {::cimi-api-fx/operation [id action on-success :on-error on-error]})))

 (reg-event-fx
   ::set-subscriptions
   (fn [{db :db} [_ subscriptions]]
     (if (instance? js/Error subscriptions)
       {:db       (assoc db ::spec/subscriptions [] ::spec/loading-subscriptions? false)
        :dispatch (error-message "failure getting MEC subscriptions" subscriptions)}
       (let [rows (->> (cond
                         (sequential? subscriptions) subscriptions
                         (map? subscriptions) (subscription-list-entries subscriptions)
                         :else [])
                       (keep normalize-subscription)
                       vec)]
         {:db (assoc db
                ::spec/subscriptions rows
                ::spec/loading-subscriptions? false)}))))

 (reg-event-fx
   ::get-subscriptions
   (fn [{db :db} _]
     (let [fail!   (fn [payload]
                     (dispatch [::messages-events/add
                                {:header  (cond-> "failure getting MEC subscriptions"
                                                  (:status payload) (str " (" (:status payload) ")"))
                                 :content (or (get-in payload [:body :message])
                                              (get-in payload [:body :detail])
                                              "Unable to retrieve MEC subscriptions.")
                                 :type    :error}])
                     (dispatch [::set-subscriptions []]))
           on-list (fn [{:keys [body]}]
                     (let [entries (subscription-list-entries body)]
                       (if (empty? entries)
                         (dispatch [::set-subscriptions []])
                         (do
                           ;; Show types/ids immediately from the link list (avoids blank ghost row).
                           (dispatch [::set-subscriptions
                                      (mapv link-entry->partial-subscription entries)])
                           (when (every? link-list-entry? entries)
                             (dispatch [::hydrate-subscriptions entries]))))))]
       {:db                         (assoc db ::spec/loading-subscriptions? true)
        ::mec-subscriptions-request {:method     "GET"
                                     :on-success on-list
                                     :on-error   fail!}})))

 (reg-event-fx
   ::hydrate-subscriptions
   (fn [_ [_ entries]]
     {::mec-subscriptions-hydrate
      {:entries    entries
       :on-success #(dispatch [::set-subscriptions %])
       :on-error   #(do
                      (dispatch [::messages-events/add
                                 {:header  "failure loading MEC subscription details"
                                  :content (or (get-in % [:body :message])
                                               "Unable to retrieve MEC subscription details.")
                                  :type    :error}])
                      ;; Keep partial rows already shown from the link list.
                      nil)}}))

 (reg-event-fx
   ::add-subscription
   (fn [_ [_ data]]
    (let [payload     (ui-subscription->api data)
          on-success #(let [{:keys [status message resource-id]} (response/parse (:body %))]
                         (dispatch [::messages-events/add
                                   {:header  (cond-> (str "added " (or resource-id (:id (:body %))))
                                                      status (str " (" status ")"))
                                     :content message
                                     :type    :success}])
                         (dispatch [::get-subscriptions]))
          on-error   #(dispatch [::messages-events/add
                                 {:header  (cond-> "failure adding MEC subscription"
                                                   (:status %) (str " (" (:status %) ")"))
                                  :content (or (:detail (:body %))
                                               (:message (:body %))
                                               "Unable to create MEC subscription.")
                                  :type    :error}])]
      {::mec-subscriptions-request {:method     "POST"
                                    :body       payload
                                    :on-success on-success
                                    :on-error   on-error}})))

 (reg-event-fx
   ::delete-subscription
   (fn [_ [_ id]]
    (let [on-success #(let [{:keys [status message]} (response/parse (:body %))]
                         (dispatch [::messages-events/add
                                    {:header  (cond-> (str "deleted " id)
                                                      status (str " (" status ")"))
                                     :content (or message "MEC subscription deleted.")
                                     :type    :success}])
                         (dispatch [::get-subscriptions]))
          on-error   #(dispatch [::messages-events/add
                                 {:header  (cond-> (str "error deleting " id)
                                                   (:status %) (str " (" (:status %) ")"))
                                  :content (or (:detail (:body %))
                                               (:message (:body %))
                                               "Unable to delete MEC subscription.")
                                  :type    :error}])]
      {::mec-subscriptions-request {:method          "DELETE"
                                    :subscription-id id
                                    :on-success      on-success
                                    :on-error        on-error}})))
