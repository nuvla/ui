(ns sixsq.nuvla.ui.routing.utils
  (:require [clojure.string :as str]
            [reitit.core :refer [match-by-path]]
            [sixsq.nuvla.ui.config :as config]))

(defn url->route-path-params [router url]
  (let [base-path (str config/context "/")
        absolute-url (if (str/starts-with? url base-path)
                       url
                       (str base-path (str/replace url #"^/" "")))
        match (match-by-path router absolute-url)
        name  (get-in match [:data :name])
        path-params (:path-params match)]
    [name path-params]))
