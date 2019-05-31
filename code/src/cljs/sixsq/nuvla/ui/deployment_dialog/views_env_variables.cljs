(ns sixsq.nuvla.ui.deployment-dialog.views-env-variables
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.deployment-dialog.events :as events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn summary-row
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        env-variables (subscribe [::subs/env-variables])
        completed?    (subscribe [::subs/env-variables-completed?])

        description   (str "Count: " (count @env-variables))
        on-click-fn   #(dispatch [::events/set-active-step :environmental-variables])]

    ^{:key "env-variables"}
    [ui/TableRow {:active   false
                  :on-click on-click-fn}
     [ui/TableCell {:collapsing true}
      (if @completed?
        [ui/Icon {:name "list alternate outline", :size "large"}]
        [ui/Icon {:name "warning sign", :size "large", :color "red"}])]
     [ui/TableCell {:collapsing true} (@tr [:environmental-variables])]
     [ui/TableCell [:div [:span description]]]]))


(defn as-form-input
  [{env-name :name env-description :description env-value :value env-required :required :as env-variable}]
  (let [deployment (subscribe [::subs/deployment])]
    ^{:key env-name}
    [ui/FormField {:required env-required}
     [:label env-name ff/nbsp (ff/help-popup env-description)]
     [ui/Input
      {:type          "text"
       :name          env-name
       :default-value (or env-value "")
       :read-only     false
       :fluid         true
       :on-blur       (ui-callback/input-callback
                        (fn [new-value]
                          (let [updated-deployment (utils/update-env-variable-in-deployment env-name new-value @deployment)]
                            (dispatch [::events/set-deployment updated-deployment]))))}]]))


(defn content
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        env-variables (subscribe [::subs/env-variables])]

    (if (seq @env-variables)
      (vec (concat [ui/Form]
                   (map as-form-input @env-variables)))
      [ui/Message {:success true} (@tr [:no-input-parameters])])))
