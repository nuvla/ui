(ns sixsq.nuvla.ui.apps.views
  (:require
    [cemerick.url :as url]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.apps-component.views :as apps-component-views]
    [sixsq.nuvla.ui.apps-project.views :as apps-project-views]
    [sixsq.nuvla.ui.apps-store.views :as apps-store-views]
    [sixsq.nuvla.ui.apps.events :as events]
    [sixsq.nuvla.ui.apps.subs :as subs]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.views :as history]
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
    [taoensso.timbre :as log]
    [taoensso.timbre :as timbre]))


(defn module-details
  [new-type]
  (let [module     (subscribe [::subs/module])
        new-parent (utils/nav-path->parent-path @(subscribe [::main-subs/nav-path]))
        new-name   (utils/nav-path->module-name @(subscribe [::main-subs/nav-path]))]
    (fn [new-type]
      (let [type (:type @module)]
        (dispatch [::events/is-new? (not-empty new-type)])
        (when (nil? @module)
          (do
            (dispatch [::events/name new-name])
            (dispatch [::events/parent new-parent])
            ))
        (if (or (= "component" new-type) (= "COMPONENT" type))
          [apps-component-views/view-edit module]
          [apps-project-views/view-edit module])
        ))))

(defn module
  [new-type version]
  (dispatch [::events/get-module version])
  [module-details new-type version])


(defn version-warning []
  (let [version-warning? (subscribe [::subs/version-warning?])]
    (fn []
      (let []
        [ui/Message {;:on-dismiss (dispatch [::events/warning nil])
                     :hidden  (not (true? @version-warning?))
                     :warning true}
         [ui/MessageHeader "Warning!"]
         [ui/MessageContent "This is not the latest version. Click or tap "
          [:a {:on-click #(dispatch [::events/get-module])
               :style    {:cursor :pointer}} "here"]
          " to load the latest."]]))))


(defn apps
  []
  (let [query       (clojure.walk/keywordize-keys (:query (url/url (-> js/window .-location .-href))))
        type        (:type query)
        version     (:version query nil)
        module-name (utils/nav-path->module-name @(subscribe [::main-subs/nav-path]))]
    (log/infof "ver: %s" version)
    (if module-name
      (module type version)
      [apps-store-views/root-view])))

(defmethod panel/render :apps
  [path]
  (timbre/set-level! :info)
  [:div
   [version-warning]
   [apps]])
