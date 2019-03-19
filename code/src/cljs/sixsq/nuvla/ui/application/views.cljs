(ns sixsq.nuvla.ui.application.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.application.events :as events]
    [sixsq.nuvla.ui.application.subs :as subs]
    [sixsq.nuvla.ui.application.utils :as utils]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.history.views :as history]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.resource-details :as resource-details]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [ui/MenuMenu {:position "right"}
       [uix/MenuItemWithIcon
        {:name      (@tr [:refresh])
         :icon-name "refresh"
         :loading?  false                                   ;; FIXME: Add loading flag for module.
         :on-click  #(dispatch [::events/get-module])}]])))


(defn control-bar []
  (let [tr            (subscribe [::i18n-subs/tr])
        module        (subscribe [::subs/module])
        cep           (subscribe [::api-subs/cloud-entry-point])
        page-changed? (subscribe [::subs/page-changed?])]
    (let [add-disabled?    (not= "PROJECT" (:type @module))
          deploy-disabled? (= "PROJECT" (:type @module))]
      (vec (concat [ui/Menu {:borderless true}]

                   (resource-details/format-operations nil @module (:base-uri @cep) nil)

                   [
                    [uix/MenuItemWithIcon
                     {:name      (@tr [:launch])
                      :icon-name "rocket"
                      :disabled  deploy-disabled?
                      :on-click  #(dispatch [::deployment-dialog-events/create-deployment (:id @module) :credentials])}]

                    [uix/MenuItemWithIcon
                     {:name      (@tr [:add])
                      :icon-name "add"
                      :disabled  add-disabled?
                      :on-click  #(dispatch [::events/open-add-modal])}]

                    [uix/MenuItemWithIcon
                     {:name      (@tr [:save])
                      :icon-name "save"
                      :disabled  (not @page-changed?)
                      :on-click  #(dispatch [::events/open-save-modal])}]

                    [refresh-button]])))))


(defn form-input-callback
  [path]
  (ui-callback/value #(dispatch [::events/update-add-data path %])))


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


(defn save-action []
  (let [page-changed? (subscribe [::subs/page-changed?])
        tr            (subscribe [::i18n-subs/tr])]
    (fn []
      [ui/Button {:primary  true
                  :style    {:margin-top 10}
                  :disabled (not @page-changed?)
                  :icon     "save"
                  :content  (@tr [:save])
                  :on-click #(dispatch [::events/open-save-modal])}])))


(defn save-modal
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/save-modal-visible?])]
    (fn []
      (let []
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-save-modal])}

         [ui/ModalHeader (str/capitalize (str (@tr [:save]) " " (@tr [:component])))]

         [ui/ModalContent
          [ui/Input {:placeholder  (@tr [:commit-placeholder])
                     :fluid        true
                     :auto-focus   true
                     :on-change    (ui-callback/input-callback #(dispatch [::events/commit-message %]))
                     :on-key-press (fn [e]
                                     (when (= 13 (.-charCode e))
                                       (dispatch [::events/close-save-modal])))}]]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:save])
                       :positive true
                       :active   true
                       :on-click #(dispatch [::events/close-save-modal])}]]]))))


(defn logo-url-modal
  []
  (let [local-url (reagent/atom "")
        tr        (subscribe [::i18n-subs/tr])
        visible?  (subscribe [::subs/logo-url-modal-visible?])
        module    (subscribe [::subs/module])]
    (fn []
      (let []
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-logo-url-modal])}

         [ui/ModalHeader (@tr [:select-logo-url])]

         [ui/ModalContent
          [ui/Input {:default-value (or (:logo-url @module) "")
                     :placeholder   (@tr [:logo-url-placeholder])
                     :fluid         true
                     :auto-focus    true
                     :on-change     (ui-callback/input-callback #(reset! local-url %))
                     :on-key-press  (fn [e]
                                      (when (= 13 (.-charCode e))
                                        (dispatch [::events/save-logo-url @local-url])))}]]

         [ui/ModalActions
          [uix/Button {:text         "Ok"
                       :positive     true
                       :disabled     (empty? @local-url)
                       :active       true
                       :on-click     #(dispatch [::events/save-logo-url @local-url])
                       :on-key-press (fn [e]
                                       (if (= 13 (.-charCode e))
                                         (log/infof "Button ENTER")
                                         (log/infof "Button NOT ENTER")))}]]]))))


(defn add-modal
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/add-modal-visible?])
        nav-path (subscribe [::main-subs/nav-path])]
    (fn []
      (let [parent   (utils/nav-path->module-path @nav-path)
            hide-fn #(dispatch [::events/close-add-modal])]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

         [ui/ModalHeader [ui/Icon {:name "add"}] (@tr [:add])]

         [ui/ModalContent {:scrolling false}
          [ui/CardGroup {:centered true}

           [ui/Card {:on-click #(do (dispatch [::events/close-add-modal])
                                    (dispatch [::history-events/navigate
                                               (str/join "/"
                                                         (remove str/blank?
                                                                 ["apps" parent "New Project?type=project"]))]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Project"]
             [ui/Icon {:name "folder"
                       :size :massive
                       }]]]

           [ui/Card {:on-click (when parent
                                 #(do
                                    (dispatch [::events/close-add-modal])
                                    (dispatch [::history-events/navigate
                                               (str/join "/"
                                                         (remove str/blank?
                                                                 ["apps" parent "New Component?type=component"]))])))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Component"]
             [:div]
             [ui/Icon {:name  "th"
                       :size  :massive
                       :color (when-not parent :grey)
                       }]]]]]
         [ui/ModalActions]]))))


(defn format-module [{:keys [type name description] :as module}]
  (when module
    (let [on-click  #(dispatch [::main-events/push-breadcrumb name])
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
  [{:keys [name path description logo-url type acl] :as module-meta}]
  {:title       name
   :subtitle    path
   :description description
   :logo        logo-url
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
        rows     (metadata-rows module-meta)]
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
        (let [selected     (keyword @selected-target)
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


(defn toggle [v]
  (swap! v not))


(defn summary
  [extras]
  (let [tr               (subscribe [::i18n-subs/tr])
        module           (subscribe [::subs/module])
        default-logo-url (subscribe [::subs/default-logo-url])]
    (fn [extras]
      (let [{name        :name
             parent      :parent-path
             description :description
             logo-url    :logo-url
             type        :type
             :or         {name        ""
                          parent      ""
                          description ""
                          logo-url    @default-logo-url
                          type        "project"}} @module]
        [ui/Grid {:style {:margin-bottom 5}}
         [ui/GridRow {:reversed :computer}
          [ui/GridColumn {:computer     2
                          :large-screen 2}
           [ui/Image {:src (or logo-url @default-logo-url)}]
           [ui/Button {:fluid    true
                       :on-click #(dispatch [::events/open-logo-url-modal])}
            (@tr [:module-change-logo])]]
          [ui/GridColumn {:computer     14
                          :large-screen 14}
           ;           [:div (pr-str @(subscribe [::subs/module]))]
           [ui/Input {:name        "name"
                      :label       "name"
                      :value       name
                      :placeholder (str/capitalize (@tr [:name]))
                      :fluid       true
                      :style       {:padding-bottom 5}
                      :on-change   (ui-callback/input-callback #(do (dispatch [::events/page-changed? true])
                                                                    (dispatch [::events/name %])))
                      }]
           [ui/Input {:name        "parent"
                      :label       (if (= "PROJECT" type) "parent project" "project")
                      :value       (or parent "")           ;should not be needed, but is!!?
                      :placeholder (str/capitalize (@tr [:parent]))
                      :fluid       true
                      :style       {:padding-bottom 5}
                      :on-change   (ui-callback/input-callback #(do (dispatch [::events/page-changed? true])
                                                                    (dispatch [::events/parent %])))}]
           [ui/Input {:name        "description"
                      :label       "description"
                      :value       description
                      :placeholder (str/capitalize (@tr [:description]))
                      :fluid       true
                      :style       {:padding-bottom 5}
                      :on-change   (ui-callback/input-callback #(do (dispatch [::events/page-changed? true])
                                                                    (dispatch [::events/description %])))}]
           extras
           ]]]))))