(ns sixsq.nuvla.ui.apps.views-versions
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as reagent]
            [sixsq.nuvla.ui.apps.events :as events]
            [sixsq.nuvla.ui.apps.subs :as subs]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn show-versions [show-versions?]
  (let [tr        (subscribe [::i18n-subs/tr])
        label     (@tr (if @show-versions? [:hide-versions] [:show-versions]))
        icon-name (if @show-versions? "caret down" "caret right")]
    [:a {:style    {:cursor "pointer"}
         :on-click #(reset! show-versions? (not @show-versions?))}
     [ui/Icon {:name icon-name}]
     label]))


(defn versions-table
  [versions current-version & {:keys [on-click]}]
  [ui/Table
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
         [ui/TableCell commit]]
        ))]])


(defn versions []
  (let [show-versions?  (reagent/atom false)
        versions        (subscribe [::subs/versions])
        current-version (subscribe [::subs/module-content-id])]
    (fn []
      (when (seq @versions)
        [:div
         [show-versions show-versions?]
         (when @show-versions?
           [versions-table @versions @current-version
            :on-click #(dispatch [::events/get-module %])])]
        ))))
