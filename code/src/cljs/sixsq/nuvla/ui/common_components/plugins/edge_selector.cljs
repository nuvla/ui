(ns sixsq.nuvla.ui.common-components.plugins.edge-selector
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.plugins.pagination-refactor :as pagination]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table-refactor]
            [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.pages.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn !selected?-fn
  [{:keys [::!selected] :as _control} edge-id]
  (r/track (fn selected?-fn [] (contains? @!selected edge-id))))

(defn set-!paginated-edges
  [{:keys [::!pagination ::!enable-pagination? ::!edges] :as control}]
  (assoc control
    ::!paginated-edges
    (pagination/!paginated-data-fn {:!pagination         !pagination
                                    :!enable-pagination? !enable-pagination?
                                    :!data               !edges})))

(defn Application
  [{:keys [::!selected ::set-selected-fn] :as control}
   {:keys [id name subtype description] :as _edge}]
  (r/with-let [!selected? (!selected?-fn control id)]
    [ui/ListItem {:on-click #(set-selected-fn ((if @!selected? disj conj) @!selected id))
                  :style    {:cursor :pointer}}
     [ui/ListIcon {:name (if @!selected?
                           icons/i-check-square-outline
                           icons/i-square-outline)}]
     [ui/ListContent
      [ui/ListHeader (when @!selected? {:as :a})
       [apps-utils/SubtypeIconInfra subtype @!selected?]
       " "
       (or name id)]
      [ui/ListDescription
       (general-utils/truncate description 100)]]]))

(defn Applications
  [control applications]
  [:<>
   (for [{:keys [id] :as child} applications]
     ^{:key id}
     [Application control child])])

(declare Node)

(defn Project
  [control path {:keys [applications] :as content}]
  [ui/ListItem
   [ui/ListIcon {:name icons/i-folder-full}]
   [ui/ListContent
    [ui/ListHeader path]
    [ui/ListList
     [Node control (dissoc content :applications) applications]]]])

(defn Projects
  [control projects]
  [:<>
   (for [[path content] projects]
     ^{:key path}
     [Project control path content])])

(defn Node
  [control projects applications]
  [:<>
   [Projects control (sort-by first projects)]
   [Applications control (sort-by (juxt :name :id) applications)]])

(defn FullTextSearch
  [{:keys [on-change placeholder-suffix tr-fn text] :as opts}]
  [ui/Input
   (-> opts
       (dissoc :on-change :placeholder-suffix :tr-fn :text)
       (assoc :placeholder (str (tr-fn [:search]) placeholder-suffix "...")
              :icon "search"
              :default-value (or text "")
              :on-change (ui-callback/input-callback on-change)))])

(defn CellName
  [_cell-data {:keys [uuid name] :as _nuvlabox} _column]
  (or name uuid))

(defn CellState
  [state _row _column]
  state)

(defn CellCreatedBy
  [created-by _row _column]
  created-by)

(defn CellCOEList
  [coe-list _row _column]
  [:div
   (for [{:keys [coe-type]} coe-list]
     (case coe-type
       edges-utils/coe-type-docker
       ^{:key coe-type} [ui/ListIcon {:name icons/i-docker}]
       edges-utils/coe-type-swarm
       ^{:key coe-type} [ui/Image {:src   "/ui/images/docker-swarm-grey.png"
                                   :style {:width   "1.50em"
                                           :margin  "0 .25rem 0 0"
                                           :display :inline-block}}]
       edges-utils/coe-type-kubernetes
       ^{:key coe-type} [apps-utils/IconK8s false]))])

(defn EdgeSelector
  [control]
  (let [{:keys [::!subtypes ::!edges ::!loading? ::!disabled? ::set-filters-fn ::!selected ::set-selected-fn
                ::!enable-pagination? ::!page-sizes ::!pagination ::set-pagination-fn ::tr-fn] :as control}
        (-> control
            set-!paginated-edges)
        !text-filter        (r/atom nil)
        filters-changed     (fn []
                              (when !pagination
                                (set-pagination-fn (assoc @!pagination :page-index 0)))
                              (when set-filters-fn
                                (set-filters-fn {:text @!text-filter})))
        text-filter-changed (fn [text]
                              (reset! !text-filter text)
                              (filters-changed))
        !sorting            (r/atom nil)
        set-sorting-fn      (fn [sorting]
                              (let [sorting (mapv (fn [[key direction value-fn]]
                                                    [key direction
                                                     (if (= :coe-list key)
                                                       (comp (partial mapv :coe-type) :coe-list)
                                                       value-fn)])
                                                  sorting)]
                                (reset! !sorting sorting)))]
    (filters-changed)
    (fn [{:keys [::!value ::set-value-fn ::!subtypes ::!disabled? ::set-filters-fn ::tr-fn] :as _control}]
      [ui/Segment
       [FullTextSearch {:tr-fn     tr-fn
                        :on-change text-filter-changed}]
       [table-refactor/TableController
        {:!columns               (r/atom [{::table-refactor/field-key      :name
                                           ::table-refactor/header-content "Name"
                                           ::table-refactor/field-cell     CellName}
                                          {::table-refactor/field-key      :state
                                           ::table-refactor/header-content "State"
                                           ::table-refactor/field-cell     CellState}
                                          {::table-refactor/field-key      :created-by
                                           ::table-refactor/header-content "User"
                                           ::table-refactor/field-cell     CellCreatedBy}
                                          {::table-refactor/field-key      :coe-list
                                           ::table-refactor/header-content "COE"
                                           ::table-refactor/field-cell     CellCOEList}])
         :!default-columns       (r/atom [:name :state :created-by :coe-list])
         :!data                  !edges
         :!enable-global-filter? (r/atom false)
         :!enable-sorting?       (r/atom true)
         :!sorting               !sorting
         :set-sorting-fn         set-sorting-fn
         :!enable-row-selection? (r/atom true)
         :!selected              !selected
         :set-selected-fn        set-selected-fn
         :!max-height            (r/atom 500)
         :!enable-pagination?    !enable-pagination?
         :!page-sizes            !page-sizes
         :!pagination            !pagination
         :set-pagination-fn      set-pagination-fn}]])))

(defn EdgeSelectorController
  [{:keys [!edges
           set-filters-fn

           ;; Optional
           ;; whether data loading is in progress
           !loading?

           ;; Optional
           !selected
           set-selected-fn

           ;; Optional (disabled by default)
           ;; Pagination
           !enable-pagination?
           !page-sizes
           !pagination
           set-pagination-fn

           ;; Optional
           ;; whether the picker should be disabled
           !disabled?

           ;; Optional
           ;; Translations
           tr-fn
           ]}]
  (r/with-let [!edges              (or !edges (r/atom []))
               !loading?           (or !loading? (r/atom false))
               !selected           (or !selected (r/atom #{}))
               set-selected-fn     (or set-selected-fn #(reset! !selected %))
               !enable-pagination? (or !enable-pagination? (r/atom false))
               set-pagination-fn   (or set-pagination-fn #(reset! !pagination %))
               !disabled?          (or !disabled? (r/atom false))
               tr-fn               (or tr-fn (comp str/capitalize name first))]
    [EdgeSelector {::!edges              !edges
                   ::!loading?           !loading?
                   ::set-filters-fn      set-filters-fn
                   ::!selected           !selected
                   ::set-selected-fn     set-selected-fn
                   ::!enable-pagination? !enable-pagination?
                   ::!page-sizes         !page-sizes
                   ::!pagination         !pagination
                   ::set-pagination-fn   set-pagination-fn
                   ::!disabled?          !disabled?
                   ::tr-fn               tr-fn}]))
