(ns sixsq.nuvla.ui.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:require
    [re-frame.core :refer [dispatch]]
    [secretary.core :refer [defroute]]
    [sixsq.nuvla.ui.main.events :as main-events]
    [taoensso.timbre :as log]))

(defn routes []

  #_ :clj-kondo/ignore
  (defroute "/*" {path :* query-params :query-params}
            (log/debug "routing /*:" path query-params)
            (when (not-empty path)
              (dispatch [::main-events/set-navigation-info path query-params])))

  #_ :clj-kondo/ignore
  (defroute "*" {path :*}
            (log/debug "routing *:" path)
            (when (not-empty path)
              (dispatch [::main-events/set-navigation-info path nil]))))
