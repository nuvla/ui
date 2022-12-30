(ns sixsq.nuvla.ui.history.effects
  (:require
    [re-frame.core :refer [reg-fx]]
    [sixsq.nuvla.ui.history.utils :as utils]))


(reg-fx
  ::initialize
  (fn [[path-prefix]]
    #_(utils/initialize path-prefix)
    #_(utils/start path-prefix)))

(reg-fx
  ::navigate
  (fn [[url]]
    (utils/navigate url)))

#_(reg-fx
    ::navigate-js-location
    (fn [[url]]
      (.replace js/location url)))
