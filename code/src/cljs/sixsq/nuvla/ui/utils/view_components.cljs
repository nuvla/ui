(ns sixsq.nuvla.ui.utils.view-components
  (:require [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn status->color
  [status]
  (case status
    (true "STARTED" "started")        "green"
    (false "ERROR" "error") "red"
    "yellow"))


(defn OnlineStatusIcon
  [status corner]
  [ui/Icon {:name   "power"
            :corner (true? corner)
            :color  (status->color status)}])