(ns sixsq.nuvla.ui.routing.utils
  (:require [clojure.string :as str]
            [reitit.frontend.easy :as rfe]
            [sixsq.nuvla.ui.config :as config]))

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

(defn strip-base-path [path]
  (-> path (str/replace-first config/base-path "")
      (str/replace #"^/|/$" "")))

(def alias->canonical {"nuvlabox"        "edges"
                       "edge"            "edges"
                       "infrastructures" "clouds"
                       "deployment"      "deployments"})

(defn split-path-alias
  [path]
  (let [path      (strip-base-path path)
        [page :as path-vec] (vec (str/split path #"/"))
        real-page (get alias->canonical page)]
    (if (and page real-page)
      (assoc path-vec 0 real-page)
      path-vec)))

(defn canonical->all-page-names
  ([canonical]
   (canonical->all-page-names canonical alias->canonical))
  ([canonical aliases-map]
   (->> aliases-map
        (filter #(= canonical (val %)))
        flatten
        set)))

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
