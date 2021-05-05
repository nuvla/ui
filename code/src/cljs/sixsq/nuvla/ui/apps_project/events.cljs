(ns sixsq.nuvla.ui.apps-project.events
  (:require
    [re-frame.core :refer [reg-event-db]]
    [sixsq.nuvla.ui.apps-component.spec :as spec]))

(reg-event-db
  ::clear-module
  (fn [db [_]]
    (merge db spec/defaults)))
