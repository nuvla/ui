(ns sixsq.nuvla.ui.common-components.plugins.table-refactor
  (:require [re-frame.core :refer [dispatch inject-cofx reg-event-db
                                   reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
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

(defn set-current-columns-fn*
  [{:keys [::set-current-columns-fn ::!sorting ::set-sorting-fn] :as _control} columns]
  (let [columns-set (set columns)
        new-sorting (filter #(columns-set (first %)) @!sorting)]
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
                      :on-click    #(set-current-columns-fn* control (remove-field-key visible-columns field-key))
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

(defn TableHeaderCell
  [{:keys [::!sorting ::set-sorting-fn] :as control} column]
  (let [field-key      (::field-key column)
        sort-direction (get-field-sort-direction @!sorting field-key)
        on-click       #(->> sort-direction
                             get-next-sort-direction
                             (calc-new-sorting @!sorting field-key)
                             set-sorting-fn)]
    (js/console.info "Render TableHeaderCell " field-key)
    [ui/TableHeaderCell {:class    ["show-child-on-hover"]
                         :on-click on-click}
     (::header-content column)
     (when sort-direction
       [SortIcon sort-direction])
     [DeleteColumn control column]]))

(defn TableHeader
  [control]
  (js/console.info "Render TableHeader")
  (r/with-let [!visible-columns (!visible-columns-fn control)
               !columns-by-key  (!columns-by-key-fn control)]
    [ui/TableHeader
     [ui/TableRow
      (js/console.info @!columns-by-key)
      (doall
        (for [visible-column @!visible-columns]
          ^{:key (str "header-column-" visible-column)}
          [TableHeaderCell control (get @!columns-by-key visible-column)]))]]))

(defn TableCell
  [_control row column]
  (js/console.info "Render TableCell " (::field-key column))
  [ui/TableCell
   ((::field-key column) row)])

(defn TableRow
  [control row]
  (js/console.info "Render TableRow " row)
  (r/with-let [visible-columns (!visible-columns-fn control)
               !columns-by-key (!columns-by-key-fn control)]
    [ui/TableRow
     (doall
       (for [visible-column @visible-columns]
         (let [column (get @!columns-by-key visible-column)]
           ^{:key (str "row-" (:id row) "-column-" visible-column)}
           [TableCell control row column])))]))

(defn TableBody
  [control]
  (js/console.info "Render TableBody")
  (r/with-let [!sorted-data (!sorted-data-fn control)]
    [ui/TableBody
     (doall
       (for [data-row @!sorted-data]
         ^{:key (str "row-" (:id data-row))}
         [TableRow control data-row]))]))

(defn ColumnsSelectorButton
  [open-fn]
  (r/with-let [!hoverable (r/atom false)]
    [:div {:on-mouse-enter #(reset! !hoverable true)
           :on-mouse-leave #(reset! !hoverable false)
           :style          {:min-height "1.5em"}
           :on-click       open-fn}
     [:span {:title "Columns selector"
             :style {:float            :right
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
  [control]
  [:div
   [ColumnsSelectorModal control]
   [ui/Table {:attached true}
    [TableHeader control]
    [TableBody control]]])

(reg-event-db
  ::set-current-columns-fn
  (fn [db [_ v]]
    (assoc db ::current-columns v)))

(reg-sub
  ::current-columns
  :-> ::current-columns)

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
               control          {::!columns               (r/atom columns)
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
                                 ::!sorting               !sorting}]
    [:div
     [ui/Button {:on-click #(swap! reset-atom inc)} "Reset"]
     [ui/Button {:on-click #(reset! (::!data control) [{:id 1, :name "hello"} {:id 5}])} "Add row"]
     [Table control]]))

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
