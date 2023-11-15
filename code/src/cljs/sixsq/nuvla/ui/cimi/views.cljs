(ns sixsq.nuvla.ui.cimi.views
  (:require [cljs.pprint :refer [pprint]]
            [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-detail.views :as cimi-detail-views]
            [sixsq.nuvla.ui.cimi.events :as events]
            [sixsq.nuvla.ui.cimi.subs :as subs]
            [sixsq.nuvla.ui.cimi.utils :as cimi-utils]
            [sixsq.nuvla.ui.filter-comp.views :as filter-comp]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.routing.utils :refer [name->href str-pathify]]
            [sixsq.nuvla.ui.utils.forms :as forms]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.response :as response]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn id-selector-formatter
  [{:keys [id] :as _entry}]
  (when id
    [uix/Link (str "api/" id) (general-utils/id->uuid id)]))

(defn field-selector
  [field]
  (let [ks (map keyword (str/split field #"/"))]
    (fn [m]
      (str (get-in m ks)))))

(defn table-header-cell
  [label]
  (let [sort-icon      (subscribe [::subs/orderby-label-icon label])
        next-direction (case @sort-icon
                         "sort ascending" (str label ":desc")
                         "sort descending" ""
                         "sort" (str label ":asc"))]
    [ui/TableHeaderCell
     [uix/LinkIcon {:name     "remove circle"
                    :on-click #(dispatch [::events/remove-field label])}]
     " " label " "
     [uix/LinkIcon {:name     @sort-icon
                    :on-click #(do (dispatch [::events/set-query-param :orderby next-direction])
                                   (dispatch [::events/get-results]))}]]))

(defn results-table-header [selected-fields]
  (let [can-bulk-delete? (subscribe [::subs/can-bulk-delete?])]
    [ui/TableHeader
     [ui/TableRow
      (when @can-bulk-delete?
        [ui/TableHeaderCell
         [ui/Checkbox {:on-change (ui-callback/checked #(dispatch [::events/select-all-row %]))}]])
      (for [selected-field selected-fields]
        ^{:key selected-field}
        [table-header-cell selected-field])]]))

(defn results-table-row-fn [selected-fields]
  (apply juxt (map (fn [selected-field] (if (= "id" selected-field)
                                          id-selector-formatter
                                          (field-selector selected-field)))
                   selected-fields)))

(defn results-table-row [row-fn entry i]
  (let [can-bulk-delete? (subscribe [::subs/can-bulk-delete?])]
    (when entry
      (let [data          (row-fn entry)
            id            (:id entry)
            row-selected? (subscribe [::subs/row-selected? id])]
        [ui/TableRow {:style    {:cursor "pointer"}
                      :on-click #(dispatch [::routing-events/navigate
                                            (str-pathify (name->href routes/api) id)])}
         (when @can-bulk-delete?
           [ui/TableCell
            [ui/Checkbox {:checked  @row-selected?
                          :on-click (fn [event]
                                      (dispatch [::events/select-row @row-selected? id])
                                      (.stopPropagation event))}]])
         (for [[j v] (map-indexed vector data)]
           ^{:key (str "row-" i "-cell-" j)}
           [ui/TableCell v])]))))

(defn results-table-body [row-fn entries]
  [ui/TableBody
   (for [[i entry] (map-indexed vector entries)]
     ^{:key (str "row-" i)}
     [results-table-row row-fn entry i])])

(defn results-table
  [selected-fields entries]
  (when (and (pos? (count entries))
             (pos? (count selected-fields)))
    (let [row-fn (results-table-row-fn selected-fields)]
      [:div {:class-name "nuvla-ui-x-autoscroll"}
       [ui/Table
        {:collapsing  true
         :compact     "very"
         :unstackable true
         :single-line true
         :selectable  true
         :padded      false}
        (results-table-header selected-fields)
        (results-table-body row-fn entries)]])))

(defn Statistic
  [[label value :as data]]
  (when data
    ^{:key label}
    [ui/Statistic {:size "tiny"}
     [ui/StatisticValue (or value 0)]
     [ui/StatisticLabel label]]))

(defn ResultsStatistic []
  (let [tr         @(subscribe [::i18n-subs/tr])
        collection @(subscribe [::subs/collection])]
    (when collection
      (let [total (:count collection)
            n     (count (get collection :resources []))]
        [ui/Statistic
         [ui/StatisticValue (str n " / " total)]
         [ui/StatisticLabel (tr [:results])]]))))

(defn AggregationsTable []
  (let [aggregations @(subscribe [::subs/aggregations-keys-values])]
    [ui/StatisticGroup {:size "tiny"}
     [ResultsStatistic]
     (for [agg aggregations]
       ^{:key (first agg)} [Statistic agg])]))

(defn ResultsDisplay []
  (let [collection      @(subscribe [::subs/collection])
        selected-fields @(subscribe [::subs/selected-fields])]
    [ui/Segment style/basic
     (if (instance? js/Error collection)
       [:pre (with-out-str (pprint (ex-data collection)))]
       [:<>
        [AggregationsTable]
        [results-table selected-fields (get collection :resources [])]])]))


(defn CollectionSelector
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        options     (subscribe [::subs/collection-dropdown-options])
        selected-id (subscribe [::subs/collection-name])
        query-param (subscribe [::route-subs/nav-query-params])]
    (fn []
      (let [callback #(dispatch [::routing-events/navigate
                                 routes/api-sub-page {:sub-path %} @query-param])]
        [ui/FormDropdown
         {:aria-label           (@tr [:resource-type])
          :style                {:max-width 250}
          :value                @selected-id
          :placeholder          (@tr [:resource-type])
          :tab-index            1
          :scrolling            true
          :search               true
          :selection            true
          :upward               false
          :options              @options
          :select-on-navigation false
          :on-change            (ui-callback/value callback)}]))))

(defn DocumentationButton
  []
  (let [tr                 @(subscribe [::i18n-subs/tr])
        documentation-page @(subscribe [::main-subs/page-info "documentation"])]
    [ui/MenuItem
     {:on-click #(dispatch [::routing-events/navigate (:key documentation-page)])}
     [icons/Icon {:name (:icon documentation-page)}]
     (tr [(:label-kw documentation-page)])]))

(def search-header-fields [{:k         :first
                            :label     :first
                            :tab-index 2
                            :type      "number"
                            :min       0
                            :cols      "1/3"}
                           {:label     :last
                            :k         :last
                            :tab-index 3
                            :type      "number"
                            :min       0
                            :max       10000
                            :cols      "3/5"}
                           {:label       :select
                            :k           :select
                            :tab-index   4
                            :type        "text"
                            :placeholder "e.g. id, endpoint, ..."
                            :cols        "5/7"}
                           {:label       :order
                            :k           :orderby
                            :tab-index   5
                            :type        "text"
                            :placeholder "e.g. created:desc, ..."
                            :cols        "1/4"}
                           {:label       :aggregation
                            :k           :aggregation
                            :tab-index   6
                            :type        "text"
                            :placeholder "e.g. min:resource:vcpu, ..."
                            :cols        "4/7"}])

(defn SearchField
  [{:keys [k label tab-index min max type placeholder cols]}]
  (let [tr    @(subscribe [::i18n-subs/tr])
        value @(subscribe [::subs/query-param k])]
    [ui/FormField
     {:style {:grid-column cols}}
     [ui/Input
      {:class "labeled"}
      [ui/Label (tr [label])]
      [:input {:aria-label  (tr [label])
               :tab-index   tab-index
               :type        type
               :min         min
               :max         max
               :value       value
               :placeholder placeholder
               :on-change   (ui-callback/input-callback
                              #(dispatch [::events/set-query-param k %]))}]]]))

(defn FilterField
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        selected-id  (subscribe [::subs/collection-name])
        value        (subscribe [::subs/query-param :filter])
        filter-open? (r/atom false)
        set-filter   #(dispatch [::events/set-query-param :filter %])]
    (fn []
      [ui/FormField
       {:style {:grid-column "1/7"}}
       [ui/Input
        {:class  "labeled"
         :action true}
        [ui/Label (@tr [:filter])]
        [:input {:aria-label  (@tr [:filter])
                 :tab-index   7
                 :type        "text"
                 :placeholder "e.g. connector/href^='exoscale-' and resource:type='VM' and resource:ram>=8096"
                 :value       @value
                 :on-change   (ui-callback/input-callback set-filter)}]
        ^{:key @selected-id}
        [filter-comp/ButtonFilter
         {:key            @value
          :resource-name  @selected-id
          :default-filter @value
          :disabled?      (nil? @selected-id)
          :open?          filter-open?
          :on-done        set-filter
          :persist?       false
          :trigger-style  {:border-radius 0}}]]])))

(defn SearchHeader []
  (let [selected-id (subscribe [::subs/collection-name])]
    (fn []
      [ui/Form {:aria-label   "filter parameters"
                :on-key-press (partial forms/on-return-key
                                       #(when @selected-id
                                          (dispatch [::events/get-results])))}
       [CollectionSelector]
       [ui/FormGroup {:class :cimi-filter-search-header}
        (for [field search-header-fields]
          ^{:key (:k field)}
          [SearchField field])
        [FilterField]]])))

(defn FormatFieldItem [selections-atom item field->view]
  (let [view         (get field->view item)
        tr           (subscribe [::i18n-subs/tr])
        label-string (if (keyword? item) (or (@tr [item]) item) item)]
    [ui/ListItem
     [ui/ListContent
      [ui/ListHeader {:style {:display :flex}}
       [ui/Checkbox {:checked   (contains? @selections-atom item)
                     :label     (if view (r/as-element [:label view]) label-string)
                     :on-change (ui-callback/checked (fn [checked]
                                                       (if checked
                                                         (swap! selections-atom set/union #{item})
                                                         (swap! selections-atom set/difference #{item}))))}]
       #_[:label view]]]]))

(defn format-field-list [available-fields-atom selections-atom field->view]
  (let [items (sort available-fields-atom)]
    (vec (concat [ui/ListSA]
                 (map (fn [item] [FormatFieldItem selections-atom item field->view]) items)))))

(defn SelectFieldsView
  [{:keys [show? selected-id-sub selections-atom
           selected-fields-sub available-fields
           update-fn trigger title-tr-key
           reset-to-default-fn field->view]}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Modal
     {:closeIcon true
      :open      @show?
      :on-close  #(reset! show? false)
      :trigger   (r/as-element
                   (or trigger
                       [uix/MenuItem
                        {:name     (@tr [:columns])
                         :icon     icons/i-columns
                         :disabled (and selected-id-sub (nil? @selected-id-sub))
                         :on-click (fn []
                                     (reset! selections-atom (set @selected-fields-sub))
                                     (reset! show? true))}]))}
     [uix/ModalHeader {:header (@tr [(or title-tr-key :fields)])}]
     [ui/ModalContent
      {:scrolling true}
      [format-field-list available-fields selections-atom field->view]]
     [ui/ModalActions
      (when (fn? reset-to-default-fn)
        [uix/Button
         {:text     "Select default columns"
          :on-click reset-to-default-fn}])
      [uix/Button
       {:text     (@tr [:cancel])
        :on-click #(reset! show? false)}]
      [uix/Button
       {:text     (@tr [:update])
        :primary  true
        :on-click (fn []
                    (reset! show? false)
                    (if (fn? update-fn)
                      (update-fn @selections-atom)
                      (dispatch [::events/set-selected-fields @selections-atom])))}]]]))

(defn SelectFields []
  (let [available-fields (subscribe [::subs/available-fields])
        selected-fields  (subscribe [::subs/selected-fields])
        selected-id      (subscribe [::subs/collection-name])
        selections       (r/atom (set @selected-fields))
        show?            (r/atom false)]
    (fn []
      [SelectFieldsView {:show?               show?
                         :selected-id-sub     selected-id
                         :selections-atom     selections
                         :selected-fields-sub selected-fields
                         :available-fields    @available-fields}])))

(defn ResourceAddForm
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        show?            (subscribe [::subs/show-add-modal?])
        collection-name  (subscribe [::subs/collection-name])
        default-text     (general-utils/edn->json {})
        text             (atom default-text)
        selected-tmpl-id (r/atom nil)]
    (fn []
      (let [collection-tmpl-href (some-> @collection-name cimi-utils/collection-template-href)
            templates-info       (subscribe [::subs/collection-templates collection-tmpl-href])]
        (when @show?
          [ui/Modal
           {:size    "large", :closeIcon true, :open @show?,
            :onClose #(dispatch [::events/hide-add-modal])}

           [uix/ModalHeader {:header (str (@tr [:add]))} " " @collection-name]

           [ui/ModalContent
            [:<>
             (when @templates-info
               [ui/Dropdown {:style       {:margin-bottom 10}
                             :selection   true
                             :placeholder "select a resource template"
                             :value       @selected-tmpl-id
                             :options     (forms/descriptions->options (vals @templates-info))
                             :on-change   (ui-callback/value
                                            (fn [value]
                                              (reset! selected-tmpl-id value)
                                              (reset! text
                                                      (-> @templates-info
                                                          (get value)
                                                          general-utils/remove-common-attrs
                                                          (assoc :href @selected-tmpl-id)
                                                          general-utils/edn->json))))}])

             [forms/resource-editor (or @selected-tmpl-id collection-name) text]]]

           [ui/ModalActions
            [uix/Button
             {:text     (@tr [:cancel])
              :on-click (fn []
                          (reset! text default-text)
                          (dispatch [::events/hide-add-modal]))}]
            [uix/Button
             {:text     (@tr [:create])
              :primary  true
              :on-click (fn []
                          (try
                            (let [data (cond->> (general-utils/json->edn @text)
                                                @selected-tmpl-id (general-utils/create-template
                                                                    @collection-name))]
                              (dispatch [::events/create-resource data]))
                            (catch :default e
                              (dispatch [::messages-events/add
                                         {:header  "invalid JSON document"
                                          :message (str "invalid JSON:\n\n" e)
                                          :type    :error}]))
                            (finally
                              (dispatch [::events/hide-add-modal]))))}]]
           ])))))

(defn SearchButton
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        loading?    (subscribe [::subs/loading?])
        selected-id (subscribe [::subs/collection-name])]
    (fn []
      [uix/MenuItemForSearch {:name     (@tr [:search])
                              :loading? @loading?
                              :disabled (nil? @selected-id)
                              :on-click #(dispatch [::events/get-results])}])))

(defn CreateButton
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/MenuItem
     {:name     (@tr [:add])
      :icon     icons/i-plus-full
      :on-click #(dispatch [::events/show-add-modal])}]))

(defn DeleteResourcesButton
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [cimi-detail-views/action-button-icon
     (@tr [:delete-resources])
     (@tr [:yes])
     "trash"
     (@tr [:delete-resources])
     (@tr [:are-you-sure?])
     #(dispatch [::events/delete-selected-rows]) (constantly nil)]))

(defn MenuBar []
  (let [tr               (subscribe [::i18n-subs/tr])
        resources        (subscribe [::subs/collection])
        selected-rows    (subscribe [::subs/selected-rows])
        can-bulk-delete? (subscribe [::subs/can-bulk-delete?])
        mobile?          (subscribe [::main-subs/is-mobile-device?])]
    (fn []
      (when (instance? js/Error @resources)
        (dispatch [::messages-events/add
                   (let [{:keys [status message]} (response/parse-ex-info @resources)]
                     {:header  (cond-> (@tr [:error])
                                       status (str " (" status ")"))
                      :message message
                      :type    :error})]))
      [ui/Segment style/basic
       [ResourceAddForm]
       [ui/Menu {:attached   "top"
                 :borderless true}
        [SearchButton]
        [SelectFields]
        (when (general-utils/can-add? @resources)
          [CreateButton])
        (when (and (not-empty @selected-rows)
                   @can-bulk-delete?)
          [DeleteResourcesButton])
        (when-not @mobile?
          [ui/MenuMenu {:position :right}
           [DocumentationButton]])]
       [ui/Segment {:attached "bottom"}
        [SearchHeader]]])))

(defn CimiResource
  []
  (dispatch [::events/get-results])
  (let [path         (subscribe [::route-subs/nav-path])
        query-params (subscribe [::route-subs/nav-query-params])]
    (fn []
      (let [[_ resource-type uuid] @path]
        (dispatch [::events/set-collection-name resource-type uuid])
        (when @query-params
          (dispatch [::events/set-query-params @query-params]))
        [ui/Segment style/basic
         (case (count @path)
           1 [MenuBar]
           2 [:<>
              [MenuBar]
              [ResultsDisplay]]
           3 [cimi-detail-views/cimi-detail @path]
           [MenuBar])]))))

(defn ApiView
  [_path]
  [CimiResource])
