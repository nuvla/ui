(ns sixsq.nuvla.ui.module-project.views
  (:require
    [reagent.core :as reagent]
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [sixsq.nuvla.ui.application.utils :as application-utils]
    [sixsq.nuvla.ui.application.subs :as application-subs]
    [sixsq.nuvla.ui.application.events :as application-events]
    [sixsq.nuvla.ui.cimi-api.utils :as cimi-api-utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.module-project.events :as events]
    [sixsq.nuvla.ui.module-project.subs :as subs]
    [sixsq.nuvla.ui.plot.plot :as plot]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.resource-details :as details]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.form-fields :as forms]
    [sixsq.nuvla.ui.panel :as panel]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.application.views :as application-views]))

(defn refresh-button
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        page-changed? (subscribe [::application-subs/page-changed?])]
    (fn []
      [ui/MenuMenu {:position "right"}
       [uix/MenuItemWithIcon
        {:name      (@tr [:refresh])
         :icon-name "refresh"
         :loading?  false                                   ;; FIXME: Add loading flag for module.
         :disabled  @page-changed?
         :on-click  #(dispatch [::application-events/get-module])
         }]])))


(defn summary []
  [application-views/summary])


(defn format-module
  [{:keys [type name description] :as module}]
  (when module
    (let [on-click  #(dispatch [::main-events/push-breadcrumb name])
          icon-name (application-utils/category-icon type)]
      [ui/ListItem {:on-click on-click}
       [ui/ListIcon {:name           icon-name
                     :size           "large"
                     :vertical-align "middle"}]
       [ui/ListContent
        [ui/ListHeader [:a {:on-click on-click} name]]
        [ui/ListDescription [:span description]]]])))


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


(defn toggle [v]
  (swap! v not))


(defn modules []
  (let [tr      (subscribe [::i18n-subs/tr])
        module  (subscribe [::application-subs/module])
        active? (reagent/atom true)]
    (fn []
      (if (nil? @module)
        [ui/Message {:warning true}
         [ui/Icon {:name "warning sign"}]
         (@tr [:no-children-modules])]

        [ui/Accordion {:fluid     true
                       :styled    true
                       :exclusive false
                       }
         [ui/AccordionTitle {:active   @active?
                             :index    1
                             :on-click #(toggle active?)}
          [:h2
           [ui/Icon {:name (if @active? "dropdown" "caret right")}]
           "Sub-modules"]]
         [ui/AccordionContent {:active @active?}
          [format-module-children (:children @module)]]]))))


(defn view-edit
  []
  (let [module (subscribe [::application-subs/module])]
    (fn []
      (let [new-parent (application-utils/nav-path->parent-path @(subscribe [::main-subs/nav-path]))
            new-name   (application-utils/nav-path->module-name @(subscribe [::main-subs/nav-path]))
            name       (:name @module)
            parent     (:parent-path @module)]
        (when (nil? @module)
          (dispatch [::application-events/name new-name])
          (dispatch [::application-events/parent new-parent]))
        [ui/Container {:fluid true}
         [:h2 [ui/Icon {:name "folder"}]
          parent (when (not-empty parent) "/") name]
         [application-views/control-bar]
         [summary]
         [application-views/save-action]
         [:div {:style {:padding-top 10}}]
         [modules]
         [application-views/add-modal]
         [application-views/save-modal]
         [application-views/logo-url-modal]
         ]))))
