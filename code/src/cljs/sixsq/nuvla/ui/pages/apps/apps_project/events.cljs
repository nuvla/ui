(ns sixsq.nuvla.ui.pages.apps.apps-project.events
  (:require [re-frame.core :refer [reg-event-db]]
            [sixsq.nuvla.ui.pages.apps.apps-component.spec :as spec]))

(reg-event-db
  ::clear-apps-project
  (fn [db [_]]
    (merge db spec/defaults)))
