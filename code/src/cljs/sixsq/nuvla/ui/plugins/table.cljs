(ns sixsq.nuvla.ui.plugins.table
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-fx reg-sub subscribe]]
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

(s/def ::sort-direction (s/nilable (s/keys :req-un [::field
                                              ::order])))

(defn build-ordering
  ([] (build-ordering {:created "desc"}))
  ([ordering]
   ordering))

(s/def ::column (s/keys
                  :req-un [::field-key]
                  :opt-un [::header
                           ::accessor
                           ::footer]))

(s/valid? ::column {:col-key :a})

(s/def ::columns (s/coll-of #(s/valid? ::column %) :min-count 1))

(s/def rows (s/coll-of any?))

(defn ordering->order-string [ordering]
  (str/join "," (for [[field order] ordering]
                  (str (name field) ":" order))))

(defn- calc-new-ordering [{:keys [order sort-key]} ordering]
  (let [cleaned-ordering (remove #(= sort-key (first %)) ordering)]
        (js/console.error "cleaned-ordering" cleaned-ordering)
        (js/console.error "order" order)
        (case order
          "asc"  cleaned-ordering
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

(defn Sort [{:keys [db-path field-key sort-key fetch-event]
             :or {sort-key field-key}}]
  (when db-path
    (let [direction        @(subscribe [::sort-direction db-path sort-key])
          direction->class {"asc" " ascending"
                            "desc" " descending"}]
      [uix/LinkIcon {:name (str "sort" (direction->class direction))
                     :on-click #(dispatch [::sort {:field        sort-key
                                                   :direction   direction
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
              :let [{:keys [field-key header-content header-cell-props no-sort?]} col]]
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
                   cell (cell {:row-data row
                               :cell-data cell-data})
                   :else (str cell-data))])])))]]]))
