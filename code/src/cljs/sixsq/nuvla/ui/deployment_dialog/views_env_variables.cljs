(ns sixsq.nuvla.ui.deployment-dialog.views-env-variables
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.deployment-dialog.events :as events]
            [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
            [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.plugins.module :as module-plugin]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn SummaryRow
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        env-variables (subscribe [::subs/env-variables])
        completed?    (subscribe [::subs/env-variables-completed?])

        description   (str "Count: " (count @env-variables))
        on-click-fn   #(dispatch [::events/set-active-step :env-variables])]

    ^{:key "env-variables"}
    [ui/TableRow {:active   false
                  :on-click on-click-fn}
     [ui/TableCell {:collapsing true}
      (if @completed?
        [ui/Icon {:name "list alternate outline", :size "large"}]
        [icons/WarningIcon {:size "large", :color "red"}])]
     [ui/TableCell {:collapsing true} (@tr [:env-variables])]
     [ui/TableCell [:div [:span description]]]]))

(defn AsFormInput
  [index {env-name        :name
          env-description :description
          env-value       :value
          env-required    :required}]
  (let [deployment     (subscribe [::subs/deployment])
        on-change #(dispatch [::events/set-deployment (assoc-in
                                                        @deployment
                                                        [:module :content
                                                         :environmental-variables
                                                         index :value] %)])]
    [ui/FormField {:required env-required}
     [uix/FieldLabel {:name env-name
                      :help-popup [uix/HelpPopup env-description]} ]
     (if (module-plugin/is-cred-env-var? env-name)
       [module-plugin/EnvCredential env-name env-value false on-change]
       [ui/Input
        {:type          "text"
         :name          env-name
         :default-value (or env-value "")
         :read-only     false
         :fluid         true
         :on-change     (ui-callback/input-callback on-change)}])]))


(defmethod utils/step-content :env-variables
  []
  (let [env-variables (subscribe [::subs/env-variables])]
    [ui/Segment
     [ui/Form
      (map-indexed
        (fn [i env-variable]
          ^{:key (str (:name env-variable) "_" i)}
          [AsFormInput i env-variable])
        @env-variables)]]))
