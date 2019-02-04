(ns sixsq.slipstream.webui.deployment-dialog.views-size
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.slipstream.webui.deployment-dialog.events :as events]
    [sixsq.slipstream.webui.deployment-dialog.subs :as subs]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.ui-callback :as ui-callback]))


(defn summary-row
  []
  (let [tr (subscribe [::i18n-subs/tr])
        size (subscribe [::subs/size])
        completed? (subscribe [::subs/size-completed?])

        {:keys [cpu ram disk]} @size

        on-click-fn #(dispatch [::events/set-active-step :size])]

    ^{:key "size"}
    [ui/TableRow {:active   false
                  :on-click on-click-fn}
     [ui/TableCell {:collapsing true}
      (if @completed?
        [ui/Icon {:name "expand arrows alternate", :size "large", :vertical-align "middle"}]
        [ui/Icon {:name "warning sign", :size "large", :color "red"}])]
     [ui/TableCell {:collapsing true} (@tr [:size])]
     [ui/TableCell [:div
                    [:span "CPU: " cpu]
                    [:br]
                    [:span "RAM: " ram " MB"]
                    [:br]
                    [:span "DISK: " disk " GB"]]]]))


(defn input-size
  [name property-key]
  (let [deployment (subscribe [::subs/deployment])
        size (subscribe [::subs/size])]
    ^{:key (str (:id @deployment) "-" name)}
    [ui/FormInput {:type          "number",
                   :label         name,
                   :default-value (get @size property-key),
                   :on-blur       (ui-callback/input-callback
                                    (fn [new-value]
                                      (let [new-int (int new-value)]
                                        (dispatch
                                          [::events/set-deployment
                                           (assoc-in @deployment [:module :content property-key] new-int)]))))}]))


(defn content
  []
  [ui/Form
   [input-size "CPU" :cpu]
   [input-size "RAM [MB]" :ram]
   [input-size "DISK [GB]" :disk]])
