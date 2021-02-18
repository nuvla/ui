(ns sixsq.nuvla.ui.apps.views-versions
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as reagent]
            [sixsq.nuvla.ui.apps.events :as events]
            [sixsq.nuvla.ui.apps.subs :as subs]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [cljsjs.jsdiff :as jsdiff]))


(defn show-versions [show-versions?]
  (let [tr        (subscribe [::i18n-subs/tr])
        label     (@tr (if @show-versions? [:hide-versions] [:show-versions]))
        icon-name (if @show-versions? "caret down" "caret right")]
    [:a {:style    {:cursor "pointer"}
         :on-click #(reset! show-versions? (not @show-versions?))}
     [ui/Icon {:name icon-name}]
     label]))


(defn version-comparison-modal
  [version-one version-two]
  [:<>
  [ui/ModalHeader
   [ui/Icon {:name "box"}] "Comparing"]
  [ui/ModalContent
   [jsdiff]
   [ui/CodeMirror {:value      version-two
                   :autoCursor true
                   :options    {:mode              "application/json"
                                :read-only         true
                                :line-numbers      true
                                :style-active-line true}
                   ;:on-change  (fn [editor data value]
                   ;              (dispatch [::events/update-docker-compose nil value])
                   ;              (dispatch [::main-events/changes-protection? true])
                   ;              (dispatch [::apps-events/validate-form])
                   ;              (reset! local-validate? true))
                   }]]]
  )

(defn versions-compare [versions]
  (let [versions-opt  (map (fn [version] {:key (first version)
                                          :value (:href (second version))
                                          :text (str "v" (first version))})
                        versions)
        tr            (subscribe [::i18n-subs/tr])
        compare-one   (reagent/atom nil)
        compare-two   (reagent/atom nil)
        compare?      (reagent/atom false)
        on-close-fn   #(do
                        (reset! compare? false))]
    (fn []
    [:span (@tr [:compare-version]) " "
     [ui/Dropdown {:selection   true
                   :compact     true
                   :placeholder (@tr [:select-version])
                   :value       @compare-one
                   :search      true
                   :on-change   (ui-callback/value
                                  (fn [value]
                                    (reset! compare-one value)
                                    (when @compare-two
                                      (reset! compare? true))))
                   :options     versions-opt}]
     " vs "
     [ui/Dropdown {:selection   true
                   :compact     true
                   :placeholder (@tr [:select-version])
                   :value       @compare-two
                   :search      true
                   :on-change   (ui-callback/value
                                  (fn [value]
                                    (reset! compare-two value)
                                    (when @compare-one
                                      (reset! compare? true))))
                   :options     versions-opt}]


     [ui/Modal {:open       @compare?
                :close-icon true
                :dimmer     "blurring"
                :on-close   on-close-fn}
      [version-comparison-modal @compare-one @compare-two]
      ]])))


(defn versions-table
  [versions current-version & {:keys [on-click]}]
  [ui/Table {:basic "very"}
   [ui/TableHeader
    [ui/TableRow
     [ui/TableHeaderCell {:width "1"} "Version"]
     [ui/TableHeaderCell {:width "1"} "Author"]
     [ui/TableHeaderCell {:width "14"} "Commit message"]]]
   [ui/TableBody
    (for [[i v] versions]
      (let [{:keys [href commit author]} v
            is-current? (= current-version href)]
        ^{:key (str "version" i)}
        [ui/TableRow (when is-current? {:active true})
         [ui/TableCell
          (if on-click
            [:a {:style    {:cursor "pointer"}
                 :on-click #(on-click i)}
             (str "v" i)]
            (str "v" i))
          (when is-current? " <<")]
         [ui/TableCell author]
         [ui/TableCell commit]]))]])


(defn versions []
  (let [show-versions?  (reagent/atom false)
        versions        (subscribe [::subs/versions])
        current-version (subscribe [::subs/module-content-id])]
    (fn []
      (when (seq @versions)
        [:div
         [show-versions show-versions?]
         (when (and (> (count @versions) 1) @show-versions?)
           [:div
            [versions-compare @versions]])
         (when @show-versions?
           [versions-table @versions @current-version
            :on-click #(dispatch [::events/get-module %])])]))))
