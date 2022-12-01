(ns sixsq.nuvla.ui.plugins.table
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :refer [dispatch reg-event-fx subscribe]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

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

(s/def ::column (s/keys
                :req-un [::field-key]
                :opt-un [::header
                         ::accessor
                         ::footer]))

(s/valid? ::column {:col-key :a})

(s/def ::columns (s/coll-of #(s/valid? ::column %) :min-count 1))

(s/def rows (s/coll-of any?))

(defn Table
  [{:keys [columns rows row-click-handler row-props row-render] :as props}]
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
           header-cell-props
           (cond
             (fn? header-content)
             (header-content)

             header-content
             header-content

             :else
             (or (tr [field-key]) "REMOVE ME" field-key))])]]
      [ui/TableBody (:body-props props)
       (doall
         (for [row rows
               :let [id (:id row)]]
           ^{:key id}
           (cond
             row-render [row-render row]
             :else
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
