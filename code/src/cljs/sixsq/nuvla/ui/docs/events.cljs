(ns sixsq.nuvla.ui.docs.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.docs.spec :as spec]))


(reg-event-fx
  ::get-documents
  (fn [{:keys [db]} _]
    {:db                  (assoc db ::spec/loading? true
                                    ::spec/documents nil)
     ::cimi-api-fx/search [:resource-metadata
                           nil
                           #(dispatch [::set-documents %])]}))


(reg-event-db
  ::set-documents
  (fn [db [_ {:keys [resources] :as _docs}]]
    (assoc db ::spec/loading? false
              ::spec/documents (->> resources
                                    (map (juxt :id identity))
                                    (into {})))))
