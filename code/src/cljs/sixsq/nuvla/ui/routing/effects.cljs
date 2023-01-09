(ns sixsq.nuvla.ui.routing.effects
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-fx]]
            [taoensso.timbre :as log]))

(defn host-url
  "Extracts the host URL from the javascript window.location object."
  []
  (when-let [location (.-location js/window)]
    (let [protocol   (.-protocol location)
          host       (.-hostname location)
          port       (.-port location)
          port-field (when-not (str/blank? port) (str ":" port))]
      (str protocol "//" host port-field))))

(defn set-window-title!
  "Sets title"
  [url]
  (log/info "navigating to" url)
  (set! (.-title js/document) (str "Nuvla " url)))

(reg-fx
  ::set-window-title
  (fn [[url]]
    (set-window-title! url)))
