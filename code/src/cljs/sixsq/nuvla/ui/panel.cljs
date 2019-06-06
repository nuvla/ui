(ns sixsq.nuvla.ui.panel
  (:require
    [re-frame.core :refer [subscribe]]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defmulti render
          "Dispatches the rendering of a panel based on the first element of
           the path."
          (fn [path]
            (keyword (first path))))


(defmethod render :default
  [path query-parameters]
  (let [nav-path    (subscribe [::main-subs/nav-path])
        reason-text (str "Unknown resource: " @nav-path)]
    [ui/Container {:textAlign "center"}
     [ui/Header {:as   "h3"
                 :icon true}
      [ui/Icon {:name "warning sign"}]
      reason-text]]))
