(ns sixsq.nuvla.ui.routing.effects
  (:require [re-frame.core :refer [reg-fx]]))


(defn set-window-title!
  "Sets title"
  [url]
  (log/info "navigating to" url)
  (set! (.-title js/document) (str "Nuvla " url)))

(reg-fx
  ::set-window-title
  (fn [[url]]
    (set-window-title! url)))
