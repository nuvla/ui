(ns sixsq.nuvla.ui.apps-application.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.apps-application.events :as events]
    [sixsq.nuvla.ui.apps-application.spec :as spec]
    [sixsq.nuvla.ui.apps-application.subs :as subs]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


(defn clear-module
  []
  (dispatch [::events/clear-module]))


(defn summary []
  (let []
    [apps-views-detail/summary]))


(defn single-file [{:keys [id ::spec/file-name ::spec/file-content]}]
  (let [form-valid?     (subscribe [::apps-subs/form-valid?])
        editable?       (subscribe [::apps-subs/editable?])
        local-validate? (r/atom false)]
    (fn [{:keys [id ::spec/file-name ::spec/file-content]}]
      (let [validate? (or @local-validate? (not @form-valid?))]
        [ui/TableRow {:key id, :vertical-align "top"}
         [ui/TableCell {:floated :left
                        :width   3}
          (if @editable?
            [apps-views-detail/input id (str "file-name-" id) file-name
             "file-name" ::events/update-file-name
             ::spec/file-name true]
            [:span file-name])]
         [ui/TableCell {:floated :left
                        :width   12
                        :error   (and validate? (not (s/valid? ::spec/file-content file-content)))}
          [ui/Form
           [ui/TextArea {:rows          10
                         :read-only     (not @editable?)
                         :default-value file-content
                         :on-change     (ui-callback/value
                                          #(do
                                             (reset! local-validate? true)
                                             (dispatch [::events/update-file-content id %])
                                             (dispatch [::main-events/changes-protection? true])
                                             (dispatch [::apps-events/validate-form])))}]]]
         (when @editable?
           [ui/TableCell {:floated :right
                          :width   1
                          :align   :right}
            [apps-views-detail/trash id ::events/remove-file]])]))))


(defn files-section []
  (let [tr        (subscribe [::i18n-subs/tr])
        files     (subscribe [::subs/files])
        editable? (subscribe [::apps-subs/editable?])]
    (fn []
      [uix/Accordion
       [:<>
        [:div (@tr [:module-files])
         [:span ff/nbsp (ff/help-popup (@tr [:module-files-help]))]]
        (if (empty? @files)
          [ui/Message
           (str/capitalize (str (@tr [:no-files]) "."))]
          [:div [ui/Table {:style {:margin-top 10}}
                 [ui/TableHeader
                  [ui/TableRow
                   [ui/TableHeaderCell {:content (str/capitalize (@tr [:filename]))}]
                   [ui/TableHeaderCell {:content (str/capitalize (@tr [:content]))}]
                   (when @editable?
                     [ui/TableHeaderCell {:content (str/capitalize (@tr [:action]))}])]]
                 [ui/TableBody
                  (for [[id file] @files]
                    ^{:key id}
                    [single-file file])]]])
        (when @editable?
          [:div {:style {:padding-top 10}}
           [apps-views-detail/plus ::events/add-file]])]
       :label (@tr [:module-files])
       :count (count @files)
       :default-open false])))


(defn docker-compose-section []
  (let [tr              (subscribe [::i18n-subs/tr])
        docker-compose  (subscribe [::subs/docker-compose])
        form-valid?     (subscribe [::apps-subs/form-valid?])
        editable?       (subscribe [::apps-subs/editable?])
        local-validate? (r/atom false)
        default-value   @docker-compose]
    (fn []
      (let [validate? (or @local-validate? (not @form-valid?))]
        [uix/Accordion
         [:<>
          [:div {:style {:margin-bottom "10px"}} "Docker compose"
           [:span ff/nbsp (ff/help-popup (@tr [:module-docker-compose-help]))]]

          [ui/CodeMirror {:value      default-value
                          :autoCursor true
                          :options    {:mode              "text/x-yaml"
                                       :read-only         (not @editable?)
                                       :line-numbers      true
                                       :style-active-line true}
                          :on-change  (fn [editor data value]
                                        (dispatch [::events/update-docker-compose nil value])
                                        (dispatch [::main-events/changes-protection? true])
                                        (dispatch [::apps-events/validate-form])
                                        (reset! local-validate? true))}]
          (when (and validate? (not (s/valid? ::spec/docker-compose @docker-compose)))
            (let [error-msg (-> @docker-compose general-utils/check-yaml second)]
              [ui/Label {:pointing "above", :basic true, :color "red"}
               (if (str/blank? error-msg)
                 (@tr [:module-docker-compose-error])
                 error-msg)]))]
         :label "Docker compose"
         :default-open true]))))


(defn view-edit
  []
  (let [module-common (subscribe [::apps-subs/module-common])
        editable?     (subscribe [::apps-subs/editable?])]
    (fn []
      (let [name   (get @module-common ::apps-spec/name)
            parent (get @module-common ::apps-spec/parent-path)]
        (dispatch [::apps-events/set-form-spec ::spec/module-application])
        (dispatch [::apps-events/set-module-subtype :application])
        [ui/Container {:fluid true}
         [uix/PageHeader "cubes" (str parent (when (not-empty parent) "/") name) :inline true]
         [acl/AclButton {:default-value (get @module-common ::apps-spec/acl)
                         :on-change     #(do (dispatch [::apps-events/acl %])
                                             (dispatch [::main-events/changes-protection? true]))
                         :read-only     (not @editable?)}]
         [apps-views-detail/control-bar]
         [summary]
         [apps-views-detail/env-variables-section]
         [files-section]
         [docker-compose-section]
         [apps-views-detail/urls-section]
         [apps-views-detail/output-parameters-section]
         [apps-views-detail/data-types-section]
         [apps-views-detail/save-action]
         [apps-views-detail/add-modal]
         [apps-views-detail/save-modal]
         [apps-views-detail/logo-url-modal]
         [deployment-dialog-views/deploy-modal]]))))
