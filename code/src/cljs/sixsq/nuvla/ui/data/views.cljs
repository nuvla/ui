(ns sixsq.nuvla.ui.data.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.apps.utils :as application-utils]
    [sixsq.nuvla.ui.data.events :as events]
    [sixsq.nuvla.ui.data-set.events :as data-set-events]
    [sixsq.nuvla.ui.data-set.views :as data-set-views]
    [sixsq.nuvla.ui.data.subs :as subs]
    [sixsq.nuvla.ui.data.utils :as utils]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as deployment-dialog-subs]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as utils-values]
    [taoensso.timbre :as log]

    [sixsq.nuvla.ui.utils.spec :as us]
    [sixsq.nuvla.ui.filter-comp.views :as filter-comp]))


(def view-type (r/atom :cards))


(defn refresh []
  (dispatch [::events/refresh]))


(defn NewDatasetModal []
  (let [open?                  (subscribe [::subs/modal-open?])
        tr                     (subscribe [::i18n-subs/tr])
        form!                  (subscribe [::subs/add-data-set-form])
        module-filter-key      :module-filter
        data-record-filter-key :data-record-filter
        set-form-value         (fn [k v] (dispatch [::events/set-add-data-set-form k v]))
        filter-record-open?    (r/atom false)
        filter-module-open?    (r/atom false)]
    (fn []
      (when @open?
        [ui/Modal
         {:size      :tiny
          :open      @open?
          :closeIcon true
          :on-close  #(dispatch [::events/set-modal-open? false])}

         [uix/ModalHeader {:header (@tr [:new-dataset])
                           :icon   "add"}]
         [ui/ModalContent
          [ui/Form
           [ui/FormInput
            {:label      (str/capitalize (@tr [:name]))
             :required   true
             :auto-focus "on"
             :on-change  (ui-callback/input-callback (partial set-form-value :name))}]
           [ui/FormInput
            {:label     (str/capitalize (@tr [:description]))
             :on-change (ui-callback/input-callback (partial set-form-value :description))}]
           [ui/FormField
            [:label "Data records filter"]
            [components/SearchInput
             {:on-change     (ui-callback/input-callback (partial set-form-value data-record-filter-key))
              :default-value ""
              :placeholder   "Data records filter"
              :action        (r/as-element
                               ^{:key (random-uuid)}
                               [filter-comp/ButtonFilter
                                {:resource-name  "data-record"
                                 :default-filter (get @form! data-record-filter-key "")
                                 :open?          filter-record-open?
                                 :on-done        (partial set-form-value data-record-filter-key)}])}]]
           [ui/FormField
            [:label "Applications filter"]
            [components/SearchInput
             {:on-change     (ui-callback/input-callback (partial set-form-value module-filter-key))
              :default-value ""
              :placeholder   "Applications filter"
              :action        (r/as-element
                               ^{:key (random-uuid)}
                               [filter-comp/ButtonFilter
                                {:resource-name  "module"
                                 :default-filter (get @form! module-filter-key "")
                                 :open?          filter-module-open?
                                 :on-done        (partial set-form-value module-filter-key)}])}]]]]
         [ui/ModalActions
          [uix/Button
           {:text     (str/capitalize (@tr [:create]))
            :positive true
            :on-click #(dispatch [::events/add-data-set])}]]]))))


(defn ProcessButton
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        data-sets (subscribe [::subs/selected-data-set-ids])]
    (fn []
      [uix/MenuItem
       {:name     (@tr [:process])
        :disabled (not (seq @data-sets))
        :icon     "rocket"
        :on-click #(dispatch [::main-events/subscription-required-dispatch
                              [::events/open-application-select-modal]])}])))


(defn RefreshMenuItem
  []
  [components/RefreshMenu
   {:on-refresh refresh}])


(defn AddDataSet []
  (let [tr           (subscribe [::i18n-subs/tr])
        active-index (subscribe [::subs/active-tab-index])]
    [uix/MenuItem
     {:name     (@tr [:add])
      :icon     "add"
      :disabled (= @active-index 1)
      :on-click #(dispatch [::events/set-modal-open? true])}]))


(defn AddButton
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/MenuItem
     {:name     (@tr [:add])
      :icon     "add"
      :on-click #(dispatch
                   [::main-events/subscription-required-dispatch
                    [::events/open-modal :add]])}]))


(defn MenuBar
  []
  [components/StickyBar
   [ui/Menu {:borderless true, :stackable true}
    [ProcessButton]
    [AddDataSet]
    [ui/MenuItem {:icon     "grid layout"
                  :active   (= @view-type :cards)
                  :on-click #(reset! view-type :cards)}]
    [ui/MenuItem {:icon     "table"
                  :active   (= @view-type :table)
                  :on-click #(reset! view-type :table)}]
    [ui/MenuItem {:icon     "map"
                  :active   (= @view-type :map)
                  :on-click #(reset! view-type :map)}]
    [components/RefreshMenu
     {:on-refresh refresh}]]])


(defn SearchBar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:div {:style {:padding "10px 0"}}
     [ui/Message {:info true}
      (@tr [:data-set-search-message])]
     [data-set-views/SearchHeader refresh ::events/set-full-text-search ::subs/full-text-search]]))

(defn ApplicationListItem
  [{:keys [id name description subtype created] :as _application}]
  (let [selected-application-id (subscribe [::subs/selected-application-id])
        on-click-fn             #(dispatch [::events/set-selected-application-id id])]
    ^{:key id}
    [ui/ListItem {:active   (and @selected-application-id (= id @selected-application-id))
                  :on-click on-click-fn}
     [ui/ListIcon {:name (application-utils/subtype-icon subtype), :size "large"}]
     [ui/ListContent
      [ui/ListHeader (str (or name id) " (" (time/ago (time/parse-iso8601 created)) ")")]
      (or description "")]]))


(defn ApplicationList
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        applications (subscribe [::subs/applications])
        loading?     (subscribe [::subs/loading-applications?])]
    [ui/Segment {:loading @loading?
                 :basic   true}
     (if (seq @applications)
       (vec (concat [ui/ListSA {:divided   true
                                :relaxed   true
                                :selection true}]
                    (mapv ApplicationListItem @applications)))
       [ui/Message {:error true} (@tr [:no-apps])])]))


(defn LaunchButton
  []
  (let [tr                     (subscribe [::i18n-subs/tr])
        visible?               (subscribe [::subs/application-select-visible?])
        selected-app-id        (subscribe [::subs/selected-application-id])

        data-step-active?      (subscribe [::deployment-dialog-subs/data-step-active?])

        deployment             (subscribe [::deployment-dialog-subs/deployment])
        data-completed?        (subscribe [::deployment-dialog-subs/data-completed?])
        credentials-completed? (subscribe [::deployment-dialog-subs/credentials-completed?])
        env-completed?         (subscribe [::deployment-dialog-subs/env-variables-completed?])
        hide-fn                #(do
                                  (dispatch [::events/close-application-select-modal])
                                  (dispatch [::events/delete-deployment]))
        configure-fn           (fn [id]
                                 (dispatch [::events/close-application-select-modal])
                                 (dispatch [::deployment-dialog-events/create-deployment id :data]))

        launch-fn              #(do
                                  (dispatch [::events/close-application-select-modal])
                                  (dispatch [::deployment-dialog-events/edit-deployment]))]

    (fn []
      (let [launch-disabled? (or (not @deployment)
                                 (and (not @data-completed?) @data-step-active?)
                                 (not @credentials-completed?)
                                 (not @env-completed?))]

        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

         [uix/ModalHeader {:header (@tr [:select-application]) :icon "sitemap"}]

         [ui/ModalContent {:scrolling true}
          [ui/ModalDescription
           [ApplicationList]]]
         [ui/ModalActions
          [ui/Button {:disabled (nil? @selected-app-id)
                      :primary  true
                      :on-click #(configure-fn @selected-app-id)}
           [ui/Icon {:name "settings"}]
           (@tr [:configure])]
          [ui/Button {:disabled (nil? @selected-app-id)
                      :primary  true
                      :on-click #()}
           [ui/Icon {:name     "rocket"
                     :disabled launch-disabled?
                     :on-click launch-fn}]
           (@tr [:launch])]]]))))


(defn ApplicationSelectModal
  []
  (let [tr                     (subscribe [::i18n-subs/tr])
        visible?               (subscribe [::subs/application-select-visible?])
        selected-app-id        (subscribe [::subs/selected-application-id])

        data-step-active?      (subscribe [::deployment-dialog-subs/data-step-active?])

        deployment             (subscribe [::deployment-dialog-subs/deployment])
        data-completed?        (subscribe [::deployment-dialog-subs/data-completed?])
        credentials-completed? (subscribe [::deployment-dialog-subs/credentials-completed?])
        env-completed?         (subscribe [::deployment-dialog-subs/env-variables-completed?])
        hide-fn                #(do
                                  (dispatch [::events/close-application-select-modal])
                                  (dispatch [::events/delete-deployment]))
        configure-fn           (fn [id]
                                 (dispatch [::events/close-application-select-modal])
                                 (dispatch [::deployment-dialog-events/create-deployment id :data]))

        launch-fn              #(do
                                  (dispatch [::events/close-application-select-modal])
                                  (dispatch [::deployment-dialog-events/edit-deployment]))]
    (fn []
      (let [launch-disabled? (or (not @deployment)
                                 (and (not @data-completed?) @data-step-active?)
                                 (not @credentials-completed?)
                                 (not @env-completed?))]

        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

         [uix/ModalHeader {:header (@tr [:select-application]) :icon "sitemap"}]

         [ui/ModalContent {:scrolling true}
          [ui/ModalDescription
           [ApplicationList]]]
         [ui/ModalActions
          [ui/Button {:disabled (nil? @selected-app-id)
                      :primary  true
                      :on-click #(configure-fn @selected-app-id)}
           [ui/Icon {:name "settings"}]
           (@tr [:configure])]
          [ui/Button {:disabled (or (nil? @selected-app-id)
                                    launch-disabled?)
                      :primary  true
                      :on-click launch-fn}
           [ui/Icon {:name "rocket"}]
           (@tr [:launch])]]]))))


(defn Pagination
  []
  (let [elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        total-elements    (subscribe [::subs/total])
        total-pages       (utils-general/total-pages @total-elements @elements-per-page)]

    [uix/Pagination {:totalitems              @total-elements
                     :totalPages              total-pages
                     :activePage              @page
                     :elementsperpage         @elements-per-page
                     :onElementsPerPageChange (ui-callback/value
                                                #(do (dispatch [::events/set-elements-per-page %])
                                                     (dispatch [::events/set-page 1])
                                                     (dispatch [::events/get-data-sets])))
                     :onPageChange            (ui-callback/callback
                                                :activePage #(dispatch [::events/set-page %]))}]))


(defn DataSetRow
  [{:keys [id name description tags created module-filter data-record-filter] :as _data-set}]
  (fn [_data-set]
    ^{:key id}
    (let [uuid (utils-general/id->uuid id)]
      [ui/TableRow
       [ui/TableCell name]
       [ui/TableCell description]
       [ui/TableCell (utils-values/format-created created)]
       [ui/TableCell data-record-filter]
       [ui/TableCell module-filter]
       [ui/TableCell [uix/Tags tags]]
       [ui/TableCell (utils-values/as-link id :label uuid)]])))


(defn DataSetTable
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        data-sets (subscribe [::subs/data-sets])]
    [:<>
     [ui/Table {:compact "very", :selectable true}
      [ui/TableHeader
       [ui/TableRow
        [ui/TableHeaderCell (@tr [:name])]
        [ui/TableHeaderCell (@tr [:description])]
        [ui/TableHeaderCell (@tr [:created])]
        [ui/TableHeaderCell (@tr [:filter])]
        [ui/TableHeaderCell (@tr [:module-filter])]
        [ui/TableHeaderCell (@tr [:tags])]
        [ui/TableHeaderCell (@tr [:id])]]]
      [ui/TableBody
       (for [[id ds] @data-sets]
         ^{:key id}
         [DataSetRow ds])]]
     [Pagination]]))


(defn DataSetCard
  [{:keys [id name description tags] :as _data-set}]
  (let [tr        (subscribe [::i18n-subs/tr])
        counts    (subscribe [::subs/counts])
        sizes     (subscribe [::subs/sizes])
        data-sets (subscribe [::subs/selected-data-set-ids])
        count     (get @counts id "...")
        size      (get @sizes id "...")
        selected? (boolean (@data-sets id))]
    ^{:key id}
    [uix/Card
     {:header      name
      :description description
      :tags        tags
      :extra       [:<>
                    [ui/Label
                     [ui/Icon {:name "file"}]
                     [:span (str count " " (@tr [:objects]))]]
                    [ui/Label
                     [ui/Icon {:name "expand arrows alternate"}]
                     [:span (utils/format-bytes size)]]]
      :on-select   #(dispatch [::events/toggle-data-set-id id])
      :selected?   selected?
      :on-click    (fn [event]
                     (dispatch [::history-events/navigate (utils/data-record-href id)])
                     (.preventDefault event))}]))


(defn DataSetCards
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        data-sets (subscribe [::subs/data-sets])
        loading?  (subscribe [::main-subs/loading?])]
    (fn []
      (if @loading?
        [ui/Loader {:active true
                    :inline "centered"}]
        [:<>
         [:div style/center-items
          (if (not (seq @data-sets))
            [ui/Message {:warning true}
             [ui/Icon {:name "warning sign"}]
             (@tr [:no-datasets])]
            (vec (concat [ui/CardGroup {:centered    true
                                        :itemsPerRow 4}]
                         (map (fn [data-set]
                                [DataSetCard data-set])
                              (vals @data-sets)))))]
         [Pagination]]))))


(defn MainActionButton
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        data-sets (subscribe [::subs/selected-data-set-ids])]
    [ui/ButtonGroup {:primary true
                     :style   {:padding-top 10}}
     [ui/Button
      {:content  (@tr [:process])
       :disabled (not (seq @data-sets))
       :icon     "rocket"
       :on-click #(dispatch [::main-events/subscription-required-dispatch
                             [::events/open-application-select-modal]])}]]))


(defn DataRecords
  []
  (dispatch [::data-set-events/get-all-data-records])
  [:<>
   (case @view-type
     :cards [data-set-views/DataRecordCards data-set-views/PaginationAllDataRecords]
     :table [data-set-views/DataRecordTable data-set-views/PaginationAllDataRecords])
   [MainActionButton]])


(defn DataSets
  []
  [:<>
   [SearchBar]
   [ApplicationSelectModal]
   (case @view-type
     :cards [DataSetCards]
     :table [DataSetTable]
     :map [DataSetCards])
   [deployment-dialog-views/deploy-modal true]
   [NewDatasetModal]
   [MainActionButton]])


(defn data-sets
  []
  {:menuItem {:content (r/as-element [:span "Datasets"])
              :key     "data-sets"
              :icon    "object group"}
   :render   (fn [] (r/as-element [DataSets]))})


(defn data-records
  []
  {:menuItem {:content (r/as-element [:span "Data records"])
              :key     "data-records"
              :icon    "file"}
   :render   (fn [] (r/as-element [DataRecords]))})


(defn data-panes
  []
  [(data-sets)
   (data-records)])


(defn Data
  []
  (refresh)
  (let [tr           (subscribe [::i18n-subs/tr])
        active-index (subscribe [::subs/active-tab-index])]
    (fn []
      [components/LoadingPage {}
       [ui/Segment style/basic
        [uix/PageHeader "database" (@tr [:data-processing])]
        [MenuBar]
        [ui/Tab
         {:menu        {:secondary true
                        :pointing  true
                        :style     {:display        "flex"
                                    :flex-direction "row"
                                    :flex-wrap      "wrap"}}
          :panes       (data-panes)
          :activeIndex @active-index
          :onTabChange (fn [_ data]
                         (let [active-index (. data -activeIndex)]
                           (dispatch [::events/set-active-tab-index active-index])))}]]])))


(defmethod panel/render :data
  [path]
  (let [[_ uuid] path
        n (count path)]
    (case n
      2 [data-set-views/DataSet uuid]
      [Data])))
