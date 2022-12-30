(ns sixsq.nuvla.ui.routing.utils
  (:require [clojure.string :as str]
            [reitit.core :refer [match-by-path]]
            [sixsq.nuvla.ui.config :as config]))

(defn url->route-path-params [router url]
  (let [base-path (str config/base-path "/")
        absolute-url (if (str/starts-with? url base-path)
                       url
                       (str base-path (str/replace url #"^/" "")))
        match (match-by-path router absolute-url)
        name  (get-in match [:data :name])
        path-params (:path-params match)]
    [name path-params]))

(defn decode-query-string [path]
  (some->
   (second (str/split path #"\?"))
   (str/split #"&")
   (->> (map (fn [s] (let [[k v] (str/split s #"=")]
                       [(keyword k) v])))
        (into {}))))

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