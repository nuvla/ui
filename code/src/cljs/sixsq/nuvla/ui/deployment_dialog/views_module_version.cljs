(ns sixsq.nuvla.ui.deployment-dialog.views-module-version
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.deployment-dialog.events :as events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn get-version-id
  [module-versions version]
  (some (fn [[idx {:keys [href]}]] (when (= version href) idx)) module-versions))


(defn summary-row
  []
  (let [tr              (subscribe [::i18n-subs/tr])
        on-click-fn     #(dispatch [::events/set-active-step :module-version])
        versions        (subscribe [::subs/module-versions])
        current-version (subscribe [::subs/current-module-content-id])]
    [ui/TableRow {:active   false
                  :on-click on-click-fn}
     [ui/TableCell {:collapsing true}
      [ui/Icon {:name "list ol", :size "large"}]]
     [ui/TableCell {:collapsing true} (@tr [:module-version])]
     [ui/TableCell [:div [:span "v" (get-version-id @versions @current-version)]]]]))


(defn content
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        module-id        (subscribe [::subs/module-id])
        versions         (subscribe [::subs/module-versions])
        current-version  (subscribe [::subs/current-module-content-id])
        selected-version (r/atom nil)]
    (fn []
      (let [options (map (fn [[idx {:keys [href commit]}]]
                           {:key   idx,
                            :value href
                            :text  (str "v" idx " | " commit)}) @versions)]
        (when (and @module-id (not (seq @versions)))
          (dispatch [::events/get-module-versions @module-id]))
        [ui/Segment {:clearing true}
         [ui/Form
          [ui/Message {:info    true
                       :header  (@tr [:quick-tip])
                       :content (@tr [:quick-tip-fetch-module])}]
          [ui/FormDropdown {:value     (or @selected-version @current-version)
                            :scrolling true
                            :selection true
                            :on-change (ui-callback/value #(reset! selected-version %))
                            :fluid     true
                            :options   options}]
          [ui/Button {:disabled (or
                                  (nil? @selected-version)
                                  (= @current-version @selected-version))
                      :floated  "right"
                      :content  (@tr [:change-version])
                      :primary  true
                      :on-click #(dispatch [::events/fetch-module
                                            (->> @selected-version
                                                 (get-version-id @versions)
                                                 (str @module-id "_"))])}]]]))))

