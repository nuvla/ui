(ns sixsq.nuvla.ui.utils.view-components)

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