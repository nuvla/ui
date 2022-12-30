(ns sixsq.nuvla.ui.history.utils
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]))

(defn navigate
  "Sets title"
  [url]
  (log/info "navigating to" url)
  ;; (.setToken history (str "/" url))
  (set! (.-title js/document) (str "Nuvla " url)))


(defn host-url
  "Extracts the host URL from the javascript window.location object."
  []
  (when-let [location (.-location js/window)]
    (let [protocol   (.-protocol location)
          host       (.-hostname location)
          port       (.-port location)
          port-field (when-not (str/blank? port) (str ":" port))]
      (str protocol "//" host port-field))))


(defn trim-path
  [path n]
  (str/join "/" (take (inc n) path)))
