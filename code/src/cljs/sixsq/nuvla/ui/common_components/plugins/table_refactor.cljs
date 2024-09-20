(ns sixsq.nuvla.ui.common-components.plugins.table-refactor
  (:require [cljs.spec.alpha :as s]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch inject-cofx reg-event-db
                                   reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.cimi.views :refer [SelectFieldsView]]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.utils :refer [get-query-param]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.tooltip :as tt]))

(defn TableHeaderCell
  [control !visible-columns !visible-column?-fn column]
  (js/console.info "Render TableHeaderCell " (::field-key column))
  (when @(!visible-column?-fn (::field-key column))
    (r/with-let [set-columns-fn (::set-current-columns-fn control)]
      [ui/TableHeaderCell {:class ["show-child-on-hover"]}
       (::header-content column)
       [:span {:style {:margin-left "0.8rem"}}
        (when (> (count @!visible-columns) 1)
          [uix/LinkIcon {:color    "red"
                        :name     "remove circle"
                        :on-click #(set-columns-fn
                                     (vec (remove (fn [field-key]
                                                    (= field-key (::field-key column)))
                                                  @!visible-columns)))
                        :class    :toggle-invisible-on-parent-hover}])]])))

(defn TableHeader
  [control !visible-columns !visible-column?-fn]
  (js/console.info "Render TableHeader")
  [ui/TableHeader
   [ui/TableRow
    (let [columns @(::!columns control)]
      (for [column columns]
        ^{:key (str "header-column-" (::field-key column))}
        [TableHeaderCell control !visible-columns !visible-column?-fn column]))]])

(defn TableCell
  [_control !visible-column?-fn row column]
  (js/console.info "Render TableCell " (::field-key column))
  (when @(!visible-column?-fn (::field-key column))
    [ui/TableCell
    ((::field-key column) row)]))

(defn TableRow
  [control !visible-column?-fn row]
  (js/console.info "Render TableRow " row)
  (r/with-let [!columns (::!columns control)]
    [ui/TableRow
     (for [column @!columns]
       ^{:key (str "row-" (:id row) "-column-" (::field-key column))}
       [TableCell control !visible-column?-fn row column])]))

(defn TableBody
  [control !visible-column?-fn]
  (js/console.info "Render TableBody")
  [ui/TableBody
   (for [row @(::!rows control)]
     ^{:key (str "row-" (:id row))}
     [TableRow control !visible-column?-fn row])])

(defn Table
  [{:keys [::!current-columns ::!default-columns] :as control}]
  (r/with-let [!visible-columns     (r/track #(or @!current-columns @!default-columns))
               !visible-columns-set (r/track #(set @!visible-columns))
               !visible-column?-fn  (fn [k] (r/track #(contains? @!visible-columns-set k)))]
    (js/console.info "Render Table")
    [ui/Table
     [TableHeader control !visible-columns !visible-column?-fn]
     [TableBody control !visible-column?-fn]]))

(reg-event-db
  ::set-current-columns-fn
  (fn [db [_ v]]
    (assoc db ::current-columns v)))

(reg-sub
  ::current-columns
  :-> ::current-columns)

(defn TableControllerReal
  [reset-atom]
  (js/console.info "Render TableControllerReal")
  (r/with-let [columns          [{::field-key      :id
                                  ::header-content "Id"}
                                 {::field-key      :name
                                  ::header-content "Name"}
                                 {::field-key      :age
                                  ::header-content "Age"}]
               !current-columns (r/atom nil)
               ;!current-columns (subscribe [::current-columns])
               control          {::!columns               (r/atom columns)
                                 ::!rows                  (r/atom [{:id 1, :name "hello"}
                                                                   {:id 2}])
                                 ::!default-columns       (r/atom [:id :name])
                                 ::set-current-columns-fn #(reset! !current-columns %)
                                 ;::set-current-columns-fn #(dispatch [::set-current-columns-fn %])
                                 ::!current-columns       !current-columns}]
    [:div
     [ui/Button {:on-click #(swap! reset-atom inc)} "Reset"]
     [ui/Button {:on-click #(reset! (::!rows control) [{:id 4} {:id 5}])} "Add row"]
     [Table control]]))

(defn TableController
  []
  (js/console.info "Render TableController")
  (r/with-let [reset-atom (r/atom 0)]
    ^{:key @reset-atom}
    [TableControllerReal reset-atom]))

;; table
;; rows
;; columns
;; sortable
;; add remove columns
;; dynamic
