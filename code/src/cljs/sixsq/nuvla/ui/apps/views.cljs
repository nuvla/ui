(ns sixsq.nuvla.ui.apps.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.apps-application.views :as apps-application-views]
    [sixsq.nuvla.ui.apps-component.views :as apps-component-views]
    [sixsq.nuvla.ui.apps-project.views :as apps-project-views]
    [sixsq.nuvla.ui.apps-store.views :as apps-store-views]
    [sixsq.nuvla.ui.apps.events :as events]
    [sixsq.nuvla.ui.apps.subs :as subs]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.apps.views-detail :as views-detail]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.deployment.events :as deployment-events]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.validation :as utils-validation]
    [taoensso.timbre :as timbre]))


(defn ModuleDetails
  [_new-subtype]
  (let [module (subscribe [::subs/module])]
    (dispatch [::main-events/changes-protection? false])
    (dispatch [::events/form-valid true])
    (dispatch [::events/set-validate-form? false])
    (fn [new-subtype]
      (dispatch [::deployment-events/get-module-deployments])
      [views-detail/VersionWarning]
      (let [subtype (or (:subtype @module) new-subtype)]
        (case subtype
          "component" [apps-component-views/view-edit]
          "application" [apps-application-views/ViewEdit]
          "application_kubernetes" [apps-application-views/ViewEdit]
          ^{:key (random-uuid)}
          [apps-project-views/ViewEdit])))))


(defn new-module
  [new-subtype]
  (let [nav-path   (subscribe [::main-subs/nav-path])
        new-parent (utils/nav-path->parent-path @nav-path)
        new-name   (utils/nav-path->module-name @nav-path)]
    (dispatch [::events/clear-module new-name new-parent new-subtype])
    (apps-component-views/clear-module)
    (apps-application-views/clear-module)
    (apps-project-views/clear-module)))


(defn Apps
  []
  (let [nav-path         (subscribe [::main-subs/nav-path])
        nav-query-params (subscribe [::main-subs/nav-query-params])]
    (fn []
      (let [module-name (utils/nav-path->module-name @nav-path)
            new-subtype (:subtype @nav-query-params)
            version     (:version @nav-query-params nil)
            is-root?    (empty? module-name)
            is-new?     (boolean (seq new-subtype))]
        (dispatch [::events/is-new? is-new?])
        (if is-root?
          [apps-store-views/RootView]
          (do
            (if is-new?
              (new-module new-subtype)
              (dispatch [::events/get-module version]))
            [ModuleDetails new-subtype]))))))


(defmethod panel/render :apps
  [_path]
  (timbre/set-level! :info)
  [ui/DimmerDimmable {:dimmed true}
   [utils-validation/validation-error-message ::subs/form-valid?]
   [views-detail/AddModal]
   [views-detail/save-modal]
   [views-detail/logo-url-modal]
   [deployment-dialog-views/deploy-modal]
   [main-components/NotFoundPortal
    ::subs/module-not-found?
    :no-module-message-header
    :no-module-message-content]
   [Apps]])
