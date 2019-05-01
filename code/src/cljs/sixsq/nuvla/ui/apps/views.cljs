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
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [taoensso.timbre :as timbre]
    [taoensso.timbre :as log]))


(defn module-details
  [new-type]
  (let [module (subscribe [::subs/module])]
    (dispatch [::main-events/changes-protection? false])
    (dispatch [::events/form-valid true])
    (dispatch [::events/set-validate-form? false])
    (fn [new-type]
      (let [type (:type @module)]
        (if (or (= "component" new-type) (= "COMPONENT" type))
          [apps-component-views/view-edit]
          [apps-project-views/view-edit])))))


(defn new-module
  [new-type]
  (let [nav-path   (subscribe [::main-subs/nav-path])
        new-parent (utils/nav-path->parent-path @nav-path)
        new-name   (utils/nav-path->module-name @nav-path)]
    (dispatch [::events/clear-module new-name new-parent (str/upper-case new-type)])
    (case new-type
      "component" (apps-component-views/clear-module)
      "project" (apps-project-views/clear-module)
      (apps-project-views/clear-module))))


(defn apps
  []
  (let [nav-path (subscribe [::main-subs/nav-path])]
    (fn []
      (let [module-name (utils/nav-path->module-name @nav-path)
            query       (clojure.walk/keywordize-keys (:query (url/url (-> js/window .-location .-href))))
            new-type    (:type query)
            version     (:version query nil)
            is-root?    (empty? module-name)
            is-new?     (not (empty? new-type))]
        (dispatch [::events/is-new? (not (empty? new-type))])
        (if-not is-root?
          (do
            (if-not is-new?
              (dispatch [::events/get-module version])
              (new-module new-type))
            [module-details new-type])
          [apps-store-views/root-view])))))


(defmethod panel/render :apps
  [path]
  (timbre/set-level! :info)
  [:div
   [views-detail/validation-error-message]
   [views-detail/version-warning]
   [views-detail/add-modal]
   [views-detail/save-modal]
   [views-detail/logo-url-modal]
   [apps]])
