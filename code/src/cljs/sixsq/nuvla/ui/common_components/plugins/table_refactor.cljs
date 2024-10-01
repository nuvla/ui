(ns sixsq.nuvla.ui.common-components.plugins.table-refactor
  (:require [clojure.set :as set]
            [clojure.string :as str]
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
  [{:keys [::!sorting] :as _control} !data]
  (r/track #(sort (partial general-utils/multi-key-direction-sort @!sorting)
                  @!data)))

(defn default-filter-fn
  [filter-str s]
  (or (nil? filter-str)
      (and (some? s)
           (str/includes? (str/lower-case (str s)) (str/lower-case filter-str)))))

(defn !filtered-data-fn
  [{:keys [::!global-filter ::global-filter-fn] :as _control} !data !visible-columns]
  (r/track (fn filter-data []
             (let [filter-fn (or global-filter-fn default-filter-fn)]
               (doall (filter #(some (partial filter-fn @!global-filter)
                                     (vals (select-keys % @!visible-columns)))
                              @!data))))))

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
    [uix/LinkIcon {:class (if direction :visible :invisible)
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
  [{:keys [::!selected] :as control}]
  (r/with-let [!all-row-ids (!all-row-ids-fn control)
               !selected?   (r/track #(= @!all-row-ids @!selected))]
    [:th [ui/Checkbox {:style    {:position       :relative
                                  :vertical-align :middle}
                       :checked  @!selected?
                       :on-click #(reset! !selected (if @!selected? #{} @!all-row-ids))}]]))

(defn TableCellCheckbox
  [{:keys [::!selected] :as control} row-id]
  (r/with-let [!selected? (!selected?-fn control row-id)]
    [:td [ui/Checkbox {:checked  @!selected?
                       :on-click #(swap! !selected (if @!selected? disj conj) row-id)
                       :style    {:position       :relative
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
                 :style    {:cursor      :pointer
                            :user-select :none
                            :transform   (dnd/translate-css sortable)}
                 :on-click on-click}
                (js->clj (.-attributes sortable))
                (js->clj (.-listeners sortable)))
     (::header-content column)
     [SortIcon sort-direction]
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
  [{:keys [::row-id-fn ::!data] :as control}]
  (js/console.info "Render TableBody")
  (r/with-let [!visible-columns (!visible-columns-fn control)
               !filtered-data   (!filtered-data-fn control !data !visible-columns)
               !sorted-data     (!sorted-data-fn control !filtered-data)]
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

(defn SearchInput
  [!global-filter]
  (js/console.info "Render SearchInput")
  [ui/Input {:style       {:padding "4px"}
             :class       :global-filter
             :placeholder "search..."
             :on-change   (ui-callback/input-callback #(reset! !global-filter %))}])

(defn case-sensitive-filter-fn
  [filter-str s]
  (or (nil? filter-str)
      (and (some? s)
           (str/includes? (str s) filter-str))))

(defn TableControllerReal
  [reset-atom]
  (js/console.info "Render TableControllerReal")
  (r/with-let [columns          [{::field-key      :Id
                                  ::header-content "Id"
                                  ::no-delete      true}
                                 {::field-key      :Size
                                  ::header-content "Size"}
                                 {::field-key      :Created
                                  ::header-content "Created"}]
               !current-columns (r/atom nil)
               ;!current-columns (subscribe [::current-columns])
               !sorting         (r/atom [])
               !selected        (r/atom #{})
               !selectable?     (r/atom false)
               !global-filter   (r/atom nil)
               control          {::row-id-fn              :Id
                                 ::!columns               (r/atom columns)
                                 ::!data                  (r/atom [{:RepoDigests
                                                                    ["nuvladev/nuvlaedge@sha256:56f8fe1fdf35d50577ab135dcbf78cfb877ccdc41948ec9352d526614b7462f2"],
                                                                    :Labels
                                                                    {:git.build.time                   "$(date --utc +%FT%T.%3NZ)",
                                                                     :org.opencontainers.image.vendor  "SixSq SA",
                                                                     :org.opencontainers.image.created "$(date --utc +%FT%T.%3NZ)",
                                                                     :git.run.id                       "10816164086",
                                                                     :org.opencontainers.image.url
                                                                     "https://github.com/nuvlaedge/nuvlaedge",
                                                                     :org.opencontainers.image.authors "support@sixsq.com",
                                                                     :git.branch                       "coe-resources",
                                                                     :git.commit.id                    "24bb0659461896b22a4a8b675a30b011bbf4efe4",
                                                                     :org.opencontainers.image.title   "NuvlaEdge",
                                                                     :org.opencontainers.image.description
                                                                     "Common image for NuvlaEdge software components",
                                                                     :git.run.number                   "839"},
                                                                    :SharedSize -1,
                                                                    :Size       192121737,
                                                                    :Id
                                                                    "sha256:b4a4526cfd461c7bd1ad3b3e864b9a3f671890b2c42ea0cbad55dd999ab6ae9c",
                                                                    :Containers -1,
                                                                    :ParentId   "",
                                                                    :Created    1726074087,
                                                                    :RepoTags   ["nuvladev/nuvlaedge:coe-resources"]}
                                                                   {:RepoDigests
                                                                    ["nuvladev/nuvlaedge@sha256:33426aed6440dccdd36e75b5a46073d0888295496c17e2afcdddb51539ea7b99"],
                                                                    :Labels
                                                                    {:git.build.time                   "$(date --utc +%FT%T.%3NZ)",
                                                                     :org.opencontainers.image.vendor  "SixSq SA",
                                                                     :org.opencontainers.image.created "$(date --utc +%FT%T.%3NZ)",
                                                                     :git.run.id                       "10746857192",
                                                                     :org.opencontainers.image.url
                                                                     "https://github.com/nuvlaedge/nuvlaedge",
                                                                     :org.opencontainers.image.authors "support@sixsq.com",
                                                                     :git.branch                       "coe-resources",
                                                                     :git.commit.id                    "46a2ba7903ee7a1faa54b2aba9e283242c1bee5a",
                                                                     :org.opencontainers.image.title   "NuvlaEdge",
                                                                     :org.opencontainers.image.description
                                                                     "Common image for NuvlaEdge software components",
                                                                     :git.run.number                   "836"},
                                                                    :SharedSize -1,
                                                                    :Size       191903136,
                                                                    :Id
                                                                    "sha256:bd1e8ef984a199d31d3fc478431165ca0236176ad62fab2a4e68a2c5b8e12fbd",
                                                                    :Containers -1,
                                                                    :ParentId   "",
                                                                    :Created    1725667915,
                                                                    :RepoTags   []}
                                                                   {:RepoDigests
                                                                    ["nuvladev/nuvlaedge@sha256:2d92c970a5d8ce3e2fae5b88bb4d2a2cf701b0cdd4aa41e883aea79cd3e61859"],
                                                                    :Labels
                                                                    {:git.build.time                   "$(date --utc +%FT%T.%3NZ)",
                                                                     :org.opencontainers.image.vendor  "SixSq SA",
                                                                     :org.opencontainers.image.created "$(date --utc +%FT%T.%3NZ)",
                                                                     :git.run.id                       "10746651311",
                                                                     :org.opencontainers.image.url
                                                                     "https://github.com/nuvlaedge/nuvlaedge",
                                                                     :org.opencontainers.image.authors "support@sixsq.com",
                                                                     :git.branch                       "coe-resources",
                                                                     :git.commit.id                    "109a34446f5edf006fb1400ca266490492bf7363",
                                                                     :org.opencontainers.image.title   "NuvlaEdge",
                                                                     :org.opencontainers.image.description
                                                                     "Common image for NuvlaEdge software components",
                                                                     :git.run.number                   "835"},
                                                                    :SharedSize -1,
                                                                    :Size       191903117,
                                                                    :Id
                                                                    "sha256:0ec61197db8b0989753da0c499be52b48c5d746a7d675ae358e157912d7d47bb",
                                                                    :Containers -1,
                                                                    :ParentId   "",
                                                                    :Created    1725666894,
                                                                    :RepoTags   []}]
                                                                  )
                                 ::!default-columns       (r/atom [:Id :Size :Created])
                                 ::set-current-columns-fn #(reset! !current-columns %)
                                 ;::set-current-columns-fn #(dispatch [::set-current-columns-fn %])
                                 ::!current-columns       !current-columns
                                 ::set-sorting-fn         #(reset! !sorting %)
                                 ::!sorting               !sorting
                                 ::!selectable?           !selectable?
                                 ::!selected              !selected
                                 ::set-selected-fn        #(reset! !selected %)
                                 ::!global-filter         !global-filter
                                 ::global-filter-fn       case-sensitive-filter-fn
                                 }]
    [:div
     [SearchInput !global-filter]
     [ui/Button {:on-click #(swap! reset-atom inc)} "Reset"]
     [ui/Button {:on-click #(reset! (::!data control) [{:id 1, :name "hello"} {:id 5}])} "Add row"]
     [ui/Button {:on-click #(swap! !selectable? not)} "Selectable?"]
     [:f> Table control]]))

(defn TableController
  []
  (r/with-let [reset-atom (r/atom 0)]
    ^{:key @reset-atom}
    [TableControllerReal reset-atom]))

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
