(ns sixsq.slipstream.webui.history.effects
  (:require
    [re-frame.core :refer [reg-fx]]
    [sixsq.slipstream.webui.history.utils :as utils]))


(reg-fx
  ::initialize
  (fn [[path-prefix]]
    (utils/initialize path-prefix)
    (utils/start path-prefix)))


(reg-fx
  ::navigate
  (fn [[url]]
    (utils/navigate url)))

(reg-fx
  ::navigate-js-location
  (fn [[url]]
    (.replace js/location url)))

(reg-fx
  ::replace-url-history
  (fn [[url]]
    (utils/replace-url-history url)))
