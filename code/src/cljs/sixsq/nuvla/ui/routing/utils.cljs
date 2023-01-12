(ns sixsq.nuvla.ui.routing.utils
  (:require [clojure.string :as str]
            [reitit.frontend.easy :as rfe]
            [sixsq.nuvla.ui.config :as config]))

(defn decode-query-string [path]
  (some->
    (second (str/split path #"\?"))
    (str/split #"&")
    (into {} (map (fn [s] (let [[k v] (str/split s #"=")]
                            [(keyword k) v]))))))


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


(defn pathify
  "Takes a list of path parts, returning a string of those parts separated by '/'."
  [path-parts]
  (str/join "/" path-parts))

(defn to-pathname
  "Takes a list of path parts, returning a string of those parts separated by '/'
   and with config/base-path appended."
  [path-parts]
  (pathify (cons config/base-path path-parts)))


(defn trim-path
  [path n]
  (str/join "/" (take (inc n) path)))
