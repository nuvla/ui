(ns sixsq.nuvla.ui.apps.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.apps-application.events :as apps-application-events]
            [sixsq.nuvla.ui.apps-application.views :as apps-application-views]
            [sixsq.nuvla.ui.apps-component.events :as apps-component-events]
            [sixsq.nuvla.ui.apps-component.views :as apps-component-views]
            [sixsq.nuvla.ui.apps-project.events :as apps-project-events]
            [sixsq.nuvla.ui.apps-project.views :as apps-project-views]
            [sixsq.nuvla.ui.apps-store.views :as apps-store-views]
            [sixsq.nuvla.ui.apps.events :as events]
            [sixsq.nuvla.ui.apps.subs :as subs]
            [sixsq.nuvla.ui.apps.utils :as utils]
            [sixsq.nuvla.ui.apps.views-detail :as views-detail]
            [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.main.events :as main-events]
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
    (dispatch [::apps-project-events/clear-apps-project])))


(defn ModuleDetails
  []
  (let [module (subscribe [::subs/module])
        new-subtype (subscribe [::route-subs/query-param :subtype])]
    (fn []
      (let [subtype (or (:subtype @module) @new-subtype)]
        (case subtype
          "component" [apps-component-views/view-edit]
          "application" [apps-application-views/ViewEdit]
          "application_kubernetes" [apps-application-views/ViewEdit]
          ^{:key subtype}
          [apps-project-views/ViewEdit])))))


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
  (let [version      (subscribe [::route-subs/query-param :version])
        sub-type     (subscribe [::route-subs/query-param :subtype ])]
    (fn []
      (let [is-new?  (boolean (seq @sub-type))]
        (dispatch [::events/is-new? is-new?])
        (if is-new?
          (dispatch-clear-events @sub-type)
          (dispatch [::events/get-module @version]))
        [:<>
         [CommonComponents]
         [Module]]))))

(defn AppDetailsRoute
  []
  (fn [] [AppDetails]))

(defn AppsOverview
  [_path]
  [:<>
   [CommonComponents]
   [apps-store-views/RootView]])
