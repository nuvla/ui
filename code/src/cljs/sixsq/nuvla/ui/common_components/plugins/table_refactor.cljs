(ns sixsq.nuvla.ui.common-components.plugins.table-refactor
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch inject-cofx reg-event-db
                                   reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.utils.dnd :as dnd]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
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
               (let [filtered-data (if @!enable-global-filter?
                                     (filterv #(some (partial global-filter-fn @!global-filter)
                                                     (vals (select-keys % @!visible-columns)))
                                              @!data)
                                     @!data)]
                 (if @!enable-sorting?
                   (vec (sort (partial general-utils/multi-key-direction-sort @!sorting) filtered-data))
                   filtered-data))))))

(defn set-!paginated-data
  [{:keys [::!pagination ::!enable-pagination? ::!processed-data] :as control}]
  (assoc control
    ::!paginated-data
    (r/track (fn paginated-data-fn []
               (if @!enable-pagination?
                 (let [{:keys [page-index page-size]} @!pagination
                       n      (count @!processed-data)
                       start  (* page-index page-size)
                       th-end (+ start page-size)
                       end    (if (> th-end n) n th-end)]
                   (subvec @!processed-data (* page-index page-size) end))
                 @!processed-data)))))

(defn !selected?-fn
  [{:keys [::!selected] :as _control} row-id]
  (r/track (fn selected?-fn [] (contains? @!selected row-id))))

(defn !all-row-ids-fn
  [{:keys [::!data ::row-id-fn] :as _control}]
  (r/track (fn all-row-ids-fn [] (set (mapv row-id-fn @!data)))))

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
  (or (nil? filter-str)
      (and (some? s)
           (str/includes? (str/lower-case (str s)) (str/lower-case filter-str)))))

(defn case-sensitive-filter-fn
  [filter-str s]
  (or (nil? filter-str)
      (and (some? s)
           (str/includes? (str s) filter-str))))

(defn CellOverflowTooltip
  [cell-data _row _column]
  (let [str-cell-data (str cell-data)]
    [tt/WithOverflowTooltip {:content str-cell-data :tooltip str-cell-data}]))

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

