(ns sixsq.nuvla.ui.unknown-resource
  (:require [re-frame.core :refer [subscribe]]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn UnknownResource
  [_match]
  (let [nav-path    (subscribe [::route-subs/nav-path])
        reason-text (str "Unknown resource: " @nav-path)]
    [ui/Container {:textAlign "center"}
     [ui/Header {:as   "h3"
                 :icon true}
      [icons/WarningIcon]
      reason-text]]))
