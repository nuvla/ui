(ns sixsq.nuvla.ui.plugins.table
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :refer [dispatch reg-event-fx subscribe]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(s/def ::pass-through-props (s/nilable map?))
(s/def ::table-props ::pass-through-props)
(s/def ::body-props ::pass-through-props)
(s/def ::header-props ::pass-through-props)

(s/def ::field-key          keyword?)
(s/def ::accessor          (s/nilable fn?))
(s/def ::cell              (s/nilable fn?))
(s/def ::header-content    (s/nilable (s/or :string string? :function fn?)))
(s/def ::header-cell-props (s/nilable map?))
(s/def ::footer-content    (s/nilable (s/or :string string? :function fn?)))
(s/def ::footer-cell-props (s/nilable map?))


(s/def ::field keyword?)
(s/def ::order #{"asc" "desc"})

(s/def ::ordering (s/nilable (s/keys :req-un [::field
                                              ::order])))

(defn build-ordering
  ([] (build-ordering {:field :created :order "desc"}))
  ([{:keys [field order]
     :or {field  :created
          order "desc"}}]
   {:field field :order order}))

(s/def ::column (s/keys
                :req-un [::field-key]
                :opt-un [::header
                         ::accessor
                         ::footer]))

(s/valid? ::column {:col-key :a})

(s/def ::columns (s/coll-of #(s/valid? ::column %) :min-count 1))

(s/def rows (s/coll-of any?))

(defn ordering->order-string [{field :field order :order}]
  (str (name field) ":" order))

(reg-event-fx
  ::sort
  (fn [{db :db} [_ {new-field    :field
                    db-path     :db-path
                    fetch-event :fetch-event}]]
    (let [ordering     (get db db-path)
          toggle-order {"asc" "desc" "desc" "asc"}
          order        (:order ordering)
          current-field (:field ordering)
          new-order    (if (= current-field new-field) (toggle-order order) order)
          old-value (get db db-path)]
      {:db (assoc db db-path {:field new-field
                              :order new-order})
       :fx [[:dispatch [fetch-event]]]})))

(defn Sort [{:keys [db-path field-key sortables
                    fetch-event full-sort]}]
  (when (and db-path
          (or full-sort (get sortables field-key)))
    (let [field  @(subscribe [::helpers/retrieve [db-path] :field])
          order @(subscribe [::helpers/retrieve [db-path] :order])]
      [uix/LinkIcon {:name (str "sort" (when
                                          (= field field-key)
                                          ({"asc" " ascending"
                                            "desc" " descending"} order)))
                     :on-click #(dispatch [::sort {:field        field-key
                                                   :db-path     db-path
                                                   :fetch-event fetch-event}])}])))

(defn Table
  [{:keys [cell-props columns rows
           row-click-handler row-props row-render
           sort-config] :as props}]
  (let [tr @(subscribe [::i18n-subs/tr])]
    [:<>
     [ui/Table (:table-props props)
      [ui/TableHeader (:header-props props)
       [ui/TableRow
        (for [col columns
              :when col
              :let [{:keys [field-key header-content header-cell-props]} col]]
          ^{:key field-key}
          [ui/TableHeaderCell
           (merge (:header cell-props) header-cell-props)
           [:div
            (cond
              (fn? header-content)
              (header-content)

              header-content
              header-content

              :else
              (or (tr [field-key]) field-key))
            [Sort (merge sort-config {:field-key field-key})]]])]]
      [ui/TableBody (:body-props props)
       (doall
         (for [row rows
               :let [id (:id row)]]
           (cond
             row-render
             ^{:key id}
             [row-render row]
             :else
             ^{:key id}
             [ui/TableRow (merge row-props {:on-click #(when row-click-handler (row-click-handler row))} (:table-row-prop row))
              (for [{:keys [field-key accessor cell cell-props]} columns
                    :let [cell-data ((or accessor field-key) row)]]
                ^{:key (str id "-" field-key)}
                [ui/TableCell
                 cell-props
                 (cond
                   cell (cell {:row-data row
                               :cell-data cell-data})
                   :else (str cell-data))])])))]]]))
