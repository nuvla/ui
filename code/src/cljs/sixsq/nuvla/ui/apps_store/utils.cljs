(ns sixsq.nuvla.ui.apps-store.utils
  (:require [sixsq.nuvla.ui.utils.general :as general-utils]))


(defn get-query-params
  [full-text-search page elements-per-page]
  {:first  (inc (* (dec page) elements-per-page))
   :last   (* page elements-per-page)
   :filter (general-utils/join-and
             (general-utils/join-or
               "subtype='component'"
               "subtype='application'")
             (general-utils/fulltext-query-string full-text-search))})
