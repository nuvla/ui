(ns sixsq.slipstream.webui.client.events
  (:require
    [re-frame.core :refer [reg-event-db]]
    [sixsq.slipstream.client.async :as async-client]

    [sixsq.slipstream.webui.client.spec :as client-spec]))


(reg-event-db
  ::initialize
  (fn [db [_ slipstream-url]]
    (let [client (async-client/instance (str slipstream-url "/api/cloud-entry-point"))]
      (-> db
          (assoc ::client-spec/slipstream-url slipstream-url)
          (assoc ::client-spec/client client)))))
