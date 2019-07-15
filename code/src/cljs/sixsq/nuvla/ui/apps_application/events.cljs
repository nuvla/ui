(ns sixsq.nuvla.ui.apps-application.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps-application.spec :as spec]))


(reg-event-db
  ::clear-module
  (fn [db [_]]
    (merge db spec/defaults)))

; Docker compose

(reg-event-db
  ::update-docker-compose
  (fn [db [_ id docker-compose]]
    (assoc-in db [::spec/module-application ::spec/docker-compose] docker-compose)))

