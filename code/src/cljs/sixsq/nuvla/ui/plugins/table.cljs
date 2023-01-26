(ns sixsq.nuvla.ui.plugins.table
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-fx reg-sub subscribe]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(s/def ::pass-through-props (s/nilable map?))
(s/def ::table-props ::pass-through-props)
(s/def ::body-props ::pass-through-props)
(s/def ::header-props ::pass-through-props)
(s/def ::header-cell-props ::pass-through-props)
(s/def ::wide? (s/nilable boolean?))

(s/def ::field keyword?)
(s/def ::order #{"asc" "desc"})

(s/def ::sort-direction (s/nilable (s/keys :req-un [::field ::order])))

(defn build-ordering
  ([] (build-ordering {:created "desc"}))
  ([ordering]
   ordering))

(defn ordering->order-string [ordering]
  (str/join "," (for [[field order] ordering]
                  (str (name field) ":" order))))

(s/def ::field-key keyword?)
(s/def ::accessor (s/nilable (s/or :function fn? :keyword keyword?)))
(s/def ::header-content (s/nilable any?))
(s/def ::sort-key (s/nilable (s/or :keyword keyword? :string string?)))
(s/def ::no-sort (s/nilable boolean?))
(s/def ::cell (s/nilable fn?))

(s/def ::column (s/nilable (s/keys
                             :opt-un [::field-key
                                      ::header-content
                                      ::accessor
                                      ::sort-key
                                      ::no-sort
                                      ::cell])))

(s/def ::columns (s/coll-of #(s/valid? ::column %)))

(s/def ::rows (s/nilable (s/coll-of map?)))


(defn- calc-new-ordering [{:keys [order sort-key]} ordering]
  (let [cleaned-ordering (remove #(= sort-key (first %)) ordering)]
    (case order
      "asc" cleaned-ordering
      "desc" (cons [sort-key "asc"] cleaned-ordering)
      (cons [sort-key "desc"] cleaned-ordering))))

(reg-event-fx
  ::sort
  (fn [{db :db} [_ {sort-key       :field
                    sort-direction :direction
                    db-path        :db-path
                    fetch-event    :fetch-event}]]
    {:db (update db db-path (partial calc-new-ordering {:sort-key sort-key :order sort-direction}))
     :fx [[:dispatch [fetch-event]]]}))

(reg-sub
  ::sort-direction
  (fn [db [_ db-path sort-key]]
    (let [ordering (get db db-path)]
      (some #(when (= sort-key (first %)) (second %)) ordering))))

(defn Sort [{:keys [db-path field-key sort-key fetch-event]}]
  (when db-path
    (let [sort-key         (or sort-key field-key)
          direction        @(subscribe [::sort-direction db-path sort-key])
          direction->class {"asc"  " ascending"
                            "desc" " descending"}]
      [uix/LinkIcon {:name     (str "sort" (direction->class direction))
                     :on-click #(dispatch [::sort {:field        sort-key
                                                   :direction   direction
                                                   :db-path     db-path
                                                   :fetch-event fetch-event}])}])))

(defn Table
  "Expects a single config map with a required `:rows` vector of documents.
   If no column definitions are passed through `:columns`, the first document's
   keys are used to construct a table header and columns and horizontally
   scrollable table is shown.

   You probably want to provide `:columns` vector of column definitions:
    - `:field-key` is used
        1. for accessing a documents data to show in a specific column, if no `:accessor` fn is provided,
        2. as column header translation key or label, if no `:header-content` fn or element is provided,
        3. as the document field to sort on, if sort is enabled by providing a `:sort-config` and no
              `:sort-key` is provided or sort is disabled with `:no-sort?` for this column,
    - `:header-content` can be passed in for custom column header, else `:field-key` is used,
    - `:accessor` custom fn to accessing data for this column, else `:field-key` is used,
    - `:cell` custom render fn for row cell, specific row document and cell-data is passed, else raw data is shown,
    - `:sort-key` custom sort key for this column, else `:field-key` is used,
    - `:no-sort?` disables sort for this column,

    To enable sort, provide a sort-config:
     - `:db-path` tells the table where the currently applied ordering is stored,
     - `:fetch-event` dispatch event after a new ordering is applied,
    Enabled sort for all columns, column wise disabling via `:no-sort?` key in column definition.

    A custom render function for a whole row can be provided with:
    - `:row-render`
    This overrides any `:cell` custom render function passed or props passed to column definitions.
"
  [{:keys [cell-props columns rows
           row-click-handler row-props row-render
           sort-config]
    :as   props}]
  (let [tr            @(subscribe [::i18n-subs/tr])
        columns       (or columns (map (fn [[k _]] {:field-key k}) (first rows)))]
    [:div {:style {:overflow :auto
                   :padding 0}}
     [ui/Table (:table-props props)
      [ui/TableHeader (:header-props props)
       [ui/TableRow
        (for [col columns
              :when col
              :let [{:keys [field-key header-content header-cell-props no-sort?]} col]]
          ^{:key (or field-key (random-uuid))}
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
            (when (and
                    sort-config
                    (not no-sort?))
              [Sort (merge
                      sort-config
                      (select-keys col [:field-key :disable-sort :sort-key]))])]])]]
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
                   cell [cell {:row-data  row
                               :cell-data cell-data}]
                   :else (str cell-data))])])))]]]))


(s/fdef Table :args (s/cat :opts (s/keys
                                   :req-un [::rows]
                                   :opt-un [::columns
                                            ::table-props
                                            ::header-cell-props
                                            ::header-props
                                            ::body-props
                                            ::cell-props
                                            ::row-render
                                            ::wide?])))
