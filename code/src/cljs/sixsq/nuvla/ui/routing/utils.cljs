(ns sixsq.nuvla.ui.routing.utils
  (:require [clojure.string :as str]
            [reitit.core :refer [match-by-path]]
            [reitit.frontend.easy :as rfe]
            [sixsq.nuvla.ui.config :as config]))

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (rfe/href k params query)))


(defn decode-query-string [path]
  (some->
   (second (str/split path #"\?"))
   (str/split #"&")
   (->> (map (fn [s] (let [[k v] (str/split s #"=")]
                       [(keyword k) v])))
        (into {}))))

(defn add-base-path
  [url]
  (let [ base-path (str config/base-path "/")
        absolute-url (if (str/starts-with? url base-path)
                       url
                       (str base-path (str/replace url #"^/" "")))]
    absolute-url))

(defn url->route-path-params [router url]
  (let [absolute-url (add-base-path url)
        [path _] (str/split absolute-url #"\?")
        match (match-by-path router path)
        name  (get-in match [:data :name])
        path-params (:path-params match)
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

(comment
  (into {} (-> (str/split "ui/apps?hello=world&world=ked&" #"\?")
               second
               (str/split #"&")
               (->> (map #(str/split % #"=")))))
  (into {} (-> (str/split "ui/apps?hello=world&world=fucked&ker" #"\?")
               second
               (str/split #"&")
               (->> (map (fn [s] (let [[k v] (str/split s #"=")]
                                   [k v]))))
               ))
  )