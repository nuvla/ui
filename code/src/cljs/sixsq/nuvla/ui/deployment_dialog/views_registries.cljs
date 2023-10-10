(ns sixsq.nuvla.ui.deployment-dialog.views-registries
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.deployment-dialog.events :as events]
            [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
            [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
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
        [icons/DbIconFull {:size "large"}]
        [icons/WarningIcon {:size "large", :color "red"}])]
     [ui/TableCell {:collapsing true} (@tr [:registries])]
     [ui/TableCell [:div [:span description]]]]))


(defn dropdown-creds
  [private-registry-id info]
  (let [tr             (subscribe [::i18n-subs/tr])
        registry       (subscribe [::subs/infra-registry private-registry-id])
        registry-name  (or (:name @registry) private-registry-id)
        creds-options  (subscribe [::subs/infra-registries-creds-by-parent-options
                                   private-registry-id])
        registry-descr (:description @registry)
        {:keys [cred-id preselected?]} info
        Label [uix/FieldLabel
               {:name       [:span registry-name]
                :help-popup (when registry-descr [uix/HelpPopup registry-descr])
                :required?  true}]]
    (if (and preselected?
             (not (some #(= cred-id (:value %)) @creds-options)))
      [ui/FormInput
       {:disabled      true
        :label         (r/as-element Label)
        :default-value (@tr [:preselected])}]
      [ui/FormDropdown
       (cond->
         {:label         (r/as-element Label)
          :selection     true
          :default-value cred-id
          :placeholder   (@tr [:select-credential])
          :options       @creds-options
          :on-change     (ui-callback/value
                           #(dispatch [::events/set-credential-registry private-registry-id %]))}
         (empty? @creds-options) (assoc :error (@tr [:no-available-creds-registry])))]
      )))


(defmethod utils/step-content :registries
  []
  (let [registries-creds (subscribe [::subs/registries-creds])
        loading?         (subscribe [::subs/infra-registries-loading?])]
    [ui/Segment
     [ui/Form {:loading @loading?}
      (for [[private-registry-id info] @registries-creds]
        ^{:key private-registry-id}
        [dropdown-creds private-registry-id info])]]))
