(ns sixsq.nuvla.ui.routing.utils
  (:require [clojure.string :as str]
            [reitit.frontend.easy :as rfe]
            [sixsq.nuvla.ui.config :as config]))

(defn name->href
  "Return relative url for given route. Url can be used in HTML links."
  ([{:keys [route-name
            path-params
            query-params] :as k}]
   (if route-name
     (name->href route-name path-params query-params)
     (name->href k nil nil)))
  ([k params]
   (name->href k params nil))
  ([k params query]
   (some->
     (rfe/href k params query)
     js/decodeURIComponent)))

(defn get-route-name
  [route]
  (get-in route [:data :name]))

(defn db-path->query-param-key
  [[qualified-key]]
  (let [ns-path     (str/split (namespace qualified-key) #"\.")
        last-two-ns (drop (- (count ns-path) 2) ns-path)
        k-prefix     (str/replace (str/join last-two-ns) "spec" "")]
    (->> qualified-key
         name
         (str k-prefix "-")
         keyword)))

(defn new-route-data
  "Takes current route data and merges it with partial new route data, returning new
  route data map with :route-name, :path-params, :query-params
  usable to create new path or to pass to reitit/match-by-name.

  Parameters:
  - route-data       Map with route-name at [:data :name] plus maps of
                     :path-params and :query-params.
  - new-route-data   Map with :route-name, :path-params, :query-params, :partial-query-params."

  [current-route-data {:keys [route-name path-params query-params partial-query-params]}]
  {:route-name   (or route-name (get-route-name current-route-data))
   :path-params  (or path-params (:path-params current-route-data))
   :query-params (merge (or query-params (:query-params current-route-data))
                            partial-query-params)})

(defn gen-href
  "Takes current route data and merges it with partial new route data, returning new path.
  Useful to only change parts of current path:

  Examples:
  - changing one query param:  (gen-href route-data
                                         {:partial-query-params {:search 'hello'}})
  - removing all query params: (gen-href route-data
                                         {:query-params {}})
  - changing path segments:    (gen-href route-data
                                         {:path-params {:uuid 'new-id'}})

  Parameters:
  - route-data       Map with route-name at [:data :name] plus maps of
                     :path-params and :query-params.
  - new-route-data   Map with :route-name, :path-params, :query-params, :partial-query-params."

  [route-data new-partial-route-data]
  (let [{:keys [route-name
                path-params
                query-params]} (new-route-data route-data new-partial-route-data)]
    (name->href route-name path-params query-params)))

(defn get-query-param
  [route key]
  (get-in route [:query-params key]))

(defn get-stored-db-value-from-query-param
  [route db-path]
  (get-query-param route (db-path->query-param-key db-path)))

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
  "Takes a seq of path parts, returning a string of those parts separated by '/'."
  [path-parts]
  (str/join "/" path-parts))

(defn str-pathify
  "Takes a variable length of path parts arguments, returning a string of those parts separated by '/'."
  [& path-parts]
  (pathify path-parts))

(defn to-pathname
  "Takes a seq of path parts, returning a string of those parts separated by '/'
   and with config/base-path appended."
  [path-parts]
  (pathify (cons config/base-path path-parts)))

(defn trim-path
  [path n]
  (str/join "/" (take (inc n) path)))
