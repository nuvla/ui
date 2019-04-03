(ns sixsq.nuvla.ui.acl.views
  (:require
    [clojure.string :as str]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.table :as table]))


(defn checked
  [v]
  (if v [ui/Icon {:name "check"}] "\u0020"))


(defn acl-row
  [[right principals]]
  [ui/TableRow
   [ui/TableCell {:collapsing true, :text-align "left"} right]
   [ui/TableCell {:collapsing true, :text-align "left"} (str/join ", " principals)]])


(defn acl-table
  [acl]
  (when acl
    (let [rows (map acl-row acl)]
      [table/wrapped-table "shield" "permissions"
       [ui/Table style/acl
        [ui/TableHeader
         [ui/TableRow
          [ui/TableHeaderCell {:collapsing true, :text-align "left"} "right"]
          [ui/TableHeaderCell {:collapsing true, :text-align "left"} "principals"]]]
        (vec (concat [ui/TableBody] rows))]])))

