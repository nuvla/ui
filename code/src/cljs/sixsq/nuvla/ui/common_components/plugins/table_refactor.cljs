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
            [sixsq.nuvla.ui.utils.icons :as icons]
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
;
;(defn FormatFieldItem [selections-atom item field->view]
;  (let [view         (get field->view item)
;        tr           (subscribe [::i18n-subs/tr])
;        label-string (if (keyword? item) (or (@tr [item]) item) item)]
;    [ui/ListItem
;     [ui/ListContent
;      [ui/ListHeader {:style {:display :flex}}
;       [ui/Checkbox {:checked   (contains? @selections-atom item)
;                     :label     (if view (r/as-element [:label view]) label-string)
;                     :on-change (ui-callback/checked (fn [checked]
;                                                       (if checked
;                                                         (swap! selections-atom set/union #{item})
;                                                         (swap! selections-atom set/difference #{item}))))}]
;       #_[:label view]]]]))
;
;(defn format-field-list [available-fields-atom selections-atom field->view]
;  (let [items (sort available-fields-atom)]
;    (vec (concat [ui/ListSA]
;                 (map (fn [item] [FormatFieldItem selections-atom item field->view]) items)))))
;
;(defn SelectFieldsView
;  [{:keys [show? selected-id-sub selections-atom
;           selected-fields-sub available-fields
;           update-fn trigger title-tr-key
;           reset-to-default-fn field->view]}]
;  (let [tr (subscribe [::i18n-subs/tr])]
;    [ui/Modal
;     {:closeIcon true
;      :open      @show?
;      :on-close  #(reset! show? false)}
;     [uix/ModalHeader {:header (@tr [(or title-tr-key :fields)])}]
;     [ui/ModalContent
;      {:scrolling true}
;      [format-field-list available-fields selections-atom field->view]]
;     [ui/ModalActions
;      (when (fn? reset-to-default-fn)
;        [uix/Button
;         {:text     "Select default columns"
;          :on-click reset-to-default-fn}])
;      [uix/Button
;       {:text     (@tr [:cancel])
;        :on-click #(reset! show? false)}]
;      [uix/Button
;       {:text     (@tr [:update])
;        :primary  true
;        :on-click (fn []
;                    (reset! show? false)
;                    (if (fn? update-fn)
;                      (update-fn @selections-atom)
;                      (dispatch [::events/set-selected-fields @selections-atom])))}]]]))


(defn ColumnsSelectorButton
  [open-fn]
  (r/with-let [!hoverable (r/atom false)]
    [:div {:on-mouse-enter #(reset! !hoverable true)
           :on-mouse-leave #(reset! !hoverable false)
           :style          {:min-height "1.5em"}
           :on-click       open-fn}
     [:span {:title "Columns selector"
             :style {:float            :right
                     :border           2
                     :cursor           :pointer
                     :background-color "rgb(249, 250, 251)"
                     :border-width     "1px 1px 0px"
                     :border-color     "rgba(34,36,38,.1)"
                     :border-style     "solid"
                     :opacity          (if @!hoverable 1 0.2)}} [icons/ListIcon]]]))

(defn ColumnsSelectorModal
  [{:keys [::set-current-columns-fn] :as _control}]
  (r/with-let [open?    (r/atom false)
               open-fn  #(reset! open? true)
               close-fn #(reset! open? false)
               tr       (subscribe [::i18n-subs/tr])]
    (js/console.info "ColumnsSelectorModal" @open?)
    [ui/Modal {:close-icon true
               :open       @open?
               :trigger    (r/as-element [ColumnsSelectorButton open-fn])
               :on-close   close-fn}
     [uix/ModalHeader {:header "Select columnssss"}]
     [ui/ModalContent {:scrolling true}
      #_[format-field-list available-fields selections-atom field->view]]
     [ui/ModalActions
      #_(when (fn? reset-to-default-fn)
          [uix/Button
           {:text     "Select default columns"
            :on-click reset-to-default-fn}])
      (js/console.info tr #_(@tr [:cancel]))
      [uix/Button
       {:text     (@tr [:cancel])
        :on-click close-fn}]
      #_[uix/Button
         {:text     (@tr [:update])
          :primary  true
          :on-click (fn []
                      (close-fn)
                      #_(set-current-columns-fn))}]]]))

(defn Table
  [{:keys [::!current-columns ::!default-columns] :as control}]
  [:div
   [ColumnsSelectorModal control]
   [ui/Table {:attached true}
    [TableHeader control]
    [TableBody control]]])

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
