(ns sixsq.nuvla.ui.cimi.views
  (:require
    [cljs.pprint :refer [cl-format pprint]]
    [clojure.set :as set]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.cimi-api.utils :as cimi-api-utils]
    [sixsq.nuvla.ui.cimi-detail.views :as cimi-detail-views]
    [sixsq.nuvla.ui.cimi.events :as events]
    [sixsq.nuvla.ui.cimi.subs :as subs]
    [sixsq.nuvla.ui.cimi.utils :as cimi-utils]
    [sixsq.nuvla.ui.docs.subs :as docs-subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.views :as history]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.forms :as forms]
    [sixsq.nuvla.ui.utils.general :as general]
    [sixsq.nuvla.ui.utils.response :as response]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


(defn id-selector-formatter [entry]
  (let [v     (:id entry)
        label (second (str/split v #"/"))]
    [history/link (str "api/" v) label]))


;; FIXME: Provide better visualization of non-string values.
(defn field-selector
  [field]
  (let [ks (map keyword (str/split field #"/"))]
    (fn [m]
      (str (get-in m ks)))))


(defn remove-column-fn
  [label]
  (fn []
    (dispatch [::events/remove-field label])))


(defn table-header-cell
  [label]
  ^{:key label} [ui/TableHeaderCell
                 [:a {:on-click (remove-column-fn label)} [ui/Icon {:name "remove circle"}]]
                 "\u00a0" label])


(defn results-table-header [selected-fields]
  [ui/TableHeader
   (vec (concat [ui/TableRow]
                (mapv table-header-cell selected-fields)))])


(defn results-table-row-fn [selected-fields]
  (apply juxt (map (fn [selected-field] (if (= "id" selected-field)
                                          id-selector-formatter
                                          (field-selector selected-field)))
                   selected-fields)))


(defn results-table-row [row-fn entry]
  (when entry
    (let [data (row-fn entry)]
      (vec (concat [ui/TableRow]
                   (mapv (fn [v] [ui/TableCell v]) data))))))


(defn results-table-body [row-fn entries]
  (vec (concat [ui/TableBody]
               (mapv (partial results-table-row row-fn) entries))))


(defn results-table [selected-fields entries]
  (when (pos? (count entries))
    (let [row-fn (results-table-row-fn selected-fields)]
      [:div {:class-name "nuvla-ui-x-autoscroll"}
       [ui/Table
        {:collapsing  true
         :compact     "very"
         :unstackable true
         :single-line true
         :padded      false}
        (results-table-header selected-fields)
        (results-table-body row-fn entries)]])))


(defn statistic
  [[value label :as data]]
  (when data
    ^{:key label}
    [ui/Statistic {:size "tiny"}
     (if (int? value)
       [ui/StatisticValue (cl-format nil "~D" value)]
       [ui/StatisticValue (cl-format nil "~,2F" value)])
     [ui/StatisticLabel label]]))


(defn results-statistic
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        resources (subscribe [::subs/collection])]
    (fn []
      (let [resources @resources]
        (when resources
          (let [total (:count resources)
                n     (count (get resources :resources []))]
            [ui/Statistic
             [ui/StatisticValue (str n " / " total)]
             [ui/StatisticLabel (@tr [:results])]]))))))


(def tuple-fn (juxt (comp :value second)
                    (comp name first)))


(defn aggregations-table
  []
  (let [aggregations (subscribe [::subs/aggregations])]
    (fn []
      (let [stats (->> @aggregations
                       (map tuple-fn)
                       (sort second)
                       (map statistic)
                       vec)]
        [ui/Segment style/basic
         (vec (concat [ui/StatisticGroup {:size "tiny"}] [[results-statistic]] stats))]))))


(defn results-display []
  (let [collection      (subscribe [::subs/collection])
        selected-fields (subscribe [::subs/selected-fields])]
    (fn []
      (let [results @collection]
        (if (instance? js/Error results)
          [ui/Segment style/basic
           [:pre (with-out-str (pprint (ex-data results)))]]
          (let [entries (get results :resources [])]
            [ui/Segment style/basic
             [aggregations-table]
             [results-table @selected-fields entries]]))))))


(defn cloud-entry-point-title
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        cep         (subscribe [::subs/cloud-entry-point])
        selected-id (subscribe [::subs/collection-name])]
    (fn []
      (let [options  (->> @cep
                          :collection-href
                          vals
                          sort
                          (map (fn [k] {:value k :text k}))
                          vec)
            callback #(dispatch [::history-events/navigate (str "api/" %)])]
        [ui/Dropdown
         {:aria-label  (@tr [:resource-type])
          :value       @selected-id
          :placeholder (@tr [:resource-type])
          :tab-index   1
          :scrolling   true
          :search      true
          :selection   true
          :upward      false
          :options     options
          :on-change   (ui-callback/value callback)}]))))


(defn DocumentationButton
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        mobile?   (subscribe [::main-subs/is-device? :mobile])
        on-button (r/atom false)]
    (fn []
      [ui/Button (cond-> {:icon           "info"
                          :basic          true
                          :color          "blue"
                          :content        (@tr [:api-doc])
                          ;; this is a workaround misbehavior when in an input of form and enter key,
                          ;; for unknown reason the on-click fn of this button is called
                          :on-mouse-enter #(reset! on-button true)
                          :on-mouse-leave #(reset! on-button false)
                          :on-click       #(when @on-button
                                             (dispatch [::history-events/navigate "documentation"]))}
                         (not @mobile?) (assoc :floated "right"))])))


