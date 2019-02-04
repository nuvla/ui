(ns sixsq.nuvla.webui.client.events
  (:require
    [re-frame.core :refer [reg-event-db]]
    [sixsq.nuvla.client.async :as async-client]

    [sixsq.nuvla.webui.client.spec :as client-spec]))


(reg-event-db
  ::initialize
  (fn [db [_ nuvla]]
    (let [client (async-client/instance (str nuvla "/api/cloud-entry-point"))]
      (-> db
          (assoc ::client-spec/nuvla-url nuvla)
          (assoc ::client-spec/client client)))))
