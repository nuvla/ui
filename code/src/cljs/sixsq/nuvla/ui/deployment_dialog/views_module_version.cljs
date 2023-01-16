(ns sixsq.nuvla.ui.deployment-dialog.views-module-version
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.deployment-dialog.events :as events]
            [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
            [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn get-version-id
  [module-versions version]
  (some (fn [[idx {:keys [href]}]] (when (= version href) idx)) module-versions))


(defn summary-row
  []
  (let [tr              (subscribe [::i18n-subs/tr])
        versions        (subscribe [::subs/module-versions])
        current-version (subscribe [::subs/current-module-content-id])]
    [ui/TableRow {:active   false
                  :on-click #(dispatch [::events/set-active-step :module-version])}
     [ui/TableCell {:collapsing true}
      [ui/Icon {:name "list ol", :size "large"}]]
     [ui/TableCell {:collapsing true} (@tr [:module-version])]
     [ui/TableCell [:div [:span "v" (get-version-id @versions @current-version)]]]]))


(defn VersionSelectionMessages
  []
  (let [tr                   (subscribe [::i18n-subs/tr])
        is-latest?           (subscribe [::subs/is-latest-version?])
        is-latest-published? (subscribe [::subs/is-latest-published-version?])
        is-module-published? (subscribe [::subs/is-module-published?])]
    [:<>
     (when (and @is-module-published? (not @is-latest-published?))
       [ui/Message {:header  (@tr [:deployment-warning-not-latest-pub-header])
                    :content (@tr [:deployment-warning-not-latest-pub-content])}])
     (when (and (not @is-module-published?) (not @is-latest?))
       [ui/Message {:header  (@tr [:deployment-warning-not-latest-draft-header])
                    :content (@tr [:deployment-warning-not-latest-draft-content])}])]))


(defmethod utils/step-content :module-version
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        versions         (subscribe [::subs/module-versions])
        module-id        (subscribe [::subs/module-id])
        selected-version (subscribe [::subs/selected-version])]
    (fn []
      (let [options (map (fn [[idx {:keys [href commit published]}]]
                           {:key   idx
                            :value href
                            :text  (str "v" idx " | " (general-utils/truncate commit 70)
                                        (when (true? published) (str " | " (@tr [:published]))))
                            :icon  (when published apps-utils/publish-icon)})
                         @versions)]
        [ui/Segment {:clearing true}
         [ui/Form
          [ui/Message {:info    true
                       :header  (@tr [:quick-tip])
                       :content (@tr [:quick-tip-fetch-module])}]
          [VersionSelectionMessages]
          [ui/FormDropdown {:value     @selected-version
                            :scrolling true
                            :upward    false
                            :selection true
                            :on-change (ui-callback/value
                                         #(do (dispatch [::events/set-selected-version %])
                                              (dispatch [::events/fetch-module
                                                         (->> %
                                                              (get-version-id @versions)
                                                              (str @module-id "_"))])))
                            :fluid     true
                            :options   options}]]]))))