(defn search-header []
  (let [tr           (subscribe [::i18n-subs/tr])
        query-params (subscribe [::subs/query-params])
        selected-id  (subscribe [::subs/collection-name])]
    (fn []
      ;; reset visible values of parameters
      (let [{$filter      :filter,
             $first       :first,
             $last        :last,
             $select      :select,
             $aggregation :aggregation,
             $orderby     :orderby} @query-params]
        [ui/Form {:aria-label   "filter parameters"
                  :on-key-press (partial forms/on-return-key
                                         #(when @selected-id
                                            (dispatch [::events/get-results])))}

         [DocumentationButton]

         [ui/FormGroup
          [ui/FormField
           [cloud-entry-point-title]]]
         [ui/FormGroup {:widths "equal"}
          [ui/FormField
           ; the key below is a workaround react issue with controlled input cursor position,
           ; this will force to re-render defaultValue on change of the value
           ^{:key (str "first:" $first)}
           [ui/Input {:aria-label   (@tr [:first])
                      :tab-index    2
                      :type         "number"
                      :min          0
                      :label        (@tr [:first])
                      :defaultValue $first
                      :on-blur      (ui-callback/input ::events/set-first)}]]

          [ui/FormField
           ^{:key (str "last:" $last)}
           [ui/Input {:aria-label   (@tr [:last])
                      :tab-index    3
                      :type         "number"
                      :min          0
                      :label        (@tr [:last])
                      :defaultValue $last
                      :on-blur      (ui-callback/input ::events/set-last)}]]

          [ui/FormField
           ^{:key (str "select:" $select)}
           [ui/Input {:aria-label   (@tr [:select])
                      :tab-index    4
                      :type         "text"
                      :label        (@tr [:select])
                      :defaultValue $select
                      :placeholder  "e.g. id, endpoint, ..."
                      :on-blur      (ui-callback/input ::events/set-select)}]]]

         [ui/FormGroup {:widths "equal"}
          [ui/FormField
           ^{:key (str "orderby:" $orderby)}
           [ui/Input {:aria-label   (@tr [:order])
                      :tab-index    5
                      :type         "text"
                      :label        (@tr [:order])
                      :defaultValue $orderby
                      :placeholder  "e.g. created:desc, ..."
                      :on-blur      (ui-callback/input ::events/set-orderby)}]]

          [ui/FormField
           ^{:key (str "aggregation:" $aggregation)}
           [ui/Input {:aria-label   (@tr [:aggregation])
                      :tab-index    6
                      :type         "text"
                      :label        (@tr [:aggregation])
                      :defaultValue $aggregation
                      :placeholder  "e.g. min:resource:vcpu, ..."
                      :on-blur      (ui-callback/input ::events/set-aggregation)}]]]

         [ui/FormGroup {:widths "equal"}
          [ui/FormField
           ^{:key (str "filter:" $filter)}
           [ui/Input
            {:aria-label   (@tr [:filter])
             :tab-index    7
             :type         "text"
             :label        (@tr [:filter])
             :defaultValue $filter
             :placeholder  "e.g. connector/href^='exoscale-' and resource:type='VM' and resource:ram>=8096"
             :on-blur      (ui-callback/input ::events/set-filter)}]]]]))))


