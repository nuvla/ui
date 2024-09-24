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

(defn !visible-columns-fn
  [{:keys [::!current-columns ::!default-columns] :as _control}]
  (r/track #(or @!current-columns @!default-columns)))

(defn !columns-by-key-fn
  [{:keys [::!columns] :as _control}]
  (r/track #(into {} (map (juxt ::field-key identity) @!columns))))

(defn DeleteColumn
  [{:keys [::set-columns-fn] :as control} {:keys [::field-key ::no-delete] :as _column}]
  (when (and (> (count @(!visible-columns-fn control)) 1)
             (not no-delete))
    [:span {:style {:margin-left "0.8rem"}}
     [uix/LinkIcon {:color    "red"
                    :name     "remove circle"
                    :on-click #(set-columns-fn
                                 (vec (remove (fn [fk] (= fk field-key))
                                              @!visible-columns-fn)))
                    :class    :toggle-invisible-on-parent-hover}]]))

(defn TableHeaderCell
  [control column]
  (r/with-let [field-key (::field-key column)]
    (js/console.info "Render TableHeaderCell " field-key)
    [ui/TableHeaderCell {:class ["show-child-on-hover"]}
     (::header-content column)
     [DeleteColumn control column]]))

(defn TableHeader
  [control]
  (js/console.info "Render TableHeader")
  (r/with-let [!visible-columns (!visible-columns-fn control)
               !columns-by-key  (!columns-by-key-fn control)]
    [ui/TableHeader
     [ui/TableRow
      (js/console.info @!columns-by-key)
      (doall
        (for [visible-column @!visible-columns]
          ^{:key (str "header-column-" visible-column)}
          [TableHeaderCell control (get @!columns-by-key visible-column)]))]]))

(defn TableCell
  [_control row column]
  (js/console.info "Render TableCell " (::field-key column))
  [ui/TableCell
   ((::field-key column) row)])

(defn TableRow
  [control row]
  (js/console.info "Render TableRow " row)
  (r/with-let [visible-columns (!visible-columns-fn control)
               !columns-by-key (!columns-by-key-fn control)]
    [ui/TableRow
     (doall
       (for [visible-column @visible-columns]
         (let [column (get @!columns-by-key visible-column)]
           ^{:key (str "row-" (:id row) "-column-" visible-column)}
           [TableCell control row column])))]))

(defn TableBody
  [control]
  (js/console.info "Render TableBody")
  [ui/TableBody
   (for [data-row @(::!data control)]
     ^{:key (str "row-" (:id data-row))}
     [TableRow control data-row])])

(defn Table
  [{:keys [::!current-columns ::!default-columns] :as control}]
  [ui/Table
   [TableHeader control]
   [TableBody control]])

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
                                  ::header-content "Id"
                                  ::no-delete      true}
                                 {::field-key      :name
                                  ::header-content "Name"}
                                 {::field-key      :age
                                  ::header-content "Age"}]
               !current-columns (r/atom nil)
               ;!current-columns (subscribe [::current-columns])
               control          {::!columns               (r/atom columns)
                                 ::!data                  (r/atom [{:id 1, :name "hello"}
                                                                   {:id 2}])
                                 ::!default-columns       (r/atom [:id :name])
                                 ::set-current-columns-fn #(reset! !current-columns %)
                                 ;::set-current-columns-fn #(dispatch [::set-current-columns-fn %])
                                 ::!current-columns       !current-columns}]
    [:div
     [ui/Button {:on-click #(swap! reset-atom inc)} "Reset"]
     [ui/Button {:on-click #(reset! (::!data control) [{:id 1, :name "hello"} {:id 5}])} "Add row"]
     [Table control]]))

(defn TableController
  []
  (r/with-let [reset-atom (r/atom 0)]
    ^{:key @reset-atom}
    [TableControllerReal reset-atom]))

;; table
;; rows
;; columns
;; sortable
;; add remove columns
;; dynamic


;; table
;; accept control map
;; delete column icon
;; select columns modal
;; sorting
;; filtering
;; drag columns
;; selectable rows
;; pagination
