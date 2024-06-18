(ns sixsq.nuvla.ui.pages.apps.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.deployment-dialog.views :as deployment-dialog-views]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.pages.apps.apps-application.views :as apps-application-views]
            [sixsq.nuvla.ui.pages.apps.apps-applications-sets.views :as apps-applications-sets-views]
            [sixsq.nuvla.ui.pages.apps.apps-component.views :as apps-component-views]
            [sixsq.nuvla.ui.pages.apps.apps-project.views :as apps-project-views]
            [sixsq.nuvla.ui.pages.apps.apps-store.views :as apps-store-views]
            [sixsq.nuvla.ui.pages.apps.events :as events]
            [sixsq.nuvla.ui.pages.apps.subs :as subs]
            [sixsq.nuvla.ui.pages.apps.utils :as utils]
            [sixsq.nuvla.ui.pages.apps.views-detail :as views-detail]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.utils.validation :as utils-validation]))


(defn ModuleDetails
  []
  (let [module (subscribe [::subs/module])
        new-subtype (subscribe [::route-subs/query-param :subtype])]
    (fn []
      (let [subtype (or (:subtype @module) @new-subtype)]
        (condp = subtype
          utils/subtype-component [apps-component-views/view-edit]
          utils/subtype-application [apps-application-views/ViewEdit]
          utils/subtype-application-k8s [apps-application-views/ViewEdit]
          utils/subtype-applications-sets [apps-applications-sets-views/ViewEdit]
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
  [:<>
   [CommonComponents]
   [Module]])

(defn AppDetailsRoute
  [{:keys [path-params]}]
  [AppDetails {:key (str path-params)}])

(defn AppsOverview
  [_path]
  [:<>
   [CommonComponents]
   [apps-store-views/RootView]])
