(ns sixsq.nuvla.ui.pages.deployments-detail.views-coe-resources
  (:require [clojure.edn :as edn]
            [re-frame.core :refer [dispatch subscribe reg-event-fx reg-sub]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(def local-storage-key "nuvla.ui.table.deployments.coe-resources.column-configs")

(def default-pagination {:page-index 0
                         :page-size  25})

(reg-sub
  ::current-cols
  (fn [db [_ k]]
    (let [ls   (aget js/window "localStorage")
          data (or
                 (get db local-storage-key)
                 (edn/read-string (.getItem ls local-storage-key)))]
      (get data k))))

(main-events/reg-set-current-cols-event-fx ::set-current-cols local-storage-key)

(defn CellKeyValueLabelGroup
  [cell-data _row _column]
  (uix/label-group-overflow-detector
    (fn []
      [ui/LabelGroup
       (for [[k v] cell-data]
         ^{:key k}
         [ui/Label {:style   {:white-space :pre}
                    :content k :detail (str v)}])])))

(defn CellLabelGroup
  [cell-data _row _column]
  (uix/label-group-overflow-detector
    (fn []
      [ui/ListSA
       (for [v cell-data]
         ^{:key (str v)}
         [ui/ListItem [ui/Label {:style {:white-space :pre} :content v}]])])))

(defn CellBoolean
  [cell-data _row _column]
  [:div {:style {:text-align :center}}
   (if (true? cell-data)
     [icons/CheckIconFull]
     [icons/MinusIcon])])

(defn CellJson
  [cell-data row _column]
  [ui/Modal {:trigger    (r/as-element [:span [icons/ZoomInIcon {:style {:cursor :pointer}}]])
             :close-icon true}
   [ui/ModalHeader (or (:name row) (first (:names row)) (:uid row))]
   [ui/ModalContent {:scrolling true}
    [uix/EditorJson {:value     (general-utils/edn->json cell-data)
                     :read-only true}]]])

(defn CoeTable
  [{:keys [::!selected ::set-selected-fn ::!global-filter ::!pagination ::!can-action?] :as control} k]
  (let [{:keys [::!data ::!columns ::!default-columns ::row-id-fn]} (get control k)]
    [table/TableController
     {:!columns                      !columns
      :!default-columns              !default-columns
      :!current-columns              (subscribe [::main-subs/current-cols local-storage-key k])
      :set-current-columns-fn        #(dispatch [::set-current-cols k %])
      :!data                         !data
      :!global-filter                !global-filter
      :!enable-pagination?           (r/atom true)
      :!pagination                   !pagination
      :!enable-global-filter?        (r/atom true)
      :!enable-column-customization? (r/atom true)
      :row-id-fn                     (or row-id-fn :Id)}]))

(defn COETabPane [control k]
  [ui/TabPane {:key k}
   [CoeTable control k]])

(defn Tab [title-k-data-list]
  (r/with-let [!global-filter        (r/atom "")
               !pagination           (r/atom default-pagination)
               control               (merge {::!global-filter        !global-filter
                                             ::!pagination           !pagination}
                                            (into {} (mapv
                                                       (fn [[_title k data]]
                                                         [k data])
                                                       title-k-data-list)))]
    [ui/Tab
     {:menu          {:style     {:display   :flex
                                  :flex-wrap :wrap}
                      :secondary true
                      :pointing  true}
      :on-tab-change #(do (reset! !pagination default-pagination)
                          (reset! !global-filter ""))
      :panes         (mapv
                       (fn [[title k _data]]
                         {:menuItem title,
                          :render   #(r/as-element [COETabPane control k])})
                       title-k-data-list)}]))

