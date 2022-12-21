(ns sixsq.nuvla.ui.deployment-dialog.views-license
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.deployment-dialog.events :as events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn summary-row
  []
  (let [tr                 (subscribe [::i18n-subs/tr])
        license            (subscribe [::subs/license])
        license-completed? (subscribe [::subs/license-completed?])
        on-click-fn        #(dispatch [::events/set-active-step :license])]
    [ui/TableRow {:active   false
                  :on-click on-click-fn}
     [ui/TableCell {:collapsing true}
      (if @license-completed?
        [ui/Icon {:name "book", :size "large"}]
        [ui/Icon {:name "warning sign", :size "large", :color "red"}])]
     [ui/TableCell {:collapsing true} (@tr [:eula])]
     [ui/TableCell [:div [:span (:name @license)]]]]))


(defmethod utils/step-content :license
  []
  (let [tr                 (subscribe [::i18n-subs/tr])
        {:keys [name description url]} @(subscribe [::subs/license])
        license-completed? (subscribe [::subs/license-completed?])]
    [ui/Segment
     [ui/Container
      [ui/Header {:as      :h4
                  :icon    "book"
                  :content (@tr [:eula-full])}]
      [:h4 [:b (str (str/capitalize (@tr [:eula])) ": ")
            [:u [:a {:href url :target "_blank"} name]]]]
      (when description
        [:p [:i description]])
      [ui/Checkbox {:label     (@tr [:accept-eula])
                    :checked   @license-completed?
                    :on-change (ui-callback/checked
                                 #(dispatch [::events/set-license-accepted? %]))}]]]))
