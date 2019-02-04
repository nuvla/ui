(ns sixsq.slipstream.webui.utils.table
  (:require
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.style :as style]))


(defn wrapped-table
  ([table]
   (wrapped-table nil nil table))
  ([title table]
   (wrapped-table nil title table))
  ([icon title table]
   [ui/Segment style/autoscroll-x
    (when title
      [ui/Header {:size "tiny", :style {:padding-top "2ex"}}
       (when icon [ui/Icon {:name icon, :size "tiny"}])
       title])
    table]))


(defn definition-table
  ([rows]
   (definition-table nil nil rows))
  ([title rows]
   (definition-table nil nil rows))
  ([icon title rows]
   (when rows
     (wrapped-table icon
                    title
                    [ui/Table (update-in style/definition [:style :max-width] (constantly "100%"))
                     (vec (concat [ui/TableBody] rows))]))))
