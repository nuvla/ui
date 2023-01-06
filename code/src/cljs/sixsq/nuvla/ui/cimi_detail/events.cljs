(ns sixsq.nuvla.ui.cimi-detail.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi-detail.spec :as cimi-detail-spec]
    [sixsq.nuvla.ui.cimi.events :as cimi-events]
    [sixsq.nuvla.ui.cimi.spec :as cimi-spec]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.routing.utils :refer [name->href]]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-event-fx
  ::get
  (fn [{{:keys [::cimi-spec/collection-name] :as db} :db} [_ resource-id]]
    {:db               (assoc db ::cimi-detail-spec/loading? true
                                 ::cimi-detail-spec/resource-id resource-id)
     ::cimi-api-fx/get [resource-id #(dispatch [::set-resource %])
                        :on-error #(do
                                     (cimi-api-fx/default-get-on-error resource-id %)
                                     (dispatch [::history-events/navigate
                                                (str (name->href :api-slashed) collection-name)]))]}))


(reg-event-db
  ::set-resource
  (fn [db [_ {:keys [id] :as resource}]]
    (assoc db ::cimi-detail-spec/loading? false
              ::cimi-detail-spec/resource-id id
              ::cimi-detail-spec/resource resource)))


(reg-event-fx
  ::delete
  (fn [{{:keys [::cimi-spec/collection-name]} :db} [_ resource-id]]
    {::cimi-api-fx/delete [resource-id
                           #(let [{:keys [status message]} (response/parse %)]
                              (dispatch [::messages-events/add
                                         {:header  (cond-> (str "deleted " resource-id)
                                                           status (str " (" status ")"))
                                          :content message
                                          :type    :success}])
                              (dispatch [::history-events/navigate (str (name->href :api-slashed) collection-name)])
                              (dispatch [::cimi-events/get-results]))]}))


(reg-event-fx
  ::edit
  (fn [_ [_ resource-id data]]
    {::cimi-api-fx/edit [resource-id data
                         #(if (instance? js/Error %)
                            (let [{:keys [status message]} (response/parse-ex-info %)]
                              (dispatch [::messages-events/add
                                         {:header  (cond-> (str "error editing " resource-id)
                                                           status (str " (" status ")"))
                                          :content message
                                          :type    :error}]))
                            (dispatch [::set-resource %]))]}))


(reg-event-fx
  ::operation
  (fn [_ [_ resource-id operation data]]
    (let [on-success #(let [{:keys [status message]} (response/parse %)]
                        (dispatch [::messages-events/add
                                   {:header  (cond-> (str "success executing operation " operation)
                                                     status (str " (" status ")"))
                                    :content message
                                    :type    :success}]))]
      {::cimi-api-fx/operation [resource-id operation on-success :data data]})))
