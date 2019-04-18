(ns sixsq.nuvla.ui.apps.views-versions
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as reagent]
            [sixsq.nuvla.ui.apps.events :as events]
            [sixsq.nuvla.ui.apps.subs :as subs]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [taoensso.timbre :as log]))


(defn is-latest? [module]
  (let [latest  (-> module :versions last :href)
        current (-> module :content :id)]
    (= latest current)))


(defn show-versions [show-versions?]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      (let [label     (@tr (if @show-versions? [:hide-versions] [:show-versions]))
            icon-name (if @show-versions? "caret down" "caret right")]
        [:a {:style    {:cursor "pointer"}
             :on-click #(reset! show-versions? (not @show-versions?))}
         [ui/Icon {:name icon-name}]
         label]))))


(defn versions []
  (let [module         (subscribe [::subs/module])
        show-versions? (reagent/atom false)]
    (fn []
      (let [versions         (:versions @module)
            is-versioned?    (not (empty? versions))
            versions-indexes (sort #(compare (second %2) (second %1)) (zipmap versions (range)))
            current          (-> @module :content :id)]
        (if (not (is-latest? @module))
          (dispatch [::events/set-version-warning])
          (dispatch [::events/clear-version-warning]))
        (when is-versioned?
          [:div
           [show-versions show-versions?]
           (when @show-versions?
             [ui/Table
              [ui/TableHeader
               [ui/TableRow
                [ui/TableHeaderCell {:width "1"} "Version"]
                [ui/TableHeaderCell {:width "1"} "Author"]
                [ui/TableHeaderCell {:width "14"} "Commit message"]]]
              [ui/TableBody
               (for [[v i] versions-indexes]
                 (let [{:keys [href commit author]} v
                       is-current? (= current href)]
                   ^{:key (str "version" i)}
                   [ui/TableRow (when is-current? {:active true})
                    [ui/TableCell [:a {:style    {:cursor "pointer"}
                                       :on-click #(dispatch [::events/get-module i])}
                                   (str "v" i)]
                     (when is-current? " <<")]
                    [ui/TableCell author]
                    [ui/TableCell commit]]
                   ))]])]
          )))))
