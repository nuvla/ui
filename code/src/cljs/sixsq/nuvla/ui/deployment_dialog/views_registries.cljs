(ns sixsq.nuvla.ui.deployment-dialog.views-registries
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.deployment-dialog.events :as events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn summary-row
  []
  (let [tr                     (subscribe [::i18n-subs/tr])
        selected-infra-service (subscribe [::subs/selected-infra-service])
        selected-credential    (subscribe [::subs/selected-credential])
        completed?             (subscribe [::subs/infra-services-completed?])
        creds-completed?       (subscribe [::subs/credentials-completed?])
        on-click-fn            #(dispatch [::events/set-active-step :infra-services])]

    ^{:key "infra-services"}
    [:<>
     (let [{:keys [id name description subtype]} @selected-infra-service]
       [ui/TableRow {:active   false
                     :on-click on-click-fn}
        [ui/TableCell {:collapsing true}
         (if @completed?
           [ui/Icon {:name "cloud", :size "large"}]
           [ui/Icon {:name "warning sign", :size "large", :color "red"}])]
        [ui/TableCell {:collapsing true} (@tr [:infra-services])]
        [ui/TableCell [:div
                       [:span (or name id)]
                       [:br]
                       [:span description]
                       [:br]
                       [:span subtype]]]])
     (let [{:keys [id name description]} @selected-credential]
       [ui/TableRow {:active   false
                     :on-click on-click-fn}
        [ui/TableCell {:collapsing true}
         (if @creds-completed?
           [ui/Icon {:name "key", :size "large"}]
           [ui/Icon {:name "warning sign", :size "large", :color "red"}])]
        [ui/TableCell {:collapsing true} (@tr [:credentials])]
        [ui/TableCell [:div
                       [:span (or name id)]
                       [:br]
                       [:span description]]]])]))


(defn dropdown-creds
  [index private-registry-id]
  (let [tr             (subscribe [::i18n-subs/tr])
        registry       (subscribe [::subs/infra-registry private-registry-id])
        registry-name  (or (:name @registry) private-registry-id)
        loading?       (subscribe [::subs/infra-registries-creds-loading?])
        creds-options  (subscribe [::subs/infra-registries-creds-by-parent-options
                                   private-registry-id])
        registry-descr (:description @registry)
        value          (subscribe [::subs/registries-credentials-by-index index])]
    (if @registry
      [ui/FormDropdown
       (cond->
         {:required     true
          :loading      @loading?
          :label        (r/as-element [:label registry-name ff/nbsp
                                       (when registry-descr (ff/help-popup registry-descr))])
          :selection    true
          :defaultValue @value
          :placeholder  (@tr [:select-credential])
          :options      @creds-options
          :on-change    (ui-callback/value
                          #(dispatch [::events/set-credential-registry index %]))}
         (and
           (not @loading?)
           (empty? @creds-options)) (assoc :error "No credentials available for this registry"))]
      [ui/Message {:negative true}
       "No infrastructure found with following id " private-registry-id "!"])))


(defn content
  []
  (fn []
    (let [private-registries (subscribe [::subs/private-registries])
          loading?           (subscribe [::subs/infra-registries-loading?])]
      [ui/Form {:loading @loading?}
       (for [[index private-registry-id] (map-indexed vector @private-registries)]
         ^{:key (str private-registry-id "-" index)}
         [dropdown-creds index private-registry-id])])))
