(ns sixsq.nuvla.ui.deployment-dialog.views-files
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.deployment-dialog.events :as events]
            [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
            [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn summary-row
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        files       (subscribe [::subs/files])

        description (str "Count: " (count @files))
        on-click-fn #(dispatch [::events/set-active-step :files])]

    ^{:key "files"}
    [ui/TableRow {:active   false
                  :on-click on-click-fn}
     [ui/TableCell {:collapsing true}
      [ui/Icon {:name "file alternate outline", :size "large"}]]
     [ui/TableCell {:collapsing true} (@tr [:files])]
     [ui/TableCell [:div [:span description]]]]))

(defn as-form-text-area
  [index {:keys [file-name file-content]}]
  (let [deployment (subscribe [::subs/deployment])]
    [uix/Accordion
     [ui/Form
      [ui/TextArea
       {:rows          10
        :default-value file-content
        :on-change     (ui-callback/value
                         #(dispatch [::events/set-deployment
                                     (assoc-in
                                       @deployment
                                       [:module :content :files
                                        index :file-content] %)]))}]]
     :label file-name
     :title-size :div
     :default-open false]))


(defmethod utils/step-content :files
  []
  (let [files (subscribe [::subs/files])]
    [ui/Segment
     (map-indexed
       (fn [i file]
         ^{:key (str (:file-name file) "-" i)}
         [as-form-text-area i file])
       @files)]))
