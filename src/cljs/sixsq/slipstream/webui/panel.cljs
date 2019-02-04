(ns sixsq.slipstream.webui.panel
  (:require [sixsq.slipstream.webui.utils.semantic-ui :as ui]))

(defmulti render
          "Dispatches the rendering of a panel based on the first element of
           the path."
          (fn [path]
            (keyword (first path))))


(defmethod render :default
  [path query-parameters]
  (let [reason-text (str "Unknown resource: " path)]
    [ui/Container {:textAlign "center"}
     [ui/Header {:as   "h3"
                 :icon true}
      [ui/Icon {:name "warning sign"}]
      reason-text]]))
