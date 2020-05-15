(ns sixsq.nuvla.ui.apps.views
  (:require
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [sixsq.nuvla.ui.apps-application.views :as apps-application-views]
    [sixsq.nuvla.ui.apps-component.views :as apps-component-views]
    [sixsq.nuvla.ui.apps-project.views :as apps-project-views]
    [sixsq.nuvla.ui.apps-store.views :as apps-store-views]
    [sixsq.nuvla.ui.apps.events :as events]
    [sixsq.nuvla.ui.apps.subs :as subs]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.apps.views-detail :as views-detail]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.validation :as utils-validation]
    [taoensso.timbre :as timbre]))


(defn module-details
  [new-subtype]
  (let [module (subscribe [::subs/module])]
    (dispatch [::main-events/changes-protection? false])
    (dispatch [::events/form-valid true])
    (dispatch [::events/set-validate-form? false])
    (fn [new-subtype]
      (let [subtype (or (:subtype @module) new-subtype)]
        (case subtype
          "component" [apps-component-views/view-edit]
          "application" [apps-application-views/view-edit]
          "application_kubernetes" [apps-application-views/view-edit]
          [apps-project-views/view-edit])))))


(defn new-module
  [new-subtype]
  (let [nav-path   (subscribe [::main-subs/nav-path])
        new-parent (utils/nav-path->parent-path @nav-path)
        new-name   (utils/nav-path->module-name @nav-path)]
    (dispatch [::events/clear-module new-name new-parent new-subtype])
    (apps-component-views/clear-module)
    (apps-application-views/clear-module)))


(defn apps
  []
  (let [nav-path         (subscribe [::main-subs/nav-path])
        nav-query-params (subscribe [::main-subs/nav-query-params])]
    (fn []
      (let [module-name (utils/nav-path->module-name @nav-path)
            new-subtype (:subtype @nav-query-params)
            version     (:version @nav-query-params nil)
            is-root?    (empty? module-name)
            is-new?     (not (empty? new-subtype))]
        (dispatch [::events/is-new? (not (empty? new-subtype))])
        (dispatch [::events/clear-version-warning])
        (if is-root?
          [apps-store-views/root-view]
          (do
            (if is-new?
              (new-module new-subtype)
              (dispatch [::events/get-module version]))
            [module-details new-subtype]))))))


(defmethod panel/render :apps
  [path]
  (timbre/set-level! :info)
  [:div
   [utils-validation/validation-error-message ::subs/form-valid?]
   [views-detail/version-warning]
   [views-detail/add-modal]
   [views-detail/save-modal]
   [views-detail/logo-url-modal]
   [apps]])
