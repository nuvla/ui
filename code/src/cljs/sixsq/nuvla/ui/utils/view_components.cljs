(ns sixsq.nuvla.ui.utils.view-components
  (:require [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn status->color
  [status]
  (case status
    (true "STARTED" "started")        "green"
    (false "ERROR" "error") "red"
    "yellow"))


(defn OnlineStatusIcon
  [status corner solid?]
  [ui/Icon {:class  (if solid? icons/i-power-full icons/i-power)
            :corner (true? corner)
            :color  (status->color status)}])