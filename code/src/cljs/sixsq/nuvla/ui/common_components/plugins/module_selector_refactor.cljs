(ns sixsq.nuvla.ui.common-components.plugins.module-selector-refactor
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.plugins.nav-tab-refactor :as nav-tab]
            [sixsq.nuvla.ui.common-components.plugins.pagination-refactor :as pagination]
            [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn !selected?-fn
  [{:keys [::!selected] :as _control} module-id]
  (r/track (fn selected?-fn [] (contains? @!selected module-id))))

(defn set-!paginated-modules
  [{:keys [::!pagination ::!enable-pagination? ::!modules] :as control}]
  (assoc control
    ::!paginated-modules
    (pagination/!paginated-data-fn {:!pagination         !pagination
                                    :!enable-pagination? !enable-pagination?
                                    :!data               !modules})))

(defn transform
  [tree {:keys [parent-path] :as app}]
  (let [paths (if (str/blank? parent-path)
                [:applications]
                (-> parent-path
                    (str/split "/")
                    (conj :applications)))]
    (update-in tree paths conj app)))

(defn !apps-tree
  [{:keys [::!paginated-modules] :as _control}]
  (r/track (fn apps-tree-fn [] (reduce transform {} @!paginated-modules))))

(defn Application
  [{:keys [::!selected ::set-selected-fn] :as control}
   {:keys [id name subtype description] :as _module}]
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

(defn TabPane
  [{:keys [::tr-fn ::!loading? ::!modules ::!modules-tree ::text-filter-changed
           ::!enable-pagination? ::!page-sizes ::!pagination ::set-pagination-fn] :as control}]
  [ui/TabPane {:loading @!loading?}
   [FullTextSearch {:tr-fn     tr-fn
                    :on-change text-filter-changed}]
   (if (seq @!modules-tree)
     [ui/ListSA
      [Node control (dissoc @!modules-tree :applications) (:applications @!modules-tree)]]
     [uix/MsgNoItemsToShow "No applications found"])
   (when @!enable-pagination?
     [pagination/PaginationController
      {:!enable-pagination? !enable-pagination?
       :total-items         (count @!modules)
       :!page-sizes         !page-sizes
       :!pagination         !pagination
       :set-pagination-fn   set-pagination-fn
       :tr-fn               tr-fn}])])

(defn ModuleSelector
  [control]
  (let [{:keys [::!subtypes ::!modules ::!loading? ::!disabled? ::set-filters-fn ::!selected ::set-selected-fn
                ::!enable-pagination? ::!page-sizes ::!pagination ::set-pagination-fn ::tr-fn] :as control}
        (-> control
            set-!paginated-modules)
        !modules-tree       (!apps-tree control)
        !text-filter        (r/atom nil)
        !active-pane        (r/atom :app-store)
        filters-changed     (fn []
                              (when set-filters-fn
                                (set-filters-fn {:category @!active-pane
                                                 :text     @!text-filter})))
        set-active-pane-fn  (fn [active-pane-key]
                              (reset! !active-pane active-pane-key)
                              (set-pagination-fn (assoc @!pagination :page-index 0))
                              (filters-changed))
        text-filter-changed (fn [text]
                              (reset! !text-filter text)
                              (filters-changed))
        render              (fn []
                              (r/as-element
                                [TabPane {::tr-fn               tr-fn
                                          ::!loading?           !loading?
                                          ::!modules            !modules
                                          ::!modules-tree       !modules-tree
                                          ::text-filter-changed text-filter-changed
                                          ::!selected           !selected
                                          ::set-selected-fn     set-selected-fn
                                          ::!enable-pagination? !enable-pagination?
                                          ::!page-sizes         !page-sizes
                                          ::!pagination         !pagination
                                          ::set-pagination-fn   set-pagination-fn}]))
        panes               [{:menuItem {:content (general-utils/capitalize-words (tr-fn [:appstore]))
                                         :key     :app-store
                                         :icon    (r/as-element [icons/StoreIcon])}
                              :render   render}
                             {:menuItem {:content (general-utils/capitalize-words (tr-fn [:all-apps]))
                                         :key     :all-apps
                                         :icon    icons/i-grid-layout}
                              :render   render}
                             {:menuItem {:content (general-utils/capitalize-words (tr-fn [:my-apps]))
                                         :key     :my-apps
                                         :icon    "user"}
                              :render   render}]]
    (filters-changed)
    (fn [{:keys [::!value ::set-value-fn ::!subtypes ::!disabled? ::set-filters-fn ::tr-fn] :as _control}]
      [nav-tab/NavTabController
       {:!panes             (r/atom panes)
        :!active-pane       !active-pane
        :set-active-pane-fn set-active-pane-fn}])))

(defn ModuleSelectorController
  [{:keys [!modules
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
  (r/with-let [!modules            (or !modules (r/atom []))
               !loading?           (or !loading? (r/atom false))
               !selected           (or !selected (r/atom #{}))
               set-selected-fn     (or set-selected-fn #(reset! !selected %))
               !enable-pagination? (or !enable-pagination? (r/atom false))
               set-pagination-fn   (or set-pagination-fn #(reset! !pagination %))
               !disabled?          (or !disabled? (r/atom false))
               tr-fn               (or tr-fn (comp str/capitalize name first))]
    [ModuleSelector {::!modules            !modules
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

