(ns sixsq.nuvla.ui.deployment-dialog.views-registries
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.credentials.components :as creds-comp]
    [sixsq.nuvla.ui.deployment-dialog.events :as events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn summary-row
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        count       (subscribe [::subs/module-private-registries-count])
        completed?  (subscribe [::subs/registries-completed?])

        description (str "Count: " @count)
        on-click-fn #(dispatch [::events/set-active-step :registries])]

    ^{:key "registries"}
    [ui/TableRow {:active   false
                  :on-click on-click-fn}
     [ui/TableCell {:collapsing true}
      (if @completed?
        [ui/Icon {:name "database", :size "large"}]
        [ui/Icon {:name "warning sign", :size "large", :color "red"}])]
     [ui/TableCell {:collapsing true} (@tr [:registries])]
     [ui/TableCell [:div [:span description]]]]))


(defn dropdown-creds
  [private-registry-id]
  (let [tr               (subscribe [::i18n-subs/tr])
        registry         (subscribe [::subs/infra-registry private-registry-id])
        registry-name    (or (:name @registry) private-registry-id)
        creds-options    (subscribe [::subs/infra-registries-creds-by-parent-options
                                     private-registry-id])
        registry-descr   (:description @registry)
        registries-creds (subscribe [::subs/registries-creds])
        default-value    (get-in @registries-creds [private-registry-id :cred-id])]
    [ui/FormDropdown
     (cond->
       {:required      true
        :label         (r/as-element [:label registry-name ff/nbsp
                                      (when registry-descr (ff/help-popup registry-descr))
                                      (when default-value
                                        [:span " "
                                         [creds-comp/CredentialCheckPopup default-value]])])
        :selection     true
        :default-value default-value
        :placeholder   (@tr [:select-credential])
        :options       @creds-options
        :on-change     (ui-callback/value
                         #(dispatch [::events/set-credential-registry private-registry-id %]))}
       (empty? @creds-options) (assoc :error (@tr [:no-available-creds-registry])))]))


(defn content
  []
  (fn []
    (let [infra-registries (subscribe [::subs/registries-creds-not-preselected])
          loading?         (subscribe [::subs/infra-registries-loading?])]
      [ui/Form {:loading @loading?}
       (for [private-registry-id @infra-registries]
         ^{:key private-registry-id}
         [dropdown-creds private-registry-id])])))
