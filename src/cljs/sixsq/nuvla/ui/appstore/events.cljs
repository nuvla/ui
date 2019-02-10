(ns sixsq.nuvla.ui.appstore.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.appstore.spec :as spec]
    [sixsq.nuvla.ui.appstore.utils :as utils]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.client.spec :as client-spec]))


(reg-event-db
  ::set-deployment-templates
  (fn [db [_ deployment-templates]]
    (assoc db ::spec/deployment-templates deployment-templates)))


(reg-event-fx
  ::get-deployment-templates
  (fn [{{:keys [::client-spec/client
                ::spec/full-text-search
                ::spec/page
                ::spec/elements-per-page] :as db} :db} _]
    (when client
      {:db                  (assoc db ::spec/deployment-templates nil)
       ::cimi-api-fx/search [client :deployment-template (utils/get-query-params full-text-search page elements-per-page)
                             #(dispatch [::set-deployment-templates %])]})))


(reg-event-fx
  ::set-full-text-search
  (fn [{{:keys [::client-spec/client
                ::spec/elements-per-page] :as db} :db} [_ full-text-search]]
    (let [new-page 1]
      {:db                  (assoc db ::spec/full-text-search full-text-search
                                      ::spec/page new-page)
       ::cimi-api-fx/search [client :deployment-template (utils/get-query-params full-text-search new-page elements-per-page)
                             #(dispatch [::set-deployment-templates %])]})))


(reg-event-fx
  ::set-page
  (fn [{{:keys [::client-spec/client
                ::spec/full-text-search
                ::spec/page
                ::spec/elements-per-page] :as db} :db} [_ page]]
    {:db                  (assoc db ::spec/page page)
     ::cimi-api-fx/search [client "deployment-template" (utils/get-query-params full-text-search page elements-per-page)
                           #(dispatch [::set-deployment-templates %])]}))
