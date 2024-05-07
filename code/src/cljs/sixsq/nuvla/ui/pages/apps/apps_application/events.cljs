(ns sixsq.nuvla.ui.pages.apps.apps-application.events
  (:require [ajax.core :as ajax]
            [re-frame.core :refer [reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.pages.apps.apps-application.spec :as spec]
            [sixsq.nuvla.ui.pages.apps.utils :as utils]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.timeseries :as ts-utils]))


(reg-event-db
  ::clear-apps-application
  (fn [db [_]]
    (merge db spec/defaults)))


; Files

(reg-event-db
  ::add-file
  (fn [db [_ _]]
    (let [id (-> db
                 (get-in [::spec/module-application ::spec/files])
                 utils/sorted-map-new-idx)]
      (assoc-in db [::spec/module-application ::spec/files id] {:id id, ::spec/file-content ""}))))


(reg-event-db
  ::remove-file
  (fn [db [_ id]]
    (update-in db [::spec/module-application ::spec/files] dissoc id)))


(reg-event-db
  ::update-file-name
  (fn [db [_ id file-name]]
    (assoc-in db [::spec/module-application ::spec/files id
                  ::spec/file-name] file-name)))


(reg-event-db
  ::update-file-content
  (fn [db [_ id file-content]]
    (assoc-in db [::spec/module-application ::spec/files id
                  ::spec/file-content] file-content)))


; Docker compose

(reg-event-db
  ::update-docker-compose
  (fn [db [_ docker-compose]]
    (assoc-in db [::spec/module-application ::spec/docker-compose] docker-compose)))

(reg-event-db
  ::update-compatibility
  (fn [db [_ compatibility]]
    (assoc-in db [::spec/module-application ::spec/compatibility] compatibility)))


; Validation errors

(reg-event-db
  ::set-license-validation-error
  (fn [db [_ key error?]]
    (utils/set-reset-error db key error? ::spec/license-validation-errors)))


(reg-event-db
  ::set-docker-validation-error
  (fn [db [_ key error?]]
    (utils/set-reset-error db key error? ::spec/docker-compose-validation-errors)))


(reg-event-db
  ::set-configuration-validation-error
  (fn [db [_ key error?]]
    (utils/set-reset-error db key error? ::spec/configuration-validation-errors)))

; Requires user rights

(reg-event-db
  ::update-requires-user-rights
  (fn [db [_ value]]
    (assoc-in db [::spec/module-application ::spec/requires-user-rights] value)))

#_(def ts-id "timeseries/f9f76bdd-56e9-4dde-bbcf-30d1b84625e0")

(def ts-id "timeseries/ff01ecae-e1e7-4baa-bab7-6ff1adf72dad")

(def query-name "test-query1")

(reg-event-fx
  ::fetch-app-data-success
  (fn [{db :db} [_ response]]
    {:db (assoc db ::spec/loading? false
                   ::spec/app-data response)}))

(reg-event-fx
  ::fetch-app-data-failure
  (fn [{db :db} [_ response]]
    {:db (assoc db ::spec/loading? false)}))

(reg-event-fx
  ::fetch-app-data-csv-success
  (fn [{db :db} [_ response]]
    {:db (assoc db ::spec/loading? false)
     :fx [[:dispatch [::main-events/open-link (str "data:text/csv," response)]]]}))

(reg-event-fx
  ::fetch-app-data-csv
  (fn [{db :db} [_ {:keys [from to granularity query]}]]
    {:db         (assoc db ::spec/loading? true)
     :http-xhrio {:method          :get
                  :uri             (str "/api/" ts-id "/data")
                  :params          {:query       query
                                    :from        (time/time->utc-str from)
                                    :to          (time/time->utc-str to)
                                    :granularity granularity}
                  :headers         {"Accept" "text/csv"}
                  :request-format  (ajax/json-request-format)
                  :response-format (ajax/text-response-format)
                  :on-success      [::fetch-app-data-csv-success]
                  :on-failure      [::fetch-app-data-failure]}}))

(reg-event-fx
  ::fetch-app-data
  (fn [{db :db} [_ {:keys [from to granularity query]}]]
    (let []
      {:db         (assoc db ::spec/loading? true)
       :http-xhrio {:method          :get
                    :uri             (str "/api/"ts-id"/data")
                    :params          {:query query
                                      :from  (time/time->utc-str from)
                                      :to    (time/time->utc-str to)
                                      :granularity granularity}
                    :request-format  (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::fetch-app-data-success]
                    :on-failure      [::fetch-app-data-failure]}})))

(reg-event-fx
  ::set-selected-timespan
  (fn [{db :db} [_ timespan]]
    (let [{:keys [from to]} timespan]
      {:db (assoc db ::spec/timespan timespan)
       :fx [[:dispatch [::fetch-app-data {:from        from
                                          :to          to
                                          :granularity (ts-utils/granularity-for-timespan timespan)
                                          :query query-name }]]]})))
