(ns sixsq.slipstream.webui.docs.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.cimi-api.effects :as cimi-api-fx]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.docs.spec :as spec]))


(reg-event-fx
  ::get-documents
  (fn [{{:keys [::client-spec/client] :as db} :db} _]
    {:db                  (assoc db ::spec/loading? true
                                    ::spec/documents nil)
     ::cimi-api-fx/search [client
                           :resourceMetadatas
                           nil
                           #(dispatch [::set-documents %])]}))


(defn add-vscope-to-attributes
  [{:keys [vscope, attributes]  :as document}]
  (->> attributes
       (map (fn [{attribute-name :name :as attribute}]
              (assoc attribute :vscope (get vscope (keyword attribute-name)))))
       (assoc document :attributes)))


(reg-event-db
  ::set-documents
  (fn [db [_ docs]]
    (assoc db ::spec/loading? false
              ::spec/documents (->> docs
                                    :resourceMetadatas
                                    (map (juxt :id add-vscope-to-attributes))
                                    (into {})))))
