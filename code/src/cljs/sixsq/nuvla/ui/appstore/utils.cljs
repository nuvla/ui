(ns sixsq.nuvla.ui.appstore.utils)


(defn get-query-params
  [full-text-search page elements-per-page]
  {:first  (inc (* (dec page) elements-per-page))
   :last   (* page elements-per-page)
   :filter (str "type=='COMPONENT'"
                (when (not-empty full-text-search) (str "and fulltext=='" full-text-search "*'")))})
