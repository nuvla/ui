(ns sixsq.nuvla.ui.common-components.plugins.table-refactor
  (:require [clojure.set :as set]
            [re-frame.core :refer [dispatch inject-cofx reg-event-db
                                   reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.utils.dnd :as dnd]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(defn !visible-columns-fn
  [{:keys [::!current-columns ::!default-columns] :as _control}]
  (r/track #(or @!current-columns @!default-columns)))

(defn !columns-by-key-fn
  [{:keys [::!columns] :as _control}]
  (r/track #(into {} (map (juxt ::field-key identity) @!columns))))

(defn remove-field-key
  [current-columns field-key]
  (vec (remove (fn [fk] (= fk field-key)) current-columns)))

(defn !sorted-data-fn
  [{:keys [::!sorting ::!data] :as _control}]
  (r/track #(sort (partial general-utils/multi-key-direction-sort @!sorting)
                  @!data)))

(defn !selected?-fn
  [{:keys [::!selected] :as _control} row-id]
  (r/track #(contains? @!selected row-id)))

(defn !all-row-ids-fn
  [{:keys [::!data ::row-id-fn] :as _control}]
  (r/track #(set (mapv row-id-fn @!data))))

(defn set-current-columns-fn*
  [{:keys [::set-current-columns-fn ::!sorting ::set-sorting-fn] :as _control} columns]
  (let [columns-set (set columns)
        new-sorting (filterv #(columns-set (first %)) @!sorting)]
    (set-sorting-fn new-sorting))
  (set-current-columns-fn columns))

(defn DeleteColumn
  [control {:keys [::field-key ::no-delete] :as _column}]
  (let [visible-columns @(!visible-columns-fn control)]
    (when (and (> (count visible-columns) 1) (not no-delete))
      [:span {:style {:margin-left "0.8rem"}}
       [uix/LinkIcon {:data-testid "DeleteColumn"
                      :aria-label  "Delete Column"
                      :color       "red"
                      :name        "remove circle"
                      :on-click    (fn [event]
                                     (set-current-columns-fn* control (remove-field-key visible-columns field-key))
                                     (.stopPropagation event))
                      :class       :toggle-invisible-on-parent-hover}]])))

(defn SortIcon [direction]
  (let [direction->class {"asc"  " ascending"
                          "desc" " descending"}]
    (when direction
      [uix/LinkIcon {:name (str "sort" (direction->class direction))}])))

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
               !selected?   (r/track #(= @!all-row-ids @!selected))]
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
  [{:keys [::!sorting ::set-sorting-fn] :as control} column]
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
    [:th (merge {:ref      setNodeRef
                 :class    ["show-child-on-hover"]
                 :style    {:cursor    :pointer
                            :transform (dnd/translate-css sortable)}
                 :on-click on-click}
                (js->clj (.-attributes sortable))
                (js->clj (.-listeners sortable)))
     (::header-content column)
     (when sort-direction
       [SortIcon sort-direction])
     [DeleteColumn control column]]))

(defn TableHeader
  [control]
  (js/console.info "Render TableHeader")
  (r/with-let [!visible-columns (!visible-columns-fn control)
               !columns-by-key  (!columns-by-key-fn control)
               !selectable?     (::!selectable? control)]
    [ui/TableHeader
     [ui/TableRow
      (js/console.info "TableHeader" @!columns-by-key @!visible-columns)
      [dnd/SortableContext
       {:items    (mapv name @!visible-columns)
        :strategy dnd/horizontalListSortingStrategy}
       (doall
         (cond->>
           (for [visible-column @!visible-columns]
             ^{:key (str "header-column-" visible-column)}
             [:f> TableHeaderCell control (get @!columns-by-key visible-column)])
           @!selectable? (cons
                           ^{:key "select-all"}
                           [TableSelectAllCheckbox control])))]]]))

(defn TableCell
  [_control row column]
  (js/console.info "Render TableCell " (::field-key column))
  (let [sortable   (dnd/useSortable #js {"id" (name (::field-key column))})
        setNodeRef (.-setNodeRef sortable)]
    ;Using html td tag instead of semantic ui TableCell, because for some reason it's not taking into account ref fn
    [:td {:ref   setNodeRef
          :style {:transform (dnd/translate-css sortable)}}
     ((::field-key column) row)]))

(defn TableRow
  [{:keys [::row-id-fn] :as control} row]
  (js/console.info "Render TableRow " row)
  (r/with-let [visible-columns (!visible-columns-fn control)
               !columns-by-key (!columns-by-key-fn control)
               !selectable?    (::!selectable? control)
               row-id          (row-id-fn row)]
    [ui/TableRow
     [dnd/SortableContext
      {:items    (mapv name @(!visible-columns-fn control))
       :strategy dnd/horizontalListSortingStrategy}
      (doall
        (cond->> (for [visible-column @visible-columns]
                   (let [column (get @!columns-by-key visible-column)]
                     ^{:key (str "row-" row-id "-column-" visible-column)}
                     [:f> TableCell control row column]))
                 @!selectable? (cons
                                 ^{:key (str "select-" row-id)}
                                 [TableCellCheckbox control row-id])))]]))

(defn TableBody
  [{:keys [::row-id-fn] :as control}]
  (js/console.info "Render TableBody")
  (r/with-let [!sorted-data (!sorted-data-fn control)]
    [ui/TableBody
     (doall
       (for [data-row @!sorted-data]
         ^{:key (str "row-" (row-id-fn data-row))}
         [TableRow control data-row]))]))

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
  [{:keys [::!default-columns ::!columns] :as control}]
  (r/with-let [open?                  (r/atom false)
               !local-current-columns (r/atom nil)
               open-fn                #(do
                                         (reset! !local-current-columns @(!visible-columns-fn control))
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
                      (close-fn))}]]])))

(defn Table
  [{:keys [::set-current-columns-fn] :as control}]
  (r/with-let [on-drag-end-fn (fn [e]
                                (let [active    (.-active e)
                                      over      (.-over e)
                                      active-id (keyword (.-id active))
                                      over-id   (keyword (.-id over))
                                      get-index #(-> % .-data .-current .-sortable .-index)]
                                  (when (and active over (not= active-id over-id))
                                    (-> @(!visible-columns-fn control)
                                        (assoc (get-index active) over-id)
                                        (assoc (get-index over) active-id)
                                        set-current-columns-fn))))]
    [:div
     [ColumnsSelectorModal control]
     [dnd/DndContext {:collisionDetection dnd/closestCenter
                      :modifiers          [dnd/restrictToHorizontalAxis]
                      :onDragEnd          on-drag-end-fn
                      :sensors            (dnd/pointerSensor)}
      [ui/Table {:attached true}
       [TableHeader control]
       [TableBody control]]]]))

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

           ;; Optional
           ;; To control which columns are currently displayed override following control attributes
           ;; format: vector of column_ids
           !current-columns
           set-current-columns-fn

           ;; Optional
           ;; To control which columns are sorted override following control attributes
           ;; format: vector of (vector of column_id direction)
           !sorting
           set-sorting-fn

           ;; Optional
           ;; make table row selectable
           !selectable?
           !selected
           set-selected-fn
           ]}]
  (r/with-let [row-id-fn              (or row-id-fn :id)
               !sorting               (or !sorting (r/atom []))
               !selected              (or !selected (r/atom #{}))
               !selectable?           (or !selectable? (r/atom false))
               !columns               (or !columns (r/atom []))
               !data                  (or !data (r/atom []))
               !default-columns       (or !default-columns (r/atom [:id]))
               !current-columns       (or !current-columns (r/atom nil))
               set-current-columns-fn (or set-current-columns-fn #(reset! !current-columns %))
               ;::set-current-columns-fn #(dispatch [::set-current-columns-fn %])
               set-sorting-fn         (or set-sorting-fn #(reset! !sorting %))
               set-selected-fn        (or set-selected-fn #(reset! !selected %))
               ;!current-columns (r/atom nil)
               ;!current-columns (subscribe [::current-columns])]
               ]
    [:f> Table {::row-id-fn              row-id-fn
                ::!columns               !columns
                ::!data                  !data
                ::!default-columns       !default-columns
                ::set-current-columns-fn set-current-columns-fn
                ::!current-columns       !current-columns
                ::set-sorting-fn         set-sorting-fn
                ::!sorting               !sorting
                ::!selectable?           !selectable?
                ::!selected              !selected
                ::set-selected-fn        set-selected-fn}]))

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
