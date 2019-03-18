(ns sixsq.nuvla.ui.module-project.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.module-project.spec :as spec]
    [sixsq.nuvla.ui.application.spec :as application-spec]
    [sixsq.nuvla.ui.application.events :as application-events]
    [taoensso.timbre :as log]))
