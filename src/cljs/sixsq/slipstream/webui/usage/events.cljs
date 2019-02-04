(ns sixsq.slipstream.webui.usage.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.cimi-api.effects :as cimi-api-fx]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.usage.effects :as usage-fx]
    [sixsq.slipstream.webui.usage.spec :as usage-spec]))


(defn get-credentials-map-cofx-for-callback
  [callback client selected-users-roles]
  (let [users-roles-filter (->> selected-users-roles
                                (map #(str "acl/owner/principal = '" % "' or acl/rules/principal = '" % "'"))
                                (str/join " or "))
        filter-str (cond-> "type^='cloud-cred-'"
                           (not-empty selected-users-roles) (str " and (" users-roles-filter ")"))]
    {::cimi-api-fx/search [client
                           :credentials
                           {:$select "id,name,description,connector"
                            :$filter filter-str}
                           callback]}))


(def get-credentials-map-cofx (partial get-credentials-map-cofx-for-callback #(dispatch [::set-credentials-map %])))


(reg-event-fx
  ::get-credentials-map
  (fn [{{:keys [::client-spec/client
                ::usage-spec/selected-users-roles] :as db} :db} _]
    (merge {:db (assoc db ::usage-spec/credentials-map nil
                          ::usage-spec/selected-credentials nil
                          ::usage-spec/loading-credentials-map? true
                          ::usage-spec/totals nil
                          ::usage-spec/results nil)})
    (get-credentials-map-cofx client selected-users-roles)))


(reg-event-fx
  ::initialize
  (fn [{{:keys [::client-spec/client
                ::usage-spec/selected-users-roles] :as db} :db} _]
    (let [callback (fn [creds]
                     (dispatch [::set-credentials-map creds])
                     (dispatch [::fetch-data-with-creds creds]))]
      (merge {:db (assoc db ::usage-spec/loading-credentials-map? true
                            ::usage-spec/loading-totals? false
                            ::usage-spec/loading-details? false
                            ::usage-spec/credentials-map nil
                            ::usage-spec/selected-credentials nil
                            ::usage-spec/totals nil
                            ::usage-spec/results nil)}
             (get-credentials-map-cofx-for-callback callback client selected-users-roles)))))


(defn response->credentials-map
  [response]
  (->> (get response :credentials [])
       (map #(vector (:id %) %))
       (into {})))


(reg-event-db
  ::set-credentials-map
  (fn [db [_ response]]
    (let [credentials-map (response->credentials-map response)]
      (assoc db ::usage-spec/credentials-map credentials-map
                ::usage-spec/loading-credentials-map? false
                ::usage-spec/totals nil
                ::usage-spec/results nil))))


(reg-event-db
  ::set-selected-credentials
  (fn [db [_ credentials]]
    (assoc db ::usage-spec/selected-credentials credentials
              ::usage-spec/totals nil
              ::usage-spec/results nil)))


(reg-event-fx
  ::set-users-roles
  (fn [{{:keys [::client-spec/client
                ::usage-spec/selected-users-roles] :as db} :db} [_ user]]
    (merge {:db (assoc db ::usage-spec/selected-users-roles user
                          ::usage-spec/loading-credentials-map? true
                          ::usage-spec/credentials-map nil
                          ::usage-spec/selected-credentials nil
                          ::usage-spec/results nil
                          ::usage-spec/totals nil)}
           (get-credentials-map-cofx client user))))


(reg-event-db
  ::push-users-roles-list
  (fn [{:keys [::usage-spec/users-roles-list] :as db} [_ user-or-role]]
    (if user-or-role
      (->> {:key user-or-role, :value user-or-role, :text user-or-role}
           (conj users-roles-list)
           (sort-by :key)
           (assoc db ::usage-spec/users-roles-list))
      db)))


(reg-event-db
  ::set-date-range
  (fn [db [_ date-range]]
    (assoc db ::usage-spec/date-range date-range
              ::usage-spec/totals nil
              ::usage-spec/results nil)))


(reg-event-db
  ::set-totals
  (fn [db [_ totals]]
    (assoc db ::usage-spec/loading-totals? false
              ::usage-spec/totals totals)))


(reg-event-db
  ::set-results
  (fn [db [_ results]]
    (assoc db ::usage-spec/loading-details? false
              ::usage-spec/results results)))


(reg-event-fx
  ::fetch-data
  (fn [{{:keys [::client-spec/client
                ::usage-spec/loading-totals?
                ::usage-spec/loading-details?
                ::usage-spec/date-range
                ::usage-spec/selected-credentials
                ::usage-spec/credentials-map
                ::usage-spec/loading-credentials-map?
                ::usage-spec/billable-only?] :as db} :db}]
    (when-not (or loading-totals? loading-details? loading-credentials-map?)
      (let [start (-> date-range first .clone .utc .format)
            end (-> date-range second .clone .utc .format)

            creds (if (empty? selected-credentials)
                    (keys credentials-map)
                    selected-credentials)]
        {:db                        (assoc db ::usage-spec/loading-totals? true
                                              ::usage-spec/loading-details? true
                                              ::usage-spec/totals nil
                                              ::usage-spec/results nil)

         ::usage-fx/fetch-totals    [client
                                     start
                                     end
                                     creds
                                     billable-only?
                                     #(dispatch [::set-totals %])]

         ::usage-fx/fetch-meterings [client
                                     start
                                     end
                                     creds
                                     billable-only?
                                     #(dispatch [::set-results %])]}))))


(reg-event-fx
  ::fetch-data-with-creds
  (fn [{{:keys [::client-spec/client
                ::usage-spec/date-range
                ::usage-spec/billable-only?] :as db} :db} [_ response]]
    (let [credentials-map (response->credentials-map response)

          start (-> date-range first .clone .utc .format)
          end (-> date-range second .clone .utc .format)

          creds (keys credentials-map)]

      {:db                        (assoc db ::usage-spec/initialized? true
                                            ::usage-spec/loading-totals? true
                                            ::usage-spec/loading-details? true
                                            ::usage-spec/totals nil
                                            ::usage-spec/results nil)

       ::usage-fx/fetch-totals    [client
                                   start
                                   end
                                   creds
                                   billable-only?
                                   #(dispatch [::set-totals %])]

       ::usage-fx/fetch-meterings [client
                                   start
                                   end
                                   creds
                                   billable-only?
                                   #(dispatch [::set-results %])]})))


(reg-event-db
  ::set-sort
  (fn [{:keys [::usage-spec/sort] :as db} [_ new-column]]
    (let [{:keys [:column :direction]} sort
          new-direction (if (= new-column column)
                          (if (= direction :ascending)
                            :descending
                            :ascending)
                          :descending)]
      (assoc db ::usage-spec/sort {:column    new-column
                                   :direction new-direction}))))


(reg-event-db
  ::toggle-billable-only?
  (fn [{:keys [::usage-spec/billable-only?] :as db} _]
    (assoc db ::usage-spec/billable-only? (not billable-only?)
              ::usage-spec/totals nil
              ::usage-spec/results nil)))
