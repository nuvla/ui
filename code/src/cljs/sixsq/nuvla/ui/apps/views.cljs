(ns sixsq.nuvla.ui.apps.views
  (:require
    [cemerick.url :as url]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
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
    [taoensso.timbre :as timbre]
    [taoensso.timbre :as log]))


(defn module-details
  [new-subtype]
  (let [module (subscribe [::subs/module])]
    (dispatch [::main-events/changes-protection? false])
    (dispatch [::events/form-valid true])
    (dispatch [::events/set-validate-form? false])
    (fn [new-subtype]
      (let [subtype (:subtype @module)]
        (if (or (= "component" new-subtype) (= "component" subtype))
          [apps-component-views/view-edit]
          [apps-project-views/view-edit])))))


(defn new-module
  [new-subtype]
  (let [nav-path   (subscribe [::main-subs/nav-path])
        new-parent (utils/nav-path->parent-path @nav-path)
        new-name   (utils/nav-path->module-name @nav-path)]
    (dispatch [::events/clear-module new-name new-parent new-subtype])
    (apps-component-views/clear-module)))


(defn apps
  []
  (let [nav-path (subscribe [::main-subs/nav-path])]
    (fn []
      (let [module-name (utils/nav-path->module-name @nav-path)
            query       (clojure.walk/keywordize-keys (:query (url/url (-> js/window .-location .-href))))
            new-subtype (:subtype query)
            version     (:version query nil)
            is-root?    (empty? module-name)
            is-new?     (not (empty? new-subtype))]
        (dispatch [::events/is-new? (not (empty? new-subtype))])
        (if-not is-root?
          (do
            (if-not is-new?
              (dispatch [::events/get-module version])
              (new-module new-subtype))
            [module-details new-subtype])
          [apps-store-views/root-view])))))


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
