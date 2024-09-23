(ns sixsq.nuvla.ui.common-components.plugins.tanstack-table
  (:require [reagent.core :as r]
            ["@tanstack/react-table" :as rt]
            ["react" :as react :default useMemo]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))



(defn mylink
  [pg-id]
  [:span pg-id])

;; Example of Clojurescript / Javascript interop.
;; Compare to equivalent JSX implementation:
;; https://github.com/karimarttila/js-node-ts-react/blob/main/frontend/src/routes/product_groups.tsx#L36

(defn TableCell
  [cell]
  (js/console.info "Render TableCell " (.-id cell))
  [ui/TableCell
   (rt/flexRender (.. cell -column -columnDef -cell) (.getContext cell))])

(defn TableCellRow
  [row]
  (js/console.info "Render TableCellRow" (.-id row))
  [ui/TableRow
   (for [^js cell (.getVisibleCells row)]
     ^{:key (.-id cell)}
     [TableCell cell])])

(defn SearchInput
  [table]
  (js/console.info "Render SearchInput")
  [ui/Input {:placeholder "search..."
             :on-change   (ui-callback/input-callback #(.setGlobalFilter table %))}])

(defn ColumnsSelector
  [table]
  (r/with-let [!hoverable (r/atom false)]
    [:div {:on-mouse-enter #(reset! !hoverable true)
           :on-mouse-leave #(reset! !hoverable false)
           :style          {:min-height "1.5em"}}
     [:span {:title "Columns selector"
             :style {:float            :right
                     :border           2
                     :cursor           :pointer
                     :background-color "rgb(249, 250, 251)"
                     :border-width     "1px 1px 0px"
                     :border-color     "rgba(34,36,38,.1)"
                     :border-style     "solid"
                     :opacity          (if @!hoverable 1 0.2)}} [icons/ListIcon]]]))

(def columnHelper (rt/createColumnHelper))
(def columns #js [(.accessor columnHelper "pgId" #js {:header        "Id"
                                                      :cell          (fn [info] (reagent.core/as-element [mylink (.getValue info)]))
                                                      :enableSorting false})
                  (.accessor columnHelper "name" #js {:header "Name"
                                                      :cell   (fn [info] (.getValue info))})])

(defn TableHeaderCell
  [{:keys [key sortable? placeholder? sorted-direction next-sort-order toggle-sorting-handler] :as opts} content]
  (js/console.info "Render TableHeaderCell " key opts content)
  [ui/TableHeaderCell {:key      key
                       :title    (when sortable?
                                   (case next-sort-order
                                     "asc" "Sort ascending"
                                     "desc" "Sort descending"
                                     "Clear sort"))
                       :style    (cond-> {}
                                         sortable? (assoc :cursor :pointer))
                       :on-click toggle-sorting-handler}
   (if placeholder? nil content)
   (case sorted-direction
     "asc" [icons/CaretUpIcon]
     "desc" [icons/CaretDownIcon]
     nil)])

(defn TableIntern
  [table]
  (let [^js headerGroups (.getHeaderGroups table)]
    [ui/Table {:attached true}
    [ui/TableHeader
     (for [^js headerGroup headerGroups]
       [ui/TableRow {:key (.-id headerGroup)}
        (for [^js header (.-headers headerGroup)]
          (let [^js column (.-column header)]
            [TableHeaderCell {:key                    (.-id header)
                              :sortable?              (.getCanSort column)
                              :placeholder?           (.-isPlaceholder header)
                              :sorted-direction       (.getIsSorted column)
                              :next-sort-order        (.getNextSortingOrder column)
                              :toggle-sorting-handler (.getToggleSortingHandler column)}
             (rt/flexRender (.. column -columnDef -header) (.getContext header))]
            #_[ui/TableHeaderCell {:key      (.-id header)

                                   :title    (if sortable?
                                               (case (.getNextSortingOrder column)
                                                 "asc" "Sort ascending"
                                                 "desc" "Sort descending"
                                                 "Clear sort")
                                               nil)
                                   :style    (cond-> {}
                                                     sortable? (assoc :cursor :pointer))
                                   :on-click (.getToggleSortingHandler column)}
               (if (.-isPlaceholder header)
                 nil
                 (rt/flexRender (.. column -columnDef -header) (.getContext header)))
               (case (.getIsSorted column)
                 "asc" [icons/CaretUpIcon]
                 "desc" [icons/CaretDownIcon]
                 nil)]))])]
    [ui/TableBody
     (for [^js row (.-rows (.getRowModel table))]
       ^{:key (.-id row)}
       [TableCellRow row])]]))

(defn product-groups-react-table
  [data]
  (let [table            (rt/useReactTable
                           #js {:columns             columns
                                :data                data
                                :getCoreRowModel     (rt/getCoreRowModel)
                                :getFilteredRowModel (rt/getFilteredRowModel)
                                :getSortedRowModel   (rt/getSortedRowModel)
                                :globalFilterFn      "equalsString"
                                ;:debugTable true,
                                ;:debugHeaders true,
                                ;:debugColumns true,
                                :initialState        #js {:globalFilter ""}})]
    (js/console.info "Render product-groups-react-table" (.getState table))
    [:div
     [SearchInput table]
     [ColumnsSelector table]
     [TableIntern table]]))


(defn Table []
  [:f> product-groups-react-table (clj->js [{:pgId  "a"
                                             "name" "A"}
                                            {:pgId  "b"
                                             "name" "B"}])])