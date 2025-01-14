(ns sixsq.nuvla.ui.common-components.plugins.table-refactor
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch inject-cofx reg-event-db
                                   reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.pages.data.utils :as data-utils]
            [sixsq.nuvla.ui.utils.dnd :as dnd]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.pagination-refactor :as pagination]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.tooltip :as tt]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(defn set-!visible-columns
  [{:keys [::!current-columns ::!default-columns] :as control}]
  (assoc control ::!visible-columns
                 (r/track (fn visible-columns-fn []
                            (or @!current-columns @!default-columns)))))

(defn set-!columns-by-key
  [{:keys [::!columns] :as control}]
  (assoc control
    ::!columns-by-key
    (r/track (fn columns-by-key-fn []
               (into {} (map (juxt ::field-key identity) @!columns))))))

(defn set-!processed-data
  [{:keys [::!enable-global-filter? ::!global-filter
           ::global-filter-fn ::!visible-columns
           ::!enable-sorting? ::!sorting
           ::!data] :as control}]
  (assoc control
    ::!processed-data
    (r/track (fn processed-data []
               (when-not (vector? @!data)
                 (throw (ex-info "Table data must be a vector" {})))
               (let [filtered-data (if @!enable-global-filter?
                                     (filterv #(or (nil? @!global-filter)
                                                   (some (partial global-filter-fn @!global-filter)
                                                         (vals (select-keys % @!visible-columns))))
                                              @!data)
                                     @!data)]
                 (if @!enable-sorting?
                   (vec (sort (partial general-utils/multi-key-direction-sort @!sorting) filtered-data))
                   filtered-data))))))

(defn set-!paginated-data
  [{:keys [::!pagination ::!enable-pagination? ::!processed-data] :as control}]
  (assoc control
    ::!paginated-data
    (pagination/!paginated-data-fn {:!pagination         !pagination
                                    :!enable-pagination? !enable-pagination?
                                    :!data               !processed-data})))

(defn !selected?-fn
  [{:keys [::!selected] :as _control} row-id]
  (r/track (fn selected?-fn [] (contains? @!selected row-id))))

(defn !all-row-ids-fn
  [{:keys [::!processed-data ::row-id-fn] :as _control}]
  (r/track (fn all-row-ids-fn [] (set (mapv row-id-fn @!processed-data)))))

(defn !current-page-row-ids-fn
  [{:keys [::!paginated-data ::row-id-fn] :as _control}]
  (r/track (fn all-row-ids-fn [] (set (mapv row-id-fn @!paginated-data)))))

