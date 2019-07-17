(ns sixsq.nuvla.ui.apps-application.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.apps-application.events :as events]
    [sixsq.nuvla.ui.apps-application.spec :as spec]
    [sixsq.nuvla.ui.apps-application.subs :as subs]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [taoensso.timbre :as log]))


(defn summary []
  (let []
    [apps-views-detail/summary]))


(defn docker-compose-section []
  (let [tr             (subscribe [::i18n-subs/tr])
        module         (subscribe [::apps-subs/module])
        is-new?        (subscribe [::apps-subs/is-new?])
        docker-compose (subscribe [::subs/docker-compose])
        form-valid?    (subscribe [::apps-subs/form-valid?])
        default-value  @docker-compose]
    (fn []
      (let [editable? (apps-utils/editable? @module @is-new?)]
        [uix/Accordion
         [ui/Form
          [ui/FormField
           [ui/Transition {:visible (not @form-valid?)}
            [ui/Label {:pointing "below", :basic true, :color "red"}
             "Please fill in the docker compose"]]

           [ui/CodeMirror {:value      default-value
                           :autoCursor true
                           :options    {:mode              "text/x-yaml"
                                        :read-only         (not editable?)
                                        :line-numbers      true
                                        :style-active-line true
                                        :fold-gutter       true
                                        :gutters           ["CodeMirror-foldgutter"]}
                           :on-change  (fn [editor data value]
                                         (dispatch [::events/update-docker-compose nil value])
                                         (dispatch [::main-events/changes-protection? true])
                                         (dispatch [::apps-events/validate-form]))
                           }]]]
         :label "docker-compose.yaml"
         :default-open true]))))


(defn clear-module
  []
  (dispatch [::events/clear-module]))


(defn view-edit
  []
  (let [module-common (subscribe [::apps-subs/module-common])
        module        (subscribe [::apps-subs/module])
        is-new?       (subscribe [::apps-subs/is-new?])]
    (fn []
      (let [name      (get @module-common ::apps-spec/name)
            parent    (get @module-common ::apps-spec/parent-path)
            editable? (apps-utils/editable? @module @is-new?)]
        (dispatch [::apps-events/set-form-spec ::spec/module-application])
        (dispatch [::apps-events/set-module-subtype :application])
        [ui/Container {:fluid true}
         [:h2 {:style {:display :inline}}
          [ui/Icon {:name "cubes"}]
          parent (when (not-empty parent) "/") name]
         [acl/AclButton {:default-value (get @module-common ::apps-spec/acl)
                         :on-change     #(do (dispatch [::apps-events/acl %])
                                             (dispatch [::main-events/changes-protection? true]))
                         :read-only     (not editable?)}]
         [apps-views-detail/control-bar]
         [summary]
         [docker-compose-section]
         [apps-views-detail/env-variables-section]
         [apps-views-detail/urls-section]
         [apps-views-detail/output-parameters-section]
         #_[data-types-section]
         [apps-views-detail/save-action]
         [apps-views-detail/add-modal]
         [apps-views-detail/save-modal]
         [apps-views-detail/logo-url-modal]
         [deployment-dialog-views/deploy-modal]]))))
