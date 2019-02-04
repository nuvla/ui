(ns sixsq.slipstream.webui.quota.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.cimi-api.effects :as cimi-api-fx]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.quota.spec :as quota-spec]))


(reg-event-fx
  ::get-quotas
  (fn [{{:keys [::client-spec/client] :as db} :db} _]
    {:db                  (assoc db ::quota-spec/loading-quotas? true
                                    ::quota-spec/credentials-quotas-map {})
     ::cimi-api-fx/search [client
                           :quotas
                           {:$filter  "resource='VirtualMachine'"
                            :$select  "id, name, description, limit, resource, selection, aggregation"
                            :$orderby "created:desc"}
                           #(dispatch [::set-credentials-quotas-map %])]}))


(reg-event-db
  ::set-credentials-quotas-map
  (fn [db [_ response]]
    (let [quotas (get response :quotas [])]
      (-> db
          (assoc ::quota-spec/credentials-quotas-map (group-by :selection quotas))
          (assoc ::quota-spec/loading-quotas? false)))))


(reg-event-fx
  ::collect
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ id callback]]
    {::cimi-api-fx/operation [client
                              id
                              "http://sixsq.com/slipstream/1/action/collect"
                              callback]}))


(reg-event-fx
  ::get-credential-info
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ id callback]]
    {::cimi-api-fx/get [client
                        (str id "?$select=name,description")
                        callback]}))
