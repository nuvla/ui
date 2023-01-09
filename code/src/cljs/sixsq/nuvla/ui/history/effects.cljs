(ns sixsq.nuvla.ui.history.effects
  (:require [re-frame.core :refer [reg-fx]]
            [sixsq.nuvla.ui.history.utils :as utils]))


(reg-fx
  ::set-window-title
  (fn [[url]]
    (utils/set-window-title! url)))
