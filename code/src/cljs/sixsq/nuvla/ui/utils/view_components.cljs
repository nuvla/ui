(ns sixsq.nuvla.ui.utils.view-components
  (:require [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn status->color
  [status]
  (case status
    true "green"
    false "red"
    "yellow"))

(defn OnlineStatusIcon
  [online corner]
  [ui/Icon {:name   "power"
            :corner (true? corner)
            :color  (status->color online)}])