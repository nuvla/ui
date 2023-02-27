(ns sixsq.nuvla.ui.routing.effects
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-fx]]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.history :as rfh]
            [taoensso.timbre :as log]))

(defn- call-navigate!
  [path]
  (rfh/-on-navigate @rfe/history path))

(defn host-url
  "Extracts the host URL from the javascript window.location object."
  []
  (when-let [location (.-location js/window)]
    (let [protocol   (.-protocol location)
          host       (.-hostname location)
          port       (.-port location)
          port-field (when-not (str/blank? port) (str ":" port))]
      (str protocol "//" host port-field))))

(reg-fx
  ::set-window-title
  (fn [[url]]
    (log/info "navigating to" url)
    (set! (.-title js/document) (str "Nuvla " url))))

;; Triggering navigation from events by using js/window.history.pushState directly,
;; expects a string as path argument
(reg-fx
  ::push-state
  (fn [path]
    ;; .pushState does not call popState, that's why we have to call rfh/-on-navigate
    ;; when navigating by raw path (from reitit source)
    (.pushState js/window.history nil {} path)
    (call-navigate! path)))


(reg-fx
  ::navigate-back!
  (fn []
    (.back js/window.history)))

(defn- replace-state!
  [path]
  (.replaceState js/window.history nil {} path))


(reg-fx
  ::replace-state
  (fn [path]
    ;; .replaceState does not call popState, that's why we have to call rfh/-on-navigate
    (replace-state! path)
    (call-navigate! path)))

(reg-fx
  ::replace-state-without-navigation
  (fn [path]
    (replace-state! path)))