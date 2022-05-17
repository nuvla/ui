(ns sixsq.nuvla.ui.apps-store.utils
  (:require [sixsq.nuvla.ui.utils.general :as general-utils]))


(defn get-query-params
  [full-text-search page elements-per-page]
  {:first   (inc (* (dec page) elements-per-page))
   :last    (* page elements-per-page)
   :orderby "created:desc"
   :filter  (general-utils/join-and
              (general-utils/join-or
                "subtype='component'"
                "subtype='application'"
                "subtype='application_kubernetes'")
              (general-utils/fulltext-query-string full-text-search))})


(defn get-published-modules-query-params
  [full-text-search page elements-per-page]
  {:first   (inc (* (dec page) elements-per-page))
   :last    (* page elements-per-page)
   :orderby "created:desc"
   :filter  (general-utils/join-and
              (general-utils/join-or
                "subtype='component'"
                "subtype='application'"
                "subtype='application_kubernetes'")
              (general-utils/published-query-string)
              (general-utils/fulltext-query-string full-text-search))})


(defn get-my-modules-query-params
  [owner full-text-search page elements-per-page]
  {:first   (inc (* (dec page) elements-per-page))
   :last    (* page elements-per-page)
   :orderby "created:desc"
   :filter  (general-utils/join-and
              (general-utils/join-or
                "subtype='component'"
                "subtype='application'"
                "subtype='application_kubernetes'")
              (general-utils/owner-like-query-string owner)
              (general-utils/fulltext-query-string full-text-search))})


(defn get-modules-by-tag-query-params
  [tag page elements-per-page]
  {:first   (inc (* (dec page) elements-per-page))
   :last    (* page elements-per-page)
   :orderby "created:desc"
   :filter  (general-utils/join-and
              (general-utils/join-or
                "subtype='component'"
                "subtype='application'"
                "subtype='application_kubernetes'")
              (general-utils/by-tag-query-string tag))})


(defn get-query-summary-params
  [full-text-search]
  {:first   0
   :last    0
   :orderby "created:desc"
   :aggregation "terms:subtype"
   :filter  (general-utils/join-and
              (general-utils/join-or
                "subtype='component'"
                "subtype='application'"
                "subtype='application_kubernetes'")
              (general-utils/fulltext-query-string full-text-search))})


