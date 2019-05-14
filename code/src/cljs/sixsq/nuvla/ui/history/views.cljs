(ns sixsq.nuvla.ui.history.views
  (:require
    [re-frame.core :refer [dispatch]]
    [sixsq.nuvla.ui.config :as config]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn link
  "Renders a link that will navigate to the given href when clicked. The href
   value will also be used as the label, unless an explicit label is provided."
  [href & [label]]
  [:a {:href     (str @config/path-prefix "/" href)
       :on-click (fn [event]
                   (dispatch [::history-events/navigate href])
                   (.preventDefault event))}
   (or label href)])


(defn icon-link
  [href]
  (vec (conj (link href "")
             [ui/Icon {:name "external alternate"
                       :size "small"}])))
