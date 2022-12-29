(ns sixsq.nuvla.ui.history.utils
  (:require
    [clojure.string :as str]
    [goog.events :as events]
    [secretary.core :as secretary]
    [taoensso.timbre :as log])
  (:import
    [goog.history EventType Html5History]
    [goog.history.Html5History TokenTransformer]))

(.-pathname (.-location js/window))
(str (.-protocol (.-location js/window)) "//" (.-host (.-location js/window)) (.-pathname (.-location js/window)))
;; => "https://nui.localhost/ui/edges"
(.-href (.-location js/window))
;; => "https://nui.localhost/ui/edges"


(defn get-token
  "Creates the history token from the given location excluding the query parameters."
  [path-prefix location]
  (let [url (str (.-protocol location) "//" (.-host location) (.-pathname location))]
    (str/replace-first url path-prefix "")))


(defn get-full-token
  "Creates the history token from the given location including the query parameters."
  [path-prefix location]
  (let [url (str (.-protocol location) "//" (.-host location) (.-pathname location) (.-search location))]
    (str/replace-first url path-prefix "")))


(defn create-transformer
  "Saves and restores the URL based on the token provided to the Html5History
   object. The methods of this object are needed when not using fragment based
   routing. The tokens are simply the remaining parts of the URL after the path
   prefix."
  []
  (let [transformer (TokenTransformer.)]
    (set! (.. transformer -retrieveToken)
          (fn [path-prefix location]
            (get-token path-prefix location)))
    (set! (.. transformer -createUrl)
          (fn [token path-prefix _location]
            (str path-prefix token)))
    transformer))


(def history
  (doto (Html5History. js/window (create-transformer))
    (events/listen EventType.NAVIGATE (fn [evt] (secretary/dispatch! (.-token evt))))
    (.setUseFragment false)
    (.setEnabled false)))


(defn initialize
  "Sets the path-prefix to use for the history object and enables the object
   to start sending events on token changes."
  [path-prefix]
  (doto history
    (.setPathPrefix path-prefix)
    (.setEnabled true)))


(defn start
  "Sets the starting point for the history. No history event will be generated
   when setting the first value, so this explicitly dispatches the value to the
   URL routing."
  [path-prefix]
  (let [location   (.-location js/window)
        token      (get-token path-prefix location)
        full-token (get-full-token path-prefix location)
        redirect   (when (= token "/") "/sign-in")]
    (log/info "start token: " token (when redirect (str "redirect to " redirect)))
    (.setToken history (or redirect token))
    (secretary/dispatch! (or redirect full-token))))


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
