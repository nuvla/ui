(ns sixsq.nuvla.ui.apps.views-versions
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as reagent]
            [sixsq.nuvla.ui.apps.events :as events]
            [sixsq.nuvla.ui.apps.subs :as subs]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [taoensso.timbre :as log]))


(defn show-versions [show-versions?]
  (let [tr        (subscribe [::i18n-subs/tr])
        label     (@tr (if @show-versions? [:hide-versions] [:show-versions]))
        icon-name (if @show-versions? "caret down" "caret right")]
    [:a {:style    {:cursor "pointer"}
         :on-click #(reset! show-versions? (not @show-versions?))}
     [ui/Icon {:name icon-name}]
     label]))


(defn version-comparison-modal
  [left right version-left version-right]
  (if (and left right)
    [ui/ModalContent {:scrolling true}
     [ui/DiffViewer {:old-value   (general-utils/edn->json left)
                     :new-value   (general-utils/edn->json right)
                     :split-view  true
                     :left-title  (str "v" version-left)
                     :right-title (str "v" version-right)}]]
    [ui/Container
     [ui/Loader {:active   true
                 :inverted true}]]))


(defn versions-compare [versions]
  (let [versions-opt         (map (fn [version] {:key   (first version)
                                                 :value (first version)
                                                 :text  (str "v" (first version))})
                                  versions)
        tr                   (subscribe [::i18n-subs/tr])
        compare-one          (reagent/atom nil)
        compare-two          (reagent/atom nil)
        compare?             (reagent/atom false)
        module               (subscribe [::subs/module])
        module-compare-left  (subscribe [::subs/compare-module-left])
        module-compare-right (subscribe [::subs/compare-module-right])
        on-close-fn          #(do
                                (reset! compare? false))]
    (fn []
      (let [module-id (:id @module)]
        (when module-id
          [:span (@tr [:compare-version]) " "
           [ui/Dropdown {:selection   true
                         :compact     true
                         :placeholder " "
                         :value       @compare-one
                         :search      true
                         :on-change   (ui-callback/value
                                        (fn [value]
                                          (reset! compare-one value)
                                          (dispatch [::events/get-module-to-compare (str module-id "_" value) "left"])
                                          (when @compare-two
                                            (reset! compare? true))))
                         :options     versions-opt}]
           " vs "
           [ui/Dropdown {:selection   true
                         :compact     true
                         :placeholder " "
                         :value       @compare-two
                         :search      true
                         :on-change   (ui-callback/value
                                        (fn [value]
                                          (reset! compare-two value)
                                          (dispatch [::events/get-module-to-compare (str module-id "_" value) "right"])
                                          (when @compare-one
                                            (reset! compare? true))))
                         :options     versions-opt}]

           [ui/Modal {:open     @compare?
                      :dimmer   "blurring"
                      :on-close on-close-fn
                      :size     "large"}
            [version-comparison-modal @module-compare-left @module-compare-right @compare-one @compare-two]]])))))


(defn versions-table
  [versions current-version & {:keys [on-click]}]
  [ui/Table {:basic "very"}
   [ui/TableHeader
    [ui/TableRow
     [ui/TableHeaderCell {:width "1"} "Version"]
     [ui/TableHeaderCell {:width "1"} "Published?"]
     [ui/TableHeaderCell {:width "1"} "Author"]
     [ui/TableHeaderCell {:width "13"} "Commit message"]]]
   [ui/TableBody
    (for [[i v] versions]
      (let [{:keys [href commit author published]} v
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
         [ui/TableCell (when (true? published) [ui/Icon {:name "check" :color "teal"}])]
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