(defn set-current-columns-fn*
  [{:keys [::set-current-columns-fn ::!sorting ::set-sorting-fn] :as _control} columns]
  (let [columns-set (set columns)
        new-sorting (filterv #(columns-set (first %)) @!sorting)]
    (set-sorting-fn new-sorting))
  (set-current-columns-fn columns))

(defn remove-field-key
  [current-columns field-key]
  (vec (remove (fn [fk] (= fk field-key)) current-columns)))

(defn case-insensitive-filter-fn
  [filter-str s]
  (and (some? s) (str/includes? (str/lower-case (str s)) (str/lower-case filter-str))))

(defn case-sensitive-filter-fn
  [filter-str s]
  (and (some? s) (str/includes? (str s) filter-str)))

(defn CellOverflowTooltip
  [cell-data _row _column as]
  (let [str-cell-data (str cell-data)]
    [tt/WithOverflowTooltip (cond-> {:content str-cell-data :tooltip str-cell-data}
                                    as (assoc :as as))]))

(defn CellOverflowTooltipAs
  [as]
  (fn [cell-data row column]
    [CellOverflowTooltip cell-data row column as]))

(defn CellTimeAgo
  [cell-data _row _column]
  [uix/TimeAgo cell-data])

(defn CellBytes
  [cell-data _row _column]
  (data-utils/format-bytes cell-data))

(defn DeleteColumn
  [{:keys [::!enable-column-customization? ::!visible-columns] :as control}
   {:keys [::field-key ::no-delete] :as _column}]
  (when (and @!enable-column-customization? (not no-delete) (> (count @!visible-columns) 1))
    [:span {:style {:margin-left "0.8rem"}}
     [uix/LinkIcon {:data-testid "DeleteColumn"
                    :aria-label  "Delete Column"
                    :color       "red"
                    :name        "remove circle"
                    :on-click    (fn [event]
                                   (set-current-columns-fn* control (remove-field-key @!visible-columns field-key))
                                   (.stopPropagation event))
                    :class       [:toggle-invisible-on-parent-hover]}]]))

(defn SortIcon [{:keys [::!enable-sorting?]}
                {:keys [::no-sort?] :as _column}
                direction]
  (when (and @!enable-sorting? (not no-sort?))
    (let [dir-class (case direction
                      "asc" "ascending"
                      "desc" "descending"
                      nil)]
      [uix/LinkIcon {:class [(when dir-class :black)]
                     :name  (cond-> "sort"
                                    dir-class (str " " dir-class))}])))

(defn- calc-new-sorting [sorting sort-key sort-direction]
  (if (some? sort-direction)
    (let [index (->> sorting
                     (map-indexed vector)
                     (some #(when (= sort-key (first (second %))) %))
                     first)]
      (if (some? index)
        (update sorting index (constantly [sort-key sort-direction]))
        (conj sorting [sort-key sort-direction])))
    (vec (remove #(= sort-key (first %)) sorting))))

(defn- get-field-sort-direction
  [sorting field-key]
  (->> sorting (some #(when (= field-key (first %)) %)) second))

(defn- get-next-sort-direction
  [sort-direction]
  (case sort-direction
    nil "asc"
    "asc" "desc"
    "desc" nil))

(defn TableSelectAllCheckbox
  [{:keys [::!selected ::set-selected-fn] :as control}]
  (r/with-let [!current-page-row-ids (!current-page-row-ids-fn control)
               !selected?            (r/track (fn selected? []
                                                (and (seq @!selected)
                                                     (every? @!selected @!current-page-row-ids))))]
    [:th {:class "collapsing"}
     [ui/Checkbox {:data-testid "checkbox-select-all"
                   :style       {:position       :relative
                                 :vertical-align :middle}
                   :checked     @!selected?
                   :on-click    #(set-selected-fn (if @!selected?
                                                    (set/difference (set @!selected) (set @!current-page-row-ids))
                                                    (set/union (set @!selected) (set @!current-page-row-ids))))}]]))

(defn TableCellCheckbox
  [{:keys [::!selected ::set-selected-fn] :as control} row-id]
  (r/with-let [!selected? (!selected?-fn control row-id)]
    [:td
     [ui/Checkbox {:data-testid (str "checkbox-select-" row-id)
                   :checked     @!selected?
                   :on-click    #(set-selected-fn ((if @!selected? disj conj) @!selected row-id))
                   :style       {:position       :relative
                                 :vertical-align :middle}}]]))

(defn TableHeaderCell
  [{:keys [::!enable-column-customization? ::!enable-sorting? ::!sorting ::set-sorting-fn ::!sticky-headers?] :as control}
   {:keys [::no-sort? ::field-key ::collapsing ::div-class] :as column}]
  (let [sortable       (dnd/useSortable #js {"id" (name field-key)})
        setNodeRef     (.-setNodeRef sortable)
        sort-direction (get-field-sort-direction @!sorting field-key)
        on-click       #(->> sort-direction
                             get-next-sort-direction
                             (calc-new-sorting @!sorting field-key)
                             set-sorting-fn)]
    ;Using html th tag instead of semantic ui TableHeaderCell, because for some reason it's not taking into account ref fn
    [:th (merge {:class    (cond-> ["show-child-on-hover" "single line"]
                                   collapsing (conj "collapsing"))
                 :style    (cond-> {:user-select :none}
                                   (and @!enable-sorting? (not no-sort?))
                                   (assoc :cursor :pointer)
                                   @!enable-column-customization?
                                   (assoc :transform (dnd/translate-css sortable))
                                   @!sticky-headers?
                                   (merge {:position :sticky
                                           :top      0
                                           :z-index  10}))
                 :on-click (when (and @!enable-sorting? (not no-sort?)) on-click)}
                ;; always adding attributes for consistency on the `role` attribute of the header
                ;; (.-attributes sortable) changes the role from `cell` to `button`
                (js->clj (.-attributes sortable))
                (when @!enable-column-customization?
                  (merge {:ref setNodeRef}
                         (js->clj (.-listeners sortable)))))
     [:div {:class div-class}
      [:span {:data-testid "column-header-text"} (::header-content column)]
      [SortIcon control column sort-direction]
      [DeleteColumn control column]]]))

(defn TableHeader
  [{:keys [::!current-columns ::!default-columns ::!visible-columns
           ::!columns-by-key] :as control}]
  (r/with-let [!enable-row-selection? (::!enable-row-selection? control)]
    [ui/TableHeader
     [ui/TableRow
      [dnd/SortableContext
       {:items    (mapv name @!visible-columns)
        :strategy dnd/horizontalListSortingStrategy}
       (doall
         (cond->>
           (for [visible-column @!visible-columns]
             (when-let [column (get @!columns-by-key visible-column)]
               ^{:key (str "header-column-" visible-column)}
               [:f> TableHeaderCell control column]))
           @!enable-row-selection? (cons
                                     ^{:key "select-all"}
                                     [TableSelectAllCheckbox control])))]]]))

(defn TableCell
  [{:keys [::!enable-column-customization?] :as _control}
   row {:keys [::field-key ::field-cell] :as column}]
  (let [sortable   (dnd/useSortable #js {"id"       (name field-key)
                                         "disabled" (not @!enable-column-customization?)})
        setNodeRef (.-setNodeRef sortable)
        Cell       (or field-cell CellOverflowTooltip)]
    ;Using html td tag instead of semantic ui TableCell, because for some reason it's not taking into account ref fn
    [:td {:ref   setNodeRef
          :style {:transform (dnd/translate-css sortable)}}
     [Cell (field-key row) row column]]))

(defn TableRow
  [{:keys [::row-id-fn ::!visible-columns ::!columns-by-key
           ::!enable-row-selection? ::on-row-click] :as control} row]
  (r/with-let [row-id (row-id-fn row)]
    [ui/TableRow
     (when on-row-click
       {:on-click #(on-row-click row)
        :style    {:cursor "pointer"}})
     [dnd/SortableContext
      {:items    (mapv name @!visible-columns)
       :strategy dnd/horizontalListSortingStrategy}
      (doall
        (cond->> (for [visible-column @!visible-columns]
                   (when-let [column (get @!columns-by-key visible-column)]
                     ^{:key (str "row-" row-id "-column-" visible-column)}
                     [:f> TableCell control row column]))
                 @!enable-row-selection? (cons
                                           ^{:key (str "select-" row-id)}
                                           [TableCellCheckbox control row-id])))]]))

(defn TableBody
  [{:keys [::row-id-fn ::!data ::!paginated-data] :as control}]
  [ui/TableBody
   (doall
     (for [data-row @!paginated-data]
       ^{:key (str "row-" (row-id-fn data-row))}
       [TableRow control data-row]))])

(defn ColumnsSelectorButton
  [open-fn]
  (r/with-let [!hoverable (r/atom false)]
    [:div {:on-mouse-enter #(reset! !hoverable true)
           :on-mouse-leave #(reset! !hoverable false)
           :style          {:min-height "1.5em"}}
     [:span {:title    "Columns selector"
             :on-click open-fn
             :style    {:float            :right
                        :border           2
                        :cursor           :pointer
                        :background-color "rgb(249, 250, 251)"
                        :border-width     "1px 1px 0px"
                        :border-color     "rgba(34,36,38,.1)"
                        :border-style     "solid"
                        :opacity          (if @!hoverable 1 0.5)}} [icons/ListIcon]]]))

(defn ColumnsSelectorModal
  [{:keys [::!default-columns ::!columns ::!enable-column-customization? ::!visible-columns] :as control}]
  (when @!enable-column-customization?
    (r/with-let [open?                  (r/atom false)
                 !local-current-columns (r/atom nil)
                 open-fn                #(do
                                           (reset! !local-current-columns @!visible-columns)
                                           (reset! open? true))
                 close-fn               #(reset! open? false)
                 tr                     (subscribe [::i18n-subs/tr])]
      (let [set-local-current-columns (set @!local-current-columns)]
        [ui/Modal {:close-icon true
                   :open       @open?
                   :trigger    (r/as-element [ColumnsSelectorButton open-fn])
                   :on-close   close-fn}
         [uix/ModalHeader {:header "Select columns"}]
         [ui/ModalContent {:scrolling true}
          [ui/Form
           (doall
             (for [{:keys [::field-key ::header-content ::no-delete]} @!columns
                   :when (not no-delete)]
               ^{:key (str "checkbox-" field-key)}
               [ui/FormCheckbox {:label     (or header-content field-key)
                                 :on-change (ui-callback/checked
                                              #(swap! !local-current-columns
                                                      (if % conj remove-field-key)
                                                      field-key))
                                 :checked   (contains? set-local-current-columns field-key)}]))]]
         [ui/ModalActions
          (when !default-columns
            [uix/Button
             {:text     "Select default columns"
              :on-click #(reset! !local-current-columns @!default-columns)}])
          [uix/Button
           {:text     (@tr [:cancel])
            :on-click close-fn}]
          [uix/Button
           {:text     (@tr [:update])
            :primary  true
            :on-click (fn []
                        (set-current-columns-fn* control @!local-current-columns)
                        (close-fn))}]]]))))

(defn Pagination
  [{:keys [::!global-filter ::!pagination ::set-pagination-fn ::!page-sizes] :as _control}]
  ;; whenever the global filter changes, reset the pagination index to point to the first page
  (when !global-filter
    (add-watch !global-filter :watcher
               (fn [_key _ref old-value new-value]
                 (when-not (= old-value new-value)
                   (set-pagination-fn (assoc @!pagination :page-index 0))))))
  (fn [{:keys [::tr-fn ::!enable-pagination? ::!pagination ::set-pagination-fn ::!processed-data] :as _control}]
    [pagination/PaginationController {:total-items         (count @!processed-data)
                                      :!enable-pagination? !enable-pagination?
                                      :!pagination         !pagination
                                      :set-pagination-fn   set-pagination-fn
                                      :!page-sizes         !page-sizes
                                      :tr-fn               tr-fn}]))

(defn Table
  [control]
  (r/with-let [{:keys [::!enable-column-customization? ::set-current-columns-fn
                       ::!enable-pagination? ::!processed-data ::!pagination ::set-pagination-fn
                       ::!visible-columns ::!max-height ::on-row-click ::tr-fn] :as control}
               (->> control
                    set-!visible-columns
                    set-!columns-by-key
                    set-!processed-data
                    set-!paginated-data)
               on-drag-end-fn (fn [e]
                                (let [active    (.-active e)
                                      over      (.-over e)
                                      active-id (keyword (.-id active))
                                      over-id   (keyword (.-id over))
                                      get-index #(-> % .-data .-current .-sortable .-index)]
                                  (when (and active over (not= active-id over-id))
                                    (-> @!visible-columns
                                        (assoc (get-index active) over-id)
                                        (assoc (get-index over) active-id)
                                        set-current-columns-fn))))]
    [:div
     [ColumnsSelectorModal control]
     [dnd/DndContext {:collisionDetection dnd/closestCenter
                      :modifiers          [dnd/restrictToHorizontalAxis]
                      :onDragEnd          on-drag-end-fn
                      :sensors            (dnd/pointerSensor)}
      [:div.table-wrapper
       {:style (cond-> {}
                       @!max-height (assoc :max-height @!max-height))}
       [ui/Table (cond-> {:style {:border :unset}}
                         on-row-click (assoc :class :selectable))
        [TableHeader control]
        [TableBody control]]]]
     (when @!enable-pagination?
       [Pagination control])]))

(reg-event-db
  ::set-current-columns-fn
  (fn [db [_ v]]
    (assoc db ::current-columns v)))

(reg-sub
  ::current-columns
  :-> ::current-columns)

(defn TableController
  [{:keys [
           ;; Definition of columns to display
           !columns

           ;; Data To be displayed
           !data

           ;; Default columns to display when current-columns is not defined yet.
           ;; Allows to reset current-columns to something defined by the developer.
           !default-columns

           ;; Optional
           ;; Give a function that allow to retrieve the row id. By default it's :id
           row-id-fn

           ;; Optional (enabled by default)
           ;; Make it possible for the user to select which columns are visible and to rearrange their order
           !enable-column-customization?

           ;; Optional
           ;; To control which columns are currently displayed override following control attributes
           ;; format: vector of column_ids
           !current-columns
           set-current-columns-fn

           ;; Optional
           ;; To control which columns are sorted override following control attributes
           ;; format: vector of (vector of column_id direction)
           !enable-sorting?
           !sorting
           set-sorting-fn

           ;; Optional
           ;; make table row selectable
           !enable-row-selection?
           !selected
           set-selected-fn

           ;; Optional (enabled by default)
           ;; Global filter on all visible columns
           !enable-global-filter?
           !global-filter
           global-filter-fn

           ;; Optional (disabled by default)
           ;; Pagination
           !enable-pagination?
           !pagination
           set-pagination-fn
           !page-sizes

           ;; Optional
           ;; If present, makes table rows clickable. Rows will be highlighted on hover
           on-row-click

           ;; Optional
           ;; Translations
           tr-fn

           ;; Optional
           ;; Css options
           !sticky-headers?
           !max-height
           ]}]
  (r/with-let [row-id-fn                     (or row-id-fn :id)
               !sorting                      (or !sorting (r/atom []))
               !selected                     (or !selected (r/atom #{}))
               !enable-row-selection?        (or !enable-row-selection? (r/atom false))
               !columns                      (or !columns (r/atom []))
               !data                         (or !data (r/atom []))
               !default-columns              (or !default-columns (r/atom [:id]))
               !enable-column-customization? (or !enable-column-customization? (r/atom true))
               !current-columns              (or !current-columns (r/atom nil))
               set-current-columns-fn        (or set-current-columns-fn #(reset! !current-columns %))
               !enable-sorting?              (or !enable-sorting? (r/atom true))
               set-sorting-fn                (or set-sorting-fn #(reset! !sorting %))
               set-selected-fn               (or set-selected-fn #(reset! !selected %))
               !enable-global-filter?        (or !enable-global-filter? (r/atom true))
               !global-filter                (or !global-filter (r/atom nil))
               global-filter-fn              (or global-filter-fn case-insensitive-filter-fn)
               !enable-pagination?           (or !enable-pagination? (r/atom false))
               set-pagination-fn             (or set-pagination-fn #(reset! !pagination %))
               tr-fn                         (or tr-fn (comp str/capitalize name first))
               !sticky-headers?              (or !sticky-headers? (r/atom false))
               !max-height                   (or !max-height (r/atom nil))
               ]
    [:f> Table {::row-id-fn                     row-id-fn
                ::!columns                      !columns
                ::!data                         !data
                ::!default-columns              !default-columns
                ::!enable-column-customization? !enable-column-customization?
                ::set-current-columns-fn        set-current-columns-fn
                ::!current-columns              !current-columns
                ::set-sorting-fn                set-sorting-fn
                ::!enable-sorting?              !enable-sorting?
                ::!sorting                      !sorting
                ::!enable-row-selection?        !enable-row-selection?
                ::!selected                     !selected
                ::set-selected-fn               set-selected-fn
                ::!enable-global-filter?        !enable-global-filter?
                ::!global-filter                !global-filter
                ::global-filter-fn              global-filter-fn
                ::!enable-pagination?           !enable-pagination?
                ; ::!manual-pagination
                ::!pagination                   !pagination
                ::set-pagination-fn             set-pagination-fn
                ::!page-sizes                   !page-sizes
                ::tr-fn                         tr-fn
                ::!sticky-headers?              !sticky-headers?
                ::!max-height                   !max-height
                ::on-row-click                  on-row-click
                }]))

;; table
;; rows
;; columns
;; add remove columns
;; columns selector modal
;; sortable
;; dynamic build columns list
;; filtering
;; drag columns
;; selectable rows
;; pagination
