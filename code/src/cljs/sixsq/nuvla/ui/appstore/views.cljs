(ns sixsq.nuvla.ui.appstore.views
  (:require
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [sixsq.nuvla.ui.application.views :as application-views]
    [sixsq.nuvla.ui.application.events :as application-events]
    [sixsq.nuvla.ui.application.subs :as application-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.application.utils :as application-utils]
    [sixsq.nuvla.ui.module-project.events :as module-project-events]
    [sixsq.nuvla.ui.module-project.views :as module-project-views]
    [sixsq.nuvla.ui.module-component.events :as module-component-events]
    [sixsq.nuvla.ui.module-component.views :as module-component-views]
    [sixsq.nuvla.ui.appstore.events :as events]
    [sixsq.nuvla.ui.appstore.subs :as subs]
    [sixsq.nuvla.ui.appstore.utils :as utils]
    [sixsq.nuvla.ui.deployment-detail.utils :as deployment-detail-utils]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [reagent.core :as reagent]
    [taoensso.timbre :as log]
    [taoensso.timbre :as timbre]
    [cemerick.url :as url]))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :position "right"
        :on-click  #(dispatch [::events/get-modules])}])))


(defn format-deployment-template
  [{:keys [id name description module] :as deployment-template}]
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [type parentPath logo-url]} module]
    ^{:key id}
    [ui/Card
     (when logo-url
       [ui/Image {:src   logo-url
                  :style {:width      "auto"
                          :height     "100px"
                          :object-fit "contain"}}])
     [ui/CardContent
      [ui/CardHeader {:style {:word-wrap "break-word"}}
       [ui/Icon {:name (deployment-detail-utils/category-icon type)}]
       (or name id)]
      [ui/CardMeta {:style {:word-wrap "break-word"}} parentPath]
      [ui/CardDescription {:style {:overflow "hidden" :max-height "100px"}} description]]
     [ui/Button {:fluid    true
                 :primary  true
                 :on-click #(dispatch [::events/create-deployment id "credentials"])}
      (@tr [:launch])]]))


(defn modules-cards-group
  [modules-list]
  [ui/Segment style/basic
   (vec (concat [ui/CardGroup {:centered true}]
                (map (fn [deployment-template]
                       [format-deployment-template deployment-template])
                     modules-list)))])


(defn toggle [v]
  (swap! v not))


(defn appstore-search []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Input {:placeholder (@tr [:search])
               :icon        "search"
               :on-change   (ui-callback/input-callback #(dispatch [::events/set-full-text-search %]))}]))


(defn control-bar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu {:secondary true}
     [ui/MenuMenu {:position "left"}
      [appstore-search]]
     [ui/MenuMenu {:position "right"}
      [refresh-button]]]))


(defn control-bar-projects []
  (let [tr     (subscribe [::i18n-subs/tr])
        module (subscribe [::subs/module])
        ]
    (let []
      (vec (concat [ui/Menu {:borderless true}

                    [uix/MenuItemWithIcon
                     {:name      (@tr [:add])
                      :icon-name "add"
                      :on-click  #(dispatch [::application-events/open-add-modal])}]

                    [refresh-button]])))))


(defn root-projects []
  (let [tr      (subscribe [::i18n-subs/tr])
        data    (subscribe [::application-subs/module])
        active? (reagent/atom true)]
    (fn []
      (let []
        [ui/Container {:fluid true}
         [application-views/add-modal]
         [deployment-dialog-views/deploy-modal false]
         [application-views/format-error @data]
         [ui/Accordion {:fluid     true
                        :styled    true
                        :exclusive false}
          [ui/AccordionTitle {:active   @active?
                              :index    1
                              :on-click #(toggle active?)}
           [:h2
            [ui/Icon {:name (if @active? "dropdown" "caret right")}]
            (sixsq.nuvla.ui.utils.general/capitalize-words (@tr [:all-projects]))]]
          [ui/AccordionContent {:active @active?}
           [control-bar-projects]
           (when (and @data (not (instance? js/Error @data)))
             (let [{:keys [children content]} @data
                   metadata (dissoc @data :content)
                   {:keys [targets nodes inputParameters outputParameters]} content
                   type     (:type metadata)]
               [application-views/format-meta metadata]
               ; TODO... needed?
               (when (= type "COMPONENT") [application-views/format-parameters :input-parameters inputParameters])
               (when (= type "COMPONENT") [application-views/format-parameters :output-parameters outputParameters])
               (when (= type "COMPONENT") [application-views/format-targets targets])
               (when (= type "APPLICATION") [application-views/format-nodes nodes])
               [application-views/format-module-children children]))]]]))))


(defn appstore
  []
  (let [modules           (subscribe [::subs/modules])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        active?           (reagent/atom true)]
    (fn []
      (let [total-modules (get @modules :count 0)
            total-pages   (general-utils/total-pages total-modules @elements-per-page)]
        [ui/Container {:fluid true}
         [ui/Accordion {:fluid     true
                        :styled    true
                        :exclusive false
                        }
          [ui/AccordionTitle {:active   @active?
                              :index    1
                              :on-click #(toggle active?)}
           [:h2
            [ui/Icon {:name (if @active? "dropdown" "caret right")}]
            "App Store"]]
          [ui/AccordionContent {:active @active?}
           [control-bar]
           [modules-cards-group (get @modules :resources [])]
           (when (> total-pages 1)
             [:div {:style {:padding-bottom 30}}
              [uix/Pagination
               {:totalitems   total-modules
                :totalPages   total-pages
                :activePage   @page
                :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}]]
             )]]]))))

(defn module-details
  [new-type]
  (let [module     (subscribe [::application-subs/module])
        new-parent (application-utils/nav-path->parent-path @(subscribe [::main-subs/nav-path]))
        new-name   (application-utils/nav-path->module-name @(subscribe [::main-subs/nav-path]))]
    (fn [new-type]
      (let [type (:type @module)]
        (when (nil? @module)
          (do
            (dispatch [::application-events/name new-name])
            (dispatch [::application-events/parent new-parent])
            ))
        (if (or (= "component" new-type) (= "COMPONENT" type))
          [module-component-views/view-edit module]
          [module-project-views/view-edit module])
        ))))

(defn module
  [new-type]
  (dispatch [::application-events/get-module])
  [module-details new-type])

(defn root-view
  []
  (dispatch [::events/get-modules])
  (dispatch [::application-events/get-module])
  [ui/Container {:fluid true}
   [:div {:style {:margin-top 10}}]
   [appstore]
   [:div {:style {:margin-top 10}}]
   [root-projects]])

(defn apps
  []
  (let [query       (clojure.walk/keywordize-keys (:query (url/url (-> js/window .-location .-href))))
        type        (:type query)
        module-name (application-utils/nav-path->module-name @(subscribe [::main-subs/nav-path]))]
    [deployment-dialog-views/deploy-modal false]
    (if module-name
      (module type)
      [root-view])))

(defmethod panel/render :apps
  [path]
  (timbre/set-level! :info)
  [apps])
