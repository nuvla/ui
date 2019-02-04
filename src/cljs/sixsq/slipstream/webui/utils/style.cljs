(ns sixsq.slipstream.webui.utils.style)

;;
;; common styles to be applied to Segments
;;


(def ^{:doc "Options for a Segment with no padding or special visual style.
             Can be used in place of a raw [:div]."}
basic
  {:basic  true
   :padded false
   :style  {:padding-top    "inherit"
            :padding-bottom "inherit"
            :padding-left   0
            :padding-right  0}})


(def ^{:doc "Options for a Segment with horizontal scroll."}
autoscroll-x
  (assoc-in basic [:style :overflow-x] "auto"))


(def ^{:doc "Options for a Segment with evenly spaced content."}
evenly
  (merge basic
         {:style {:display         "flex"
                  :justify-content "space-evenly"}}))


;;
;; common styles for Tables
;;

(def ^{:doc "Common styles for single-line tables."}
single-line
  {:compact     "very"
   :single-line true
   :padded      false
   :style       {:max-width "100%"}})


(def ^{:doc "Common styles for single-line, selectable tables."}
selectable
  (merge single-line {:selectable  true
                      :unstackable true}))


(def ^{:doc "Style for very compact, single-line definition table."}
definition
  (merge single-line
         {:definition true}))


(def ^{:doc "Style for very compact, single-line ACL table."}
acl
  (merge single-line
         {:celled      true
          :text-align  "center"
          :unstackable true
          :style       {:max-width "100%"}}))
