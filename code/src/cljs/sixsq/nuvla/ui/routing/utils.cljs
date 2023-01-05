(ns sixsq.nuvla.ui.routing.utils
  (:require [clojure.string :as str]
            [reitit.core :as r :refer [match-by-path]]
            [reitit.frontend.easy :as rfe]
            [sixsq.nuvla.ui.config :as config]))

(def route-name->full-route-key
  #_{:clj-kondo/ignore [:unresolved-namespace]}
  (let [route-names (r/route-names sixsq.nuvla.ui.routing.routes/router)]
    (zipmap
      (map (comp keyword name) route-names)
      route-names)))


(defn- full-name->href
  "Return relative url for given full route name. Url can be used in HTML links."
  ([k]
   (full-name->href k nil nil))
  ([k params]
   (full-name->href k params nil))
  ([k params query]
   (rfe/href k params query)))


(defn name->href
  "Return relative url for given route, unqualified key is enough.
   Url can be used in HTML links."
  ([k]
   (full-name->href (k route-name->full-route-key) nil nil))
  ([k params]
   (full-name->href (k route-name->full-route-key) params nil))
  ([k params query]
   (rfe/href (k route-name->full-route-key) params query)))


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
;; routes is a map from unqualified key to qualified route key
  route-name->full-route-key
         ;; => {:apps :sixsq.nuvla.ui.routing.routes/apps,
         ;;     :deployment-sets-sub-age :sixsq.nuvla.ui.routing.routes/deployment-sets-sub-age,
         ;;     :home :sixsq.nuvla.ui.routing.routes/home,
         ;;     :api :sixsq.nuvla.ui.routing.routes/api,
         ;;     :sign-in :sixsq.nuvla.ui.routing.routes/sign-in,
         ;;     :credentials :sixsq.nuvla.ui.routing.routes/credentials,
         ;;     :home-slash :sixsq.nuvla.ui.routing.routes/home-slash,
         ;;     :sign-up :sixsq.nuvla.ui.routing.routes/sign-up,
         ;;     :set-password :sixsq.nuvla.ui.routing.routes/set-password,
         ;;     :api-sub-page :sixsq.nuvla.ui.routing.routes/api-sub-page,
         ;;     :edges-details :sixsq.nuvla.ui.routing.routes/edges-details,
         ;;     :dashboard :sixsq.nuvla.ui.routing.routes/dashboard,
         ;;     :documentation-subpage :sixsq.nuvla.ui.routing.routes/documentation-subpage,
         ;;     :deployment-sets :sixsq.nuvla.ui.routing.routes/deployment-sets,
         ;;     :deployments :sixsq.nuvla.ui.routing.routes/deployments,
         ;;     :credentials-slash :sixsq.nuvla.ui.routing.routes/credentials-slash,
         ;;     :documentation :sixsq.nuvla.ui.routing.routes/documentation,
         ;;     :deployment-details :sixsq.nuvla.ui.routing.routes/deployment-details,
         ;;     :notifications :sixsq.nuvla.ui.routing.routes/notifications,
         ;;     :sign-in-token :sixsq.nuvla.ui.routing.routes/sign-in-token,
         ;;     :root :sixsq.nuvla.ui.routing.routes/root,
         ;;     :home-root :sixsq.nuvla.ui.routing.routes/home-root,
         ;;     :data-details :sixsq.nuvla.ui.routing.routes/data-details,
         ;;     :deployment :sixsq.nuvla.ui.routing.routes/deployment,
         ;;     :reset-password :sixsq.nuvla.ui.routing.routes/reset-password,
         ;;     :clouds :sixsq.nuvla.ui.routing.routes/clouds,
         ;;     :clouds-slashed :sixsq.nuvla.ui.routing.routes/clouds-slashed,
         ;;     :apps-slashed :sixsq.nuvla.ui.routing.routes/apps-slashed,
         ;;     :catch-all :sixsq.nuvla.ui.routing.routes/catch-all,
         ;;     :edges :sixsq.nuvla.ui.routing.routes/edges,
         ;;     :profile :sixsq.nuvla.ui.routing.routes/profile,
         ;;     :clouds-details :sixsq.nuvla.ui.routing.routes/clouds-details,
         ;;     :apps-details :sixsq.nuvla.ui.routing.routes/apps-details,
         ;;     :about :sixsq.nuvla.ui.routing.routes/about,
         ;;     :deployment-slashed :sixsq.nuvla.ui.routing.routes/deployment-slashed,
         ;;     :edges-slashed :sixsq.nuvla.ui.routing.routes/edges-slashed,
         ;;     :data :sixsq.nuvla.ui.routing.routes/data}
  )
