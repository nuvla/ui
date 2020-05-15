(ns sixsq.nuvla.ui.intercom.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.intercom.subs :as subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.intercom :as intercom]))

(defn widget
  []
  (fn []
    (let [_      (subscribe [::main-subs/nav-path])
          app-id (subscribe [::main-subs/config :intercom-app-id])
          email  (subscribe [::session-subs/user])
          events (subscribe [::subs/events])]
      @_                                                    ;to force the component to refresh
      [intercom/Intercom (merge
                           {:appID (or @app-id "")}
                           (when (not (nil? @email)) {:email @email})
                           @events)])))
