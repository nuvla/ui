(ns sixsq.nuvla.ui.components.table-refactor-scenes
  (:require [reagent.core :as r]
            [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table-refactor]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn TableController
  [{:keys [!enable-sorting? !enable-pagination?] :as control}]
  (let [enable-sorting?    (if !enable-sorting? @!enable-sorting? false)
        enable-pagination? (if !enable-pagination? @!enable-pagination? false)]
    [table-refactor/TableController
     (merge {;; for testing purposes disable all functionalities that are otherwise enabled
             ;; by default. They can be enabled in each test via the control map.
             :!enable-column-customization? (r/atom false)
             :!enable-sorting?              (r/atom false)}
            control
            {:!columns         (r/atom (cond->
                                         [{::table-refactor/field-key      :Id
                                           ::table-refactor/header-content "Id"
                                           ::table-refactor/no-delete      true}
                                          {::table-refactor/field-key      :Size
                                           ::table-refactor/header-content "Size"}
                                          {::table-refactor/field-key      :Created
                                           ::table-refactor/header-content "Created"}]
                                         enable-pagination?
                                         (conj {::table-refactor/field-key      :Idx
                                                ::table-refactor/header-content "Idx"
                                                ::table-refactor/no-delete      true})
                                         enable-sorting?
                                         (conj {::table-refactor/field-key      :NoSort
                                                ::table-refactor/header-content "Not sortable"
                                                ::table-refactor/no-sort?       true})))
             :!default-columns (r/atom (cond-> [:Id :Size :Created]
                                               enable-pagination? (conj :Idx)
                                               enable-sorting? (conj :NoSort)))
             :row-id-fn        (if enable-pagination? :Idx :Id)
             :!data            (r/atom (cond->
                                         [{:RepoDigests
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
                                         enable-pagination? (->> (repeat 101)
                                                                 (apply concat)
                                                                 (map-indexed (fn [idx item] (assoc item :Idx idx)))
                                                                 vec)))})]))

(defscene basic-table
  [TableController])

(defn ColumnCustomizationParams
  [!enable-column-customization?]
  [:div {:style {:margin-bottom "5px"}}
   [ui/Checkbox {:data-testid "checkbox-enable-column-customization"
                 :label       "Enable column customization ?"
                 :style       {:position       :relative
                               :vertical-align :middle}
                 :checked     @!enable-column-customization?
                 :on-click    #(swap! !enable-column-customization? not)}]])

(defscene column-customization
  (r/with-let [!enable-column-customization? (r/atom true)]
    [:div
     [ColumnCustomizationParams !enable-column-customization?]
     [TableController {:!enable-column-customization? !enable-column-customization?}]]))

(defn RowSelectionParams
  [!enable-row-selection? !selected]
  [:div {:style {:margin-bottom "5px"}}
   [ui/Checkbox {:data-testid "checkbox-enable-row-selection"
                 :label       "Enable row selection ?"
                 :style       {:position       :relative
                               :vertical-align :middle}
                 :checked     @!enable-row-selection?
                 :on-click    #(swap! !enable-row-selection? not)}]
   (when @!enable-row-selection?
     [:div {:data-testid "selected-items-summary"
            :style       {:margin "10px"}}
      (str (count @!selected) " items selected")])])

(defscene row-selection
  (r/with-let [!enable-row-selection? (r/atom true)
               !selected              (r/atom #{})]
    [:div
     [RowSelectionParams !enable-row-selection? !selected]
     [TableController {:!enable-row-selection? !enable-row-selection?
                       :!selected              !selected}]]))

(defscene clickable-rows
  (r/with-let [clicked-rows (r/atom #{})
               on-row-click #(swap! clicked-rows conj %)]
    [:div
     [:div {:data-testid "clicked-rows-summary"
            :style       {:margin "10px"}}
      (str (count @clicked-rows) " rows clicked")]
     [TableController {:on-row-click on-row-click}]]))

(defn SearchInput
  [!global-filter]
  (js/console.info "Render SearchInput")
  [:div
   [ui/Input {:value       @!global-filter
              :style       {:padding "4px"}
              :class       :global-filter
              :placeholder "search..."
              :on-change   (ui-callback/input-callback #(reset! !global-filter %))}]])

(defn GlobalFilterParams
  [!enable-global-filter? !global-filter]
  [:div {:style {:margin-bottom "5px"}}
   [ui/Checkbox {:data-testid "checkbox-enable-global-filter"
                 :label       "Enable global filter ?"
                 :style       {:position       :relative
                               :vertical-align :middle}
                 :checked     @!enable-global-filter?
                 :on-click    #(swap! !enable-global-filter? not)}]
   (when @!enable-global-filter?
     [SearchInput !global-filter])])

(defscene global-filter
  (r/with-let [!enable-column-customization? (r/atom true)
               !enable-global-filter?        (r/atom true)
               !global-filter                (r/atom "")]
    [:div
     [GlobalFilterParams !enable-global-filter? !global-filter]
     [TableController {:!enable-column-customization? !enable-column-customization?
                       :!enable-global-filter?        !enable-global-filter?
                       :!global-filter                !global-filter}]]))

(defn SortingParams
  [!enable-sorting?]
  [:div {:style {:margin-bottom "5px"}}
   [ui/Checkbox {:data-testid "checkbox-enable-sorting"
                 :label       "Enable sorting ?"
                 :style       {:position       :relative
                               :vertical-align :middle}
                 :checked     @!enable-sorting?
                 :on-click    #(swap! !enable-sorting? not)}]])

(defscene sorting
  (r/with-let [!enable-sorting? (r/atom true)
               !sorting         (r/atom [[:Created "asc"]])]
    [:div
     [SortingParams !enable-sorting?]
     [TableController {:!enable-sorting? !enable-sorting?
                       :!sorting         !sorting}]]))

(defn PaginationParams
  [!enable-pagination?]
  [:div {:style {:margin-bottom "5px"}}
   [ui/Checkbox {:data-testid "checkbox-enable-pagination"
                 :label       "Enable pagination ?"
                 :style       {:position       :relative
                               :vertical-align :middle}
                 :checked     @!enable-pagination?
                 :on-click    #(swap! !enable-pagination? not)}]])

(defscene pagination
  (r/with-let [!enable-pagination? (r/atom true)
               !pagination         (r/atom {:page-index 0
                                            :page-size  25})]
    [:div
     [PaginationParams !enable-pagination?]
     [TableController {:!enable-pagination? !enable-pagination?
                       :!pagination         !pagination}]]))

(defscene filter-sort-paginate-select
  (r/with-let [!enable-global-filter?        (r/atom true)
               !enable-row-selection?        (r/atom true)
               !selected                     (r/atom #{})
               !enable-sorting?              (r/atom true)
               !enable-pagination?           (r/atom true)
               !enable-column-customization? (r/atom false)
               !global-filter                (r/atom "")
               !pagination                   (r/atom {:page-index 0
                                                      :page-size  25})]
    [:div
     [GlobalFilterParams !enable-global-filter? !global-filter]
     [RowSelectionParams !enable-row-selection? !selected]
     [SortingParams !enable-sorting?]
     [PaginationParams !enable-pagination?]
     [ColumnCustomizationParams !enable-column-customization?]
     [TableController {:!enable-global-filter?        !enable-global-filter?
                       :!enable-column-customization? !enable-column-customization?
                       :!enable-row-selection?        !enable-row-selection?
                       :!selected                     !selected
                       :!enable-sorting?              !enable-sorting?
                       :!enable-pagination?           !enable-pagination?
                       :!global-filter                !global-filter
                       :!pagination                   !pagination}]]))
