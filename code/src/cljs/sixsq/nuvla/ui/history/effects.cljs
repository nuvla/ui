(ns sixsq.nuvla.ui.history.effects
  (:require
    [re-frame.core :refer [reg-fx]]
    [sixsq.nuvla.ui.history.utils :as utils]
    [taoensso.timbre :as log]))


(reg-fx
  ::initialize
  (fn [[path-prefix]]
    (utils/initialize path-prefix)
    (utils/start path-prefix)))


(reg-fx
  ::navigate
  (fn [[url]]
    (log/warn "navigate effect")
    (utils/navigate url)))

#_(reg-fx
    ::navigate-js-location
    (fn [[url]]
      (.replace js/location url)))

