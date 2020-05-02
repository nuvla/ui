(ns sixsq.nuvla.ui.intercom.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.utils.intercom :as intercom]
    [sixsq.nuvla.ui.intercom.subs :as subs]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [taoensso.timbre :as log]))

(defn widget
  []
  (fn []
    (let [_      (subscribe [::main-subs/nav-path])
          app-id (subscribe [::subs/app-id])
          email  (subscribe [::session-subs/user])
          events (subscribe [::subs/events])]
      @_
      [intercom/Intercom (merge {:appID @app-id :email @email} @events)])))
