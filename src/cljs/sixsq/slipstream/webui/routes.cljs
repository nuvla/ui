(ns sixsq.slipstream.webui.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:require
    [re-frame.core :refer [dispatch]]
    [secretary.core :refer [defroute]]
    [sixsq.slipstream.webui.authn.events :as authn-events]

    [sixsq.slipstream.webui.main.events :as main-events]
    [taoensso.timbre :as log]))

(defn routes []

  (defroute "/*" {path :* query-params :query-params}
            (log/debug "routing /*:" path query-params)
            (when-let [error (:error query-params)]
              (dispatch [::authn-events/set-error-message error])
              (dispatch [::authn-events/open-modal :login]))
            (when (not-empty path)
              (dispatch [::main-events/set-navigation-info path query-params])))

  (defroute "*" {path :*}
            (log/debug "routing *:" path)
            (when (not-empty path)
              (dispatch [::main-events/set-navigation-info path nil]))))
