(ns sixsq.nuvla.ui.routing.utils
  (:require [clojure.string :as str]
            [reitit.core :as r :refer [match-by-path]]
            [reitit.frontend.easy :as rfe]
            [sixsq.nuvla.ui.config :as config]))

(defn decode-query-string [path]
  (some->
    (second (str/split path #"\?"))
    (str/split #"&")
    (->> (map (fn [s] (let [[k v] (str/split s #"=")]
                        [(keyword k) v])))
         (into {}))))


(defn name->href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (name->href k nil nil))
  ([k params]
   (name->href k params nil))
  ([k params query]
   (rfe/href k params query)))


(defn add-base-path
  [url]
  (let [base-path    (str config/base-path "/")
        absolute-url (if (str/starts-with? url base-path)
                       url
                       (str base-path (str/replace url #"^/" "")))]
    absolute-url))

(defn url->route-path-params [router url]
  (let [absolute-url (add-base-path url)
        [path _] (str/split absolute-url #"\?")
        match        (match-by-path router path)
        name         (get-in match [:data :name])
        path-params  (:path-params match)
        query-params (decode-query-string absolute-url)]
    [name path-params query-params]))

(defn pathify
  "Takes a list of path parts, returning a string of those parts separated by '/'."
  [path-parts]
  (str/join "/" path-parts))

(defn to-pathname
  "Takes a list of path parts, returning a string of those parts separated by '/'
   and with config/base-path appended."
  [path-parts]
  (pathify (concat [config/base-path] path-parts)))

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
