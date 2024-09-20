(ns sixsq.nuvla.ui.components.table-scenes
  (:require [portfolio.reagent :refer-macros [defscene]]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defscene simple-table
  [ui/Table
   [ui/TableHeader
    [ui/TableRow
     [ui/TableHeaderCell {:text-align "right"} "Col 1"]
     [ui/TableHeaderCell {:text-align "right"} "Col 2"]
     [ui/TableHeaderCell {:text-align "right"} "Col 3"]
     [ui/TableHeaderCell {:text-align "right"} "Col 4"]]]
   [ui/TableBody
    [ui/TableRow
     [ui/TableCell {:text-align "right"} "value 1"]
     [ui/TableCell {:text-align "right"} "value 2"]
     [ui/TableCell {:text-align "right"} "value 3"]
     [ui/TableCell {:text-align "right"} "value 4"]]]])
