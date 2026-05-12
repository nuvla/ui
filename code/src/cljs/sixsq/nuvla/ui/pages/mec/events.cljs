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
   (str (mec-subscriptions-url)
        "/"
        (js/encodeURIComponent subscription-id))))

(defn- response-text->edn
  [text]
  (when-not (str/blank? text)
    (try
      (general-utils/json->edn text)
      (catch :default _
        {:message text}))))

(defn- normalize-subscription
  [subscription]
  {:id                 (:id subscription)
   :subscription-type  (:subscriptionType subscription)
   :callback-uri       (:callbackUri subscription)
   :app-instance-filter (:appInstanceFilter subscription)
   :app-lcm-op-occ-filter (:appLcmOpOccFilter subscription)
   :created            (:created subscription)
   :updated            (:updated subscription)
   :owner              (:owner subscription)
   :active             (:active subscription)})

(defn- ui-subscription->api
  [{:keys [subscription-type callback-uri app-instance-filter app-lcm-op-occ-filter]}]
  (cond-> {:subscriptionType subscription-type
           :callbackUri      callback-uri}
    (seq app-instance-filter) (assoc :appInstanceFilter app-instance-filter)
    (seq app-lcm-op-occ-filter) (assoc :appLcmOpOccFilter app-lcm-op-occ-filter)))

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

 (reg-event-fx
   ::init
   (fn [{db :db} _]
     {:db (merge db spec/defaults)
      :fx [[:dispatch [::refresh]]]}))

 (reg-event-fx
   ::refresh
   (fn [_ _]
     {:fx [[:dispatch [::get-mepms]]
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
       {:db (assoc db
              ::spec/subscriptions (mapv normalize-subscription
                                         (or (:items subscriptions)
                                             (:resources subscriptions)
                                             subscriptions
                                             []))
              ::spec/loading-subscriptions? false)})))

 (reg-event-fx
   ::get-subscriptions
   (fn [{db :db} _]
    (let [on-success #(dispatch [::set-subscriptions (:body %)])
          on-error   #(do
                        (dispatch [::messages-events/add
                                   {:header  (cond-> "failure getting MEC subscriptions"
                                                     (:status %) (str " (" (:status %) ")"))
                                    :content (or (:message (:body %))
                                                 "Unable to retrieve MEC subscriptions.")
                                    :type    :error}])
                        (dispatch [::set-subscriptions []]))]
      {:db                        (assoc db ::spec/loading-subscriptions? true)
       ::mec-subscriptions-request {:method     "GET"
                                    :on-success on-success
                                    :on-error   on-error}})))

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
