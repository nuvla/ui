(ns sixsq.nuvla.ui.cimi-detail.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi-detail.spec :as cimi-detail-spec]
    [sixsq.nuvla.ui.cimi.spec :as cimi-spec]
    [sixsq.nuvla.ui.client.spec :as client-spec]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))


(reg-event-fx
  ::get
  (fn [{{:keys [::client-spec/client
                ::cimi-spec/collection-name] :as db} :db} [_ resource-id]]
    (when client
      {:db               (assoc db ::cimi-detail-spec/loading? true
                                   ::cimi-detail-spec/resource-id resource-id)
       ::cimi-api-fx/get [client resource-id #(if (instance? js/Error %)
                                                (let [{:keys [status message]} (response/parse-ex-info %)]
                                                  (dispatch [::messages-events/add
                                                             {:header  (cond-> (str "error getting " resource-id)
                                                                               status (str " (" status ")"))
                                                              :content message
                                                              :type    :error}])
                                                  (dispatch [::history-events/navigate
                                                             (str "api/" collection-name)]))
                                                (dispatch [::set-resource %]))]})))


(reg-event-db
  ::set-resource
  (fn [{:keys [::client-spec/client
               ::cimi-spec/collection-name
               ::cimi-spec/cloud-entry-point] :as db} [_ {:keys [id] :as resource}]]
    (assoc db ::cimi-detail-spec/loading? false
              ::cimi-detail-spec/resource-id id
              ::cimi-detail-spec/resource resource)))


(reg-event-fx
  ::delete
  (fn [{{:keys [::client-spec/client
                ::cimi-spec/collection-name] :as db} :db} [_ resource-id]]
    (when client
      {::cimi-api-fx/delete [client resource-id
                             #(if (instance? js/Error %)
                                (let [{:keys [status message]} (response/parse-ex-info %)]
                                  (dispatch [::messages-events/add
                                             {:header  (cond-> (str "error deleting " resource-id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :error}]))
                                (let [{:keys [status message]} (response/parse %)]
                                  (dispatch [::messages-events/add
                                             {:header  (cond-> (str "deleted " resource-id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :success}])
                                  (dispatch [::history-events/navigate (str "api/" collection-name)])))]})))


(reg-event-fx
  ::edit
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ resource-id data]]
    (when client
      {::cimi-api-fx/edit [client resource-id data
                           #(if (instance? js/Error %)
                              (let [{:keys [status message]} (response/parse-ex-info %)]
                                (dispatch [::messages-events/add
                                           {:header  (cond-> (str "error editing " resource-id)
                                                             status (str " (" status ")"))
                                            :content message
                                            :type    :error}]))
                              (dispatch [::set-resource %]))]})))


(reg-event-fx
  ::operation
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ resource-id operation]]
    {::cimi-api-fx/operation [client resource-id operation
                              #(let [op (second (re-matches #"(?:.*/)?(.*)" operation))]
                                 (if (instance? js/Error %)
                                   (let [{:keys [status message]} (response/parse-ex-info %)]
                                     (dispatch [::messages-events/add
                                                {:header  (cond-> (str "error executing operation " op)
                                                                  status (str " (" status ")"))
                                                 :content message
                                                 :type    :error}]))
                                   (let [{:keys [status message]} (response/parse %)]
                                     (dispatch [::messages-events/add
                                                {:header  (cond-> (str "success executing operation " op)
                                                                  status (str " (" status ")"))
                                                 :content message
                                                 :type    :success}]))))]}))
