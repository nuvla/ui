(ns sixsq.nuvla.ui.utils.view-components
  (:require [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn status->color
  [status]
  (case status
    (true "STARTED" "started") "green"
    (false "ERROR" "error")    "red"
    "yellow"))


(defn OnlineStatusIcon
  [status corner solid?]
  [ui/Icon {:class  (if solid? icons/i-power-full icons/i-power)
            :corner (true? corner)
            :color  (status->color status)}])

(defn TitledCard
  [{:keys [class icon label]} & children]
  [ui/Segment {:class     class
               :secondary true
               :raised    true
               :style {:height          "100%"
                       :display         "flex"
                       :flex-direction  "column"
                       :justify-content "space-evenly"}}
   [:h4 {:class [:ui-header :ui-card-header]}
    [icons/Icon {:name icon}] label]
   (into [:div {:style {:height          "100%"
                        :display         "flex"
                        :flex-direction  "column"
                        :justify-content "space-evenly"}}
          children])])
