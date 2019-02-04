(ns sixsq.slipstream.webui.legacy-application.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.legacy-application.effects :as application-fx]
    [sixsq.slipstream.webui.legacy-application.spec :as spec]
    [sixsq.slipstream.webui.main.spec :as main-spec]))


(reg-event-db
  ::set-module
  (fn [db [_ module-id module]]
    (assoc db ::spec/completed? true
              ::spec/module-id module-id
              ::spec/module module)))


(reg-event-fx
  ::get-module
  (fn [{{:keys [::client-spec/client ::main-spec/nav-path] :as db} :db} _]
    (when client
      (let [path (some->> nav-path rest seq (str/join "/"))]
        {:db                         (assoc db ::spec/completed? false
                                               ::spec/module-id nil
                                               ::spec/module nil)
         ::application-fx/get-module [client path]}))))
