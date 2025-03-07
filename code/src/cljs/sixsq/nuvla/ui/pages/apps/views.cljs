(ns sixsq.nuvla.ui.pages.apps.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.deployment-dialog.views :as deployment-dialog-views]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.pages.apps.apps-application.events :as apps-application-events]
            [sixsq.nuvla.ui.pages.apps.apps-application.views :as apps-application-views]
            [sixsq.nuvla.ui.pages.apps.apps-applications-sets.events :as apps-applications-sets-events]
            [sixsq.nuvla.ui.pages.apps.apps-applications-sets.views :as apps-applications-sets-views]
            [sixsq.nuvla.ui.pages.apps.apps-component.events :as apps-component-events]
            [sixsq.nuvla.ui.pages.apps.apps-component.views :as apps-component-views]
            [sixsq.nuvla.ui.pages.apps.apps-project.events :as apps-project-events]
            [sixsq.nuvla.ui.pages.apps.apps-project.views :as apps-project-views]
            [sixsq.nuvla.ui.pages.apps.apps-store.views :as apps-store-views]
            [sixsq.nuvla.ui.pages.apps.events :as events]
            [sixsq.nuvla.ui.pages.apps.subs :as subs]
            [sixsq.nuvla.ui.pages.apps.utils :as utils]
            [sixsq.nuvla.ui.pages.apps.views-detail :as views-detail]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.utils.validation :as utils-validation]))


(defn dispatch-clear-events
  [new-subtype]
  (let [nav-path   (subscribe [::route-subs/nav-path])
        new-parent (utils/nav-path->parent-path @nav-path)
        new-name   (utils/nav-path->module-name @nav-path)]
    (dispatch [::events/clear-module new-name new-parent new-subtype])
    (dispatch [::apps-component-events/clear-apps-component])
    (dispatch [::apps-application-events/clear-apps-application])
    (dispatch [::apps-project-events/clear-apps-project])
    (dispatch [::apps-applications-sets-events/clear-apps-applications-sets])))


(defn ModuleDetails
  []
  (let [module            (subscribe [::subs/module])
        module-content-id (subscribe [::subs/module-content-id])
        new-subtype       (subscribe [::route-subs/query-param :subtype])]
    (fn []
      (let [subtype (or (:subtype @module) @new-subtype)
            comp-fn (cond
                      (#{utils/subtype-application
                         utils/subtype-application-k8s
                         utils/subtype-application-helm} subtype) apps-application-views/ViewEdit
                      (utils/component? subtype) apps-component-views/view-edit
                      (= utils/subtype-applications-sets subtype) apps-applications-sets-views/ViewEdit
                      :else apps-project-views/ViewEdit)]
        ^{:key (or @module-content-id subtype)}
        [comp-fn]))))


(defn Module
  []
  (dispatch [::main-events/changes-protection? false])
  (dispatch [::events/form-valid true])
  (dispatch [::events/set-validate-form? false])
  [components/LoadingPage {:dimmable? true}
   [:<>
    [components/NotFoundPortal
     ::subs/module-not-found?
     :no-module-message-header
     :no-module-message-content]
    [views-detail/VersionWarning]
    [ModuleDetails]]])

(defn- CommonComponents
  []
  [:<>
   [utils-validation/validation-error-message ::subs/form-valid?]
   [views-detail/AddModal]
   [views-detail/save-modal]
   [views-detail/logo-url-modal]
   [deployment-dialog-views/deploy-modal]])

(defn AppDetails
  []
  (let [version  (subscribe [::route-subs/query-param :version])
        sub-type (subscribe [::route-subs/query-param :subtype])]
    (fn []
      (dispatch [::events/reset-version])
      (let [is-new? (boolean (seq @sub-type))]
        (dispatch [::events/is-new? is-new?])
        (if is-new?
          (dispatch-clear-events @sub-type)
          (dispatch [::events/get-module @version]))
        [:<>
         [CommonComponents]
         [Module]]))))

(defn AppDetailsRoute
  [{:keys [path-params]}]
  [AppDetails {:key (str path-params)}])

(defn AppsOverview
  [_path]
  [:<>
   [CommonComponents]
   [apps-store-views/RootView]])