(defn SortIcon [{:keys [::!enable-sorting?]} direction]
  (let [direction->class {"asc"  " ascending"
                          "desc" " descending"}]
    [uix/LinkIcon {:class (if (and @!enable-sorting? direction) :visible :invisible)
                   :name  (str "sort" (direction->class direction))}]))

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
  (r/with-let [!all-row-ids (!all-row-ids-fn control)
               !selected?   (r/track (fn selected? [] (= @!all-row-ids @!selected)))]
    [:th [ui/Checkbox {:data-testid "checkbox-select-all"
                       :style       {:position       :relative
                                     :vertical-align :middle}
                       :checked     @!selected?
                       :on-click    #(set-selected-fn (if @!selected? #{} @!all-row-ids))}]]))

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
  [{:keys [::!enable-column-customization? ::!enable-sorting? ::!sorting ::set-sorting-fn] :as control} column]
  (let [field-key      (::field-key column)
        sortable       (dnd/useSortable #js {"id" (name field-key)})
        setNodeRef     (.-setNodeRef sortable)
        sort-direction (get-field-sort-direction @!sorting field-key)
        on-click       #(->> sort-direction
                             get-next-sort-direction
                             (calc-new-sorting @!sorting field-key)
                             set-sorting-fn)]
    (js/console.info "Render TableHeaderCell " field-key)
    ;Using html th tag instead of semantic ui TableHeaderCell, because for some reason it's not taking into account ref fn
    [:th (merge {:class    ["show-child-on-hover" "single line"]
                 :style    (cond-> {:cursor      :pointer
                                    :user-select :none}
                                   @!enable-column-customization?
                                   (assoc :transform (dnd/translate-css sortable)))
                 :on-click (when @!enable-sorting? on-click)}
                ;; always adding attributes for consistency on the `role` attribute of the header
                ;; (.-attributes sortable) changes the role from `cell` to `button`
                (js->clj (.-attributes sortable))
                (when @!enable-column-customization?
                  (merge {:ref setNodeRef}
                         (js->clj (.-listeners sortable)))))
     (::header-content column)
     [SortIcon control sort-direction]
     [DeleteColumn control column]]))

(defn columns-by-key-fn [{:keys [::!columns] :as control}] (into {} (map (juxt ::field-key identity) @!columns)))

(defn TableHeader
  [{:keys [::!current-columns ::!default-columns ::!visible-columns] :as control}]
  (r/with-let [!columns-by-key        (r/track columns-by-key-fn control)
               !enable-row-selection? (::!enable-row-selection? control)]
    (js/console.info "Render TableHeader " (str @!visible-columns))
    [ui/TableHeader
     [ui/TableRow
      [dnd/SortableContext
       {:items    (mapv name @!visible-columns)
        :strategy dnd/horizontalListSortingStrategy}
       (doall
         (cond->>
           (for [visible-column @!visible-columns]
             ^{:key (str "header-column-" visible-column)}
             [:f> TableHeaderCell control (get @!columns-by-key visible-column)])
           @!enable-row-selection? (cons
                                     ^{:key "select-all"}
                                     [TableSelectAllCheckbox control])))]]]))

(defn TableCell
  [{:keys [::!enable-column-customization?] :as _control} row column]
  (js/console.info "Render TableCell " (::field-key column))
  (let [sortable   (dnd/useSortable #js {"id"       (name (::field-key column))
                                         "disabled" (not @!enable-column-customization?)})
        setNodeRef (.-setNodeRef sortable)
        Cell       (get column ::field-cell CellOverflowTooltip)]
    ;Using html td tag instead of semantic ui TableCell, because for some reason it's not taking into account ref fn
    [:td {:ref   setNodeRef
          :style {:transform (dnd/translate-css sortable)}}
     [Cell ((::field-key column) row) row column]]))

(defn TableRow
  [{:keys [::row-id-fn ::!visible-columns ::!columns-by-key
           ::!enable-row-selection?] :as control} row]
  (js/console.info "Render TableRow " row)
  (r/with-let [row-id (row-id-fn row)]
    [ui/TableRow
     [dnd/SortableContext
      {:items    (mapv name @!visible-columns)
       :strategy dnd/horizontalListSortingStrategy}
      (doall
        (cond->> (for [visible-column @!visible-columns]
                   (let [column (get @!columns-by-key visible-column)]
                     ^{:key (str "row-" row-id "-column-" visible-column)}
                     [:f> TableCell control row column]))
                 @!enable-row-selection? (cons
                                           ^{:key (str "select-" row-id)}
                                           [TableCellCheckbox control row-id])))]]))

(defn TableBody
  [{:keys [::row-id-fn ::!data ::!paginated-data] :as control}]
  (js/console.info "Render TableBody")
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
                        :opacity          (if @!hoverable 1 0.2)}} [icons/ListIcon]]]))

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
             (for [{:keys [::field-key ::header-content]} @!columns]
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

(defn BasicPagination
  [{:keys [::!pagination ::set-pagination-fn ::!processed-data] :as _control}]
  (let [{:keys [page-index page-size] :as pagination} @!pagination
        total-items   (count @!processed-data)
        page-count    (cond-> (quot total-items page-size)
                              (pos? (rem total-items page-size)) inc)
        goto-page     #(set-pagination-fn (assoc pagination :page-index (max 0 (min (dec page-count) %))))
        set-page-size #(set-pagination-fn (assoc pagination :page-size % :page-index 0))]
    [:div {:style {:display    :flex
                   :margin-top "4px"
                   :gap        "4px"}}
     [:label (str "Total " total-items)]
     [:button {:disabled (zero? page-index), :on-click #(goto-page 0)} "<<"]
     [:button {:disabled (zero? page-index), :on-click #(goto-page (dec page-index))} "<"]
     [:label (str "Page " (inc page-index) " of " page-count)]
     [:button {:disabled (= page-index (dec page-count)), :on-click #(goto-page (inc page-index))} ">"]
     [:button {:disabled (= page-index (dec page-count)), :on-click #(goto-page (dec page-count))} ">>"]
     [ui/Dropdown {:value     page-size
                   :options   (map (fn [n-per-page] {:key     n-per-page
                                                     :value   n-per-page
                                                     :content n-per-page
                                                     :text    (str n-per-page " per page")})
                                   [10 20 30 40])
                   :pointing  true
                   :on-change (ui-callback/value set-page-size)}]]))

(defn- icon
  [icon-name]
  {:content (r/as-element [ui/Icon {:class icon-name}]) :icon true})

(defn NuvlaPagination
  [{:keys [::tr-fn ::!pagination ::set-pagination-fn ::!processed-data] :as control}]
  (let [{:keys [page-index page-size] :as pagination} @!pagination
        total-items   (count @!processed-data)
        page-count    (cond-> (quot total-items page-size)
                              (pos? (rem total-items page-size)) inc)
        goto-page     #(set-pagination-fn (assoc pagination :page-index (max 0 (min (dec page-count) %))))
        set-page-size #(set-pagination-fn (assoc pagination :page-size % :page-index 0))
        per-page-opts (map (fn [n-per-page] {:key     n-per-page
                                             :value   n-per-page
                                             :content n-per-page
                                             :text    (str n-per-page " per page")})
                           [10 20 30 40])]
    [:div {:style {:display         :flex
                   :justify-content :space-between
                   :align-items     :baseline
                   :flex-wrap       :wrap-reverse
                   :margin-top      10}
           :class :uix-pagination}
     [:div {:style {:display :flex}
            :class :uix-pagination-control}
      [:div {:style {:display :flex}}
       [:div {:style {:margin-right "0.5rem"}}
        (str (str/capitalize (tr-fn [:total])) ":")]
       [:div (or total-items 0)]]
      [:div {:style {:color "#C10E12" :margin-right "1rem" :margin-left "1rem"}} "| "]
      [ui/Dropdown {:value     page-size
                    :options   per-page-opts
                    :pointing  true
                    :on-change (ui-callback/value set-page-size)}]]
     [ui/Pagination
      {:size          :tiny
       :class         :uix-pagination-navigation
       :total-pages   page-count
       :first-item    (icon "angle double left")
       :last-item     (icon "angle double right")
       :prev-item     (icon "angle left")
       :next-item     (icon "angle right")
       :ellipsis-item nil
       :active-page   (inc page-index)
       :onPageChange  (ui-callback/callback :activePage #(goto-page (dec %)))}]]))

(defn Table
  [control]
  (r/with-let [{:keys [::!enable-column-customization? ::set-current-columns-fn
                       ::!enable-pagination? ::!visible-columns] :as control}
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
      [ui/Table
       [TableHeader control]
       [TableBody control]]]
     (when @!enable-pagination?
       [NuvlaPagination control])]))

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

           ;; Optional
           ;; Translations
           tr-fn
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
               ;::set-current-columns-fn #(dispatch [::set-current-columns-fn %])
               !enable-sorting?              (or !enable-sorting? (r/atom true))
               set-sorting-fn                (or set-sorting-fn #(reset! !sorting %))
               set-selected-fn               (or set-selected-fn #(reset! !selected %))
               ;!current-columns (r/atom nil)
               ;!current-columns (subscribe [::current-columns])]
               !enable-global-filter?        (or !enable-global-filter? (r/atom true))
               !global-filter                (or !global-filter (r/atom nil))
               global-filter-fn              (or global-filter-fn case-insensitive-filter-fn)
               !enable-pagination?           (or !enable-pagination? (r/atom false))
               set-pagination-fn             (or set-pagination-fn #(reset! !pagination %))
               tr-fn                         (or tr-fn (comp str/capitalize name first))]
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
                ::tr-fn                         tr-fn
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
