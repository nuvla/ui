(ns sixsq.nuvla.ui.infra-service.utils
  (:require [clojure.string :as str]))

(defn get-query-params
  [full-text-search page elements-per-page]
  (let [full-text-search (str "fulltext=='" full-text-search "*'")]
    (cond-> {:first (inc (* (dec page) elements-per-page))
             :last  (* page elements-per-page)
             ;:orderby "created:desc"
             }
            (not (str/blank? filter)) (assoc :filter full-text-search))))
