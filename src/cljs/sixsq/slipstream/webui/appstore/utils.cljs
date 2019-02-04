(ns sixsq.slipstream.webui.appstore.utils)


(defn get-query-params
  [full-text-search page elements-per-page]
  (cond-> {:$first (inc (* (dec page) elements-per-page))
           :$last  (* page elements-per-page)}
          (not-empty full-text-search) (assoc :$filter (str "description=='" full-text-search "*'"))))
