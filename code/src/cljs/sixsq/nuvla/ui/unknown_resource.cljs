(ns sixsq.nuvla.ui.unknown-resource
  (:require [re-frame.core :refer [subscribe]]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn UnknownResource
  [_match]
  (let [nav-path    (subscribe [::main-subs/nav-path])
        reason-text (str "Unknown resource: " @nav-path)]
    [ui/Container {:textAlign "center"}
     [ui/Header {:as   "h3"
                 :icon true}
      [ui/Icon {:name "warning sign"}]
      reason-text]]))