(defn format-field-item [selections-atom item]
  [ui/ListItem
   [ui/ListContent
    [ui/ListHeader
     [ui/Checkbox {:default-checked (contains? @selections-atom item)
                   :label           item
                   :on-change       (ui-callback/checked (fn [checked]
                                                           (if checked
                                                             (swap! selections-atom set/union #{item})
                                                             (swap! selections-atom set/difference #{item}))))}]]]])


(defn format-field-list [available-fields-atom selections-atom]
  (let [items (sort @available-fields-atom)]
    (vec (concat [ui/ListSA]
                 (map (partial format-field-item selections-atom) items)))))


(defn select-fields []
  (let [tr               (subscribe [::i18n-subs/tr])
        available-fields (subscribe [::subs/available-fields])
        selected-fields  (subscribe [::subs/selected-fields])
        selected-id      (subscribe [::subs/collection-name])
        selections       (r/atom (set @selected-fields))
        show?            (r/atom false)]
    (fn []
      [ui/Modal
       {:closeIcon true
        :open      @show?
        :on-close  #(reset! show? false)
        :trigger   (r/as-element
                     [uix/MenuItemWithIcon
                      {:name      (@tr [:columns])
                       :icon-name "columns"
                       :disabled  (nil? @selected-id)
                       :on-click  (fn []
                                    (reset! selections (set @selected-fields))
                                    (reset! show? true))}])}
       [ui/ModalHeader (@tr [:fields])]
       [ui/ModalContent
        {:scrolling true}
        (format-field-list available-fields selections)]
       [ui/ModalActions
        [uix/Button
         {:text     (@tr [:cancel])
          :on-click #(reset! show? false)}]
        [uix/Button
         {:text     (@tr [:update])
          :primary  true
          :on-click (fn []
                      (reset! show? false)
                      (dispatch [::events/set-selected-fields @selections]))}]]])))


(defn resource-add-form
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        show?            (subscribe [::subs/show-add-modal?])
        collection-name  (subscribe [::subs/collection-name])
        default-text     (general/edn->json {})
        text             (atom default-text)
        collection       (subscribe [::subs/collection])
        selected-tmpl-id (r/atom nil)]
    (fn []
      (let [resource-metadata           (subscribe [::docs-subs/document @collection])
            collection-template-href    (some-> @collection-name cimi-utils/collection-template-href)
            templates-info              (subscribe [::subs/collection-templates collection-template-href])
            selected-tmpl-resource-meta (subscribe [::docs-subs/document (get @templates-info @selected-tmpl-id)])]
        (when @show?
          [ui/Modal
           {:size    "large", :closeIcon true, :open @show?,
            :onClose #(dispatch [::events/hide-add-modal])}

           [ui/ModalContent
            [:div

             (when @templates-info
               [ui/Dropdown {:selection   true
                             :placeholder "select a resource template"
                             :value       @selected-tmpl-id
                             :options     (forms/descriptions->options (vals @templates-info))
                             :on-change   (ui-callback/value
                                            (fn [value]
                                              (reset! selected-tmpl-id value)
                                              (reset! text
                                                      (-> @templates-info
                                                          (get value)
                                                          cimi-api-utils/remove-common-attrs
                                                          (assoc :href @selected-tmpl-id)
                                                          general/edn->json))))}])

             [:br]
             [:br]

             [forms/resource-editor (or @selected-tmpl-id collection-name) text
              :resource-meta (if @templates-info
                               @selected-tmpl-resource-meta
                               @resource-metadata)]]]

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
                            (let [data (cond->> (general/json->edn @text)
                                                @selected-tmpl-id (cimi-api-utils/create-template @collection-name))]
                              (dispatch [::events/create-resource data]))
                            (catch :default e
                              (dispatch [::messages-events/add
                                         {:header  "invalid JSON document"
                                          :message (str "invalid JSON:\n\n" e)
                                          :type    :error}]))
                            (finally
                              (dispatch [::events/hide-add-modal]))))}]]
           ])))))


(defn can-add?
  [ops]
  (->> ops
       (map :rel)
       (filter #(= "add" %))
       not-empty))


(defn search-button
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        loading?    (subscribe [::subs/loading?])
        selected-id (subscribe [::subs/collection-name])]
    (fn []
      [uix/MenuItemForSearch {:name     (@tr [:search])
                              :loading? @loading?
                              :disabled (nil? @selected-id)
                              :on-click #(dispatch [::events/get-results])}])))


(defn create-button
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        search-results (subscribe [::subs/collection])]
    (fn []
      (when (can-add? (:operations @search-results))
        [uix/MenuItemWithIcon
         {:name      (@tr [:add])
          :icon-name "add"
          :on-click  #(dispatch [::events/show-add-modal])}]))))


(defn menu-bar []
  (let [tr        (subscribe [::i18n-subs/tr])
        resources (subscribe [::subs/collection])]
    (fn []
      (when (instance? js/Error @resources)
        (dispatch [::messages-events/add
                   (let [{:keys [status message]} (response/parse-ex-info @resources)]
                     {:header  (cond-> (@tr [:error])
                                       status (str " (" status ")"))
                      :message message
                      :type    :error})]))
      [ui/Segment style/basic
       [resource-add-form]
       [ui/Menu {:attached   "top"
                 :borderless true}
        [search-button]
        [select-fields]
        (when (can-add? (:operations @resources))
          [create-button])]
       [ui/Segment {:attached "bottom"}
        [search-header]]])))


(defn cimi-resource
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        path         (subscribe [::main-subs/nav-path])
        query-params (subscribe [::main-subs/nav-query-params])]
    (fn []
      (let [[_ resource-type resource-id] @path]
        (dispatch [::events/set-collection-name resource-type])
        (when @query-params
          (dispatch [::events/set-query-params @query-params])))
      (let [n        (count @path)
            children (case n
                       1 [menu-bar]
                       2 [:<>
                          [menu-bar]
                          [results-display]]
                       3 [cimi-detail-views/cimi-detail]
                       [menu-bar])
            header   [:h2 [ui/Icon {:name "code"}]
                      " "
                      (str/upper-case (@tr [:api]))]]
        [:div
         header
         children]))))


(defmethod panel/render :api
  [path]
  [cimi-resource])
