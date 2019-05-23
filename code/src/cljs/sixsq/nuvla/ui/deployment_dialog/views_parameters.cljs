(ns sixsq.nuvla.ui.deployment-dialog.views-parameters
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
  (let [tr           (subscribe [::i18n-subs/tr])
        input-params (subscribe [::subs/input-parameters])
        completed?   (subscribe [::subs/parameters-completed?])

        description  (str "Number of parameters: " (count @input-params))
        on-click-fn  #(dispatch [::events/set-active-step :parameters])]

    ^{:key "parameters"}
    [ui/TableRow {:active   false
                  :on-click on-click-fn}
     [ui/TableCell {:collapsing true}
      (if @completed?
        [ui/Icon {:name "list alternate outline", :size "large"}]
        [ui/Icon {:name "warning sign", :size "large", :color "red"}])]
     [ui/TableCell {:collapsing true} (@tr [:parameters])]
     [ui/TableCell [:div [:span description]]]]))


(defn as-form-input
  [{:keys [parameter description value] :as param}]
  (let [deployment (subscribe [::subs/deployment])]
    ^{:key parameter}
    [ui/FormField
     [:label parameter ff/nbsp (ff/help-popup description)]
     [ui/Input
      {:type          "text"
       :name          parameter
       :default-value (or value "")
       :read-only     false
       :fluid         true
       :on-blur       (ui-callback/input-callback
                        (fn [new-value]
                          (let [updated-deployment (utils/update-parameter-in-deployment parameter new-value @deployment)]
                            (dispatch [::events/set-deployment updated-deployment]))))}]]))


(defn content
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        input-params (subscribe [::subs/input-parameters])]

    (if (seq @input-params)
      (vec (concat [ui/Form]
                   (map as-form-input @input-params)))
      [ui/Message {:success true} (@tr [:no-input-parameters])])))
