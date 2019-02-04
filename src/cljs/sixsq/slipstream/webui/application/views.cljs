(ns sixsq.slipstream.webui.application.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.slipstream.webui.application.events :as application-events]
    [sixsq.slipstream.webui.application.subs :as application-subs]
    [sixsq.slipstream.webui.application.utils :as utils]
    [sixsq.slipstream.webui.cimi.subs :as api-subs]
    [sixsq.slipstream.webui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.slipstream.webui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.slipstream.webui.history.views :as history]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.main.events :as main-events]
    [sixsq.slipstream.webui.main.subs :as main-subs]
    [sixsq.slipstream.webui.panel :as panel]
    [sixsq.slipstream.webui.utils.collapsible-card :as cc]
    [sixsq.slipstream.webui.utils.resource-details :as resource-details]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [sixsq.slipstream.webui.utils.style :as style]
    [sixsq.slipstream.webui.utils.ui-callback :as ui-callback]))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [ui/MenuMenu {:position "right"}
       [uix/MenuItemWithIcon
        {:name      (@tr [:refresh])
         :icon-name "refresh"
         :loading?  false                                   ;; FIXME: Add loading flag for module.
         :on-click  #(dispatch [::application-events/get-module])}]])))


(defn control-bar []
  (let [tr (subscribe [::i18n-subs/tr])
        module (subscribe [::application-subs/module])
        cep (subscribe [::api-subs/cloud-entry-point])]
    (let [add-disabled? (not= "PROJECT" (:type @module))
          deploy-disabled? (= "PROJECT" (:type @module))]
      (vec (concat [ui/Menu {:borderless true}]

                   (resource-details/format-operations nil @module (:baseURI @cep) nil)

                   [[uix/MenuItemWithIcon
                     {:name      (@tr [:launch])
                      :icon-name "rocket"
                      :disabled  deploy-disabled?
                      :on-click  #(dispatch [::deployment-dialog-events/create-deployment (:id @module) :credentials])}]

                    [uix/MenuItemWithIcon
                     {:name      (@tr [:add])
                      :icon-name "add"
                      :disabled  add-disabled?
                      :on-click  #(dispatch [::application-events/open-add-modal])}]
                    [refresh-button]])))))


(defn form-input-callback
  [path]
  (ui-callback/value #(dispatch [::application-events/update-add-data path %])))


(defn project-pane
  []
  (let [add-data (subscribe [::application-subs/add-data])]
    (let [{{:keys [name description] :as project-data} :project} @add-data]
      ^{:key "project-pane"}
      [ui/TabPane
       [ui/Form {:id "add-project"}
        [ui/FormInput {:label     "name"
                       :value     (or name "")
                       :on-change (form-input-callback [:project :name])}]
        [ui/FormInput {:label     "description"
                       :value     (or description "")
                       :on-change (form-input-callback [:project :description])}]]])))


(defn image-pane
  []
  (let [add-data (subscribe [::application-subs/add-data])]
    (let [{{:keys [name
                   description
                   connector
                   image-id
                   author
                   loginUser
                   networkType
                   os] :as image-data} :image} @add-data]
      ^{:key "image-pane"}
      [ui/TabPane
       [ui/Form {:id "add-image"}
        [ui/FormInput {:label     "name"
                       :value     (or name "")
                       :on-change (form-input-callback [:image :name])}]
        [ui/FormInput {:label     "description"
                       :value     (or description "")
                       :on-change (form-input-callback [:image :description])}]
        [ui/FormInput {:label     "connector"
                       :value     (or connector "")
                       :on-change (form-input-callback [:image :connector])}]
        [ui/FormInput {:label     "image ID"
                       :value     (or image-id "")
                       :on-change (form-input-callback [:image :image-id])}]
        [ui/FormInput {:label     "author"
                       :value     (or author "")
                       :on-change (form-input-callback [:image :author])}]
        [ui/FormInput {:label     "loginUser"
                       :value     (or loginUser "")
                       :on-change (form-input-callback [:image :loginUser])}]
        [ui/FormInput {:label     "networkType"
                       :value     (or networkType "")
                       :on-change (form-input-callback [:image :networkType])}]
        [ui/FormInput {:label     "os"
                       :value     (or os "")
                       :on-change (form-input-callback [:image :os])}]
        ]])))


(defn component-pane
  []
  ^{:key "component-pane"}
  [ui/TabPane
   [ui/Form {:id "add-component"}
    [ui/FormInput {:label "name"}]
    [ui/FormInput {:label "description"}]]])


(defn application-pane
  []
  ^{:key "application-pane"}
  [ui/TabPane
   [ui/Form {:id "add-application"}
    [ui/FormInput {:label "name"}]
    [ui/FormInput {:label "description"}]]])


(defn kw->icon-name
  [kw]
  (-> kw name str/upper-case utils/category-icon))


(defn pane
  [tr kw element]
  {:menuItem {:key     (name kw)
              :icon    (kw->icon-name kw)
              :content (@tr [kw])}
   :render   (fn [] (reagent/as-element [element]))})


(defn index->kw
  [index]
  (case index
    0 :project
    1 :image
    2 :component
    3 :application
    :project))


(defn add-modal
  []
  (let [tr (subscribe [::i18n-subs/tr])
        visible? (subscribe [::application-subs/add-modal-visible?])
        nav-path (subscribe [::main-subs/nav-path])]
    (let [hide-fn #(dispatch [::application-events/close-add-modal])
          submit-fn #(dispatch [::application-events/add-module])]
      [ui/Modal {:open       @visible?
                 :close-icon true
                 :on-close   hide-fn}

       [ui/ModalHeader [ui/Icon {:name "add"}] (@tr [:add])]

       [ui/ModalContent {:scrolling true}
        [ui/Header {:as "h3"} (utils/nav-path->module-path @nav-path)]
        [ui/Tab
         {:panes         [(pane tr :project project-pane)
                          (pane tr :image image-pane)
                          #_(pane tr :component component-pane)
                          #_(pane tr :application application-pane)]
          :on-tab-change (ui-callback/callback :activeIndex
                                               (fn [index]
                                                 (let [kw (index->kw index)]
                                                   (dispatch [::application-events/set-active-tab kw]))))}]]

       [ui/ModalActions
        [uix/Button {:text (@tr [:cancel]), :on-click hide-fn}]
        [uix/Button {:text (@tr [:add]), :positive true, :on-click submit-fn}]]])))


(defn format-module [{:keys [type name description] :as module}]
  (when module
    (let [on-click #(dispatch [::main-events/push-breadcrumb name])
          icon-name (utils/category-icon type)]
      [ui/ListItem {:on-click on-click}
       [ui/ListIcon {:name           icon-name
                     :size           "large"
                     :vertical-align "middle"}]
       [ui/ListContent
        [ui/ListHeader [:a {:on-click on-click} name]]
        [ui/ListDescription [:span description]]]])))


(defn tuple-to-row [[v1 v2]]
  [ui/TableRow
   [ui/TableCell {:collapsing true} (str v1)]
   [ui/TableCell (str v2)]])


(defn preprocess-metadata
  [{:keys [name path description logoURL type acl] :as module-meta}]
  {:title       name
   :subtitle    path
   :description description
   :logo        logoURL
   :icon        (utils/meta-category-icon type)
   :acl         acl})


(defn metadata-rows
  [module-meta]
  (->> (dissoc module-meta :versions :children :acl :operations)
       (map (juxt (comp name first) (comp str second)))
       (map tuple-to-row)))


(defn format-meta
  [module-meta]
  (let [metadata (preprocess-metadata module-meta)
        rows (metadata-rows module-meta)]
    [cc/metadata metadata rows]))


(defn error-text [tr error]
  (if-let [{:keys [status reason]} (ex-data error)]
    (str (or (@tr [reason]) (name reason)) " (" status ")")
    (str error)))


(defn format-error
  [error]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [error]
      (when (instance? js/Error error)
        [ui/Container
         [ui/Header {:as "h3", :icon true}
          [ui/Icon {:name "warning sign"}]
          (error-text tr error)]]))))


(defn format-module-children
  [module-children]
  (when (pos? (count module-children))
    [ui/Segment style/basic
     (vec (concat [ui/ListSA {:divided   true
                              :relaxed   true
                              :selection true}]
                  (map (fn [{:keys [id] :as module}]
                         ^{:key id}
                         [format-module module]) module-children)))]))


(defn parameter-table-row
  [[name description value]]
  [ui/TableRow
   [ui/TableCell {:collapsing true} name]
   [ui/TableCell description]
   [ui/TableCell value]])


(defn format-parameters
  [title-kw parameters]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [title-kw parameters]
      (when parameters
        (let [rows (mapv (juxt :parameter :description :value) parameters)]
          [cc/collapsible-segment
           (@tr [title-kw])
           [ui/Table style/definition
            (vec (concat [ui/TableBody] (map parameter-table-row rows)))]])))))


(defn target-dropdown
  [state]
  [ui/Dropdown {:inline        true
                :default-value :deployment
                :on-change     (ui-callback/value #(reset! state %))
                :options       [{:key "preinstall", :value "preinstall", :text "pre-install"}
                                {:key "packages", :value "packages", :text "packages"}
                                {:key "postinstall", :value "postinstall", :text "post-install"}
                                {:key "deployment", :value "deployment", :text "deployment"}
                                {:key "reporting", :value "reporting", :text "reporting"}
                                {:key "onVmAdd", :value "onVmAdd", :text "on VM add"}
                                {:key "onVmRemove", :value "onVmRemove", :text "on VM remove"}
                                {:key "prescale", :value "prescale", :text "pre-scale"}
                                {:key "postscale", :value "postscale", :text "post-scale"}]}])


(defn render-package
  [package]
  ^{:key package}
  [ui/ListItem
   [ui/ListContent
    [ui/ListHeader package]]])


(defn render-packages
  [packages]
  (if (empty? packages)
    [:span "no packages defined"]
    (vec (concat [ui/ListSA] (mapv render-package packages)))))


(defn render-script
  [script]
  (if (str/blank? script)
    [:span "undefined"]
    [ui/CodeMirror {:value   script
                    :options {:line-numbers true
                              :read-only    true}}]))


(defn format-targets
  [targets]
  (let [selected-target (reagent/atom "deployment")]
    (fn [targets]
      (when targets
        (let [selected (keyword @selected-target)
              target-value (get targets selected)]
          [cc/collapsible-segment
           [:span [target-dropdown selected-target] "target"]
           [ui/Segment
            (if (= :packages selected)
              (render-packages target-value)
              (render-script target-value))]])))))


(defn format-component-link
  [label href]
  [history/link (str "api/" href) label])


(defn render-parameter-mapping
  [{:keys [parameter value mapped]}]
  (let [label (str parameter
                   (if mapped " \u2192 " " \uff1d ")
                   (or value "empty"))]
    ^{:key parameter}
    [ui/ListItem
     [ui/ListContent
      [ui/ListHeader label]]]))


(defn render-parameter-mappings
  [parameter-mappings]
  (if (empty? parameter-mappings)
    [:span "none"]
    (vec (concat [ui/ListSA] (mapv render-parameter-mapping (sort-by :parameter parameter-mappings))))))


(defn render-node
  [{:keys [node multiplicity component parameterMappings] :as content}]
  (let [label (name node)]
    [cc/collapsible-segment
     [:span label]
     [ui/Table style/definition
      [ui/TableBody
       [ui/TableRow
        [ui/TableCell {:collapsing true} "component"]
        [ui/TableCell (format-component-link label (:href component))]]
       [ui/TableRow
        [ui/TableCell {:collapsing true} "multiplicity"]
        [ui/TableCell multiplicity]]
       [ui/TableRow
        [ui/TableCell {:collapsing true} "parameterMappings"]
        [ui/TableCell (render-parameter-mappings parameterMappings)]]]]]))


(defn format-nodes
  [nodes]
  (let [sorted-nodes (sort-by :node nodes)]
    (vec (concat [ui/Segment] (mapv render-node sorted-nodes)))))


(defn module-resource []
  (let [data (subscribe [::application-subs/module])]
    (fn []
      (vec (concat [ui/Container {:fluid true}
                    [control-bar]
                    [add-modal]
                    [deployment-dialog-views/deploy-modal false]
                    [format-error @data]]
                   (when (and @data (not (instance? js/Error @data)))
                     (let [{:keys [children content]} @data
                           metadata (dissoc @data :content)
                           {:keys [targets nodes inputParameters outputParameters]} content
                           type (:type metadata)]
                       [[format-meta metadata]
                        (when (= type "COMPONENT") [format-parameters :input-parameters inputParameters])
                        (when (= type "COMPONENT") [format-parameters :output-parameters outputParameters])
                        (when (= type "COMPONENT") [format-targets targets])
                        (when (= type "APPLICATION") [format-nodes nodes])
                        [format-module-children children]])))))))


(defmethod panel/render :application
  [path]
  (dispatch [::application-events/get-module])
  [module-resource])
