(ns sixsq.nuvla.ui.common-components.plugins.table
  (:require [cljs.spec.alpha :as s]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch inject-cofx reg-event-db
                                   reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.pages.cimi.views :refer [SelectFieldsView]]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.utils :refer [get-query-param]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.tooltip :as tt]))

(s/def ::pass-through-props (s/nilable map?))
(s/def ::table-props ::pass-through-props)
(s/def ::body-props ::pass-through-props)
(s/def ::header-props ::pass-through-props)
(s/def ::header-cell-props ::pass-through-props)
(s/def ::wide? (s/nilable boolean?))

(s/def ::field keyword?)
(s/def ::order #{"asc" "desc"})

(s/def ::sort-direction
  (s/nilable (s/keys :req-un [::field ::order])))

(defn build-ordering
  ([] (build-ordering [[:created "desc"]]))
  ([ordering]
   ordering))

(defn ordering->order-string [ordering]
  (str/join "," (for [[field order] ordering]
                  (str (name field) ":" (name order)))))

(s/def ::field-key keyword?)
(s/def ::accessor (s/nilable (s/or :function fn? :keyword keyword?)))
(s/def ::header-content (s/nilable any?))
(s/def ::sort-key (s/nilable (s/or :keyword keyword? :string string?)))
(s/def ::sort-value-fn (s/nilable fn?))
(s/def ::no-sort (s/nilable boolean?))
(s/def ::cell (s/nilable fn?))

(s/def ::column (s/nilable (s/keys
                             :opt-un [::field-key
                                      ::header-content
                                      ::accessor
                                      ::sort-key
                                      ::sort-value-fn
                                      ::no-sort
                                      ::cell])))

(s/def ::columns (s/coll-of #(s/valid? ::column %)))

(s/def ::rows (s/nilable (s/coll-of map?)))

;;;; Additional Features ;;;;
;; sorting


(defn- calc-new-ordering [{:keys [order sort-key sort-value-fn]} ordering]
  (let [cleaned-ordering (remove #(= sort-key (first %)) ordering)]
    (case order
      "asc" cleaned-ordering
      "desc" (cons [sort-key "asc" sort-value-fn] cleaned-ordering)
      (cons [sort-key "desc" sort-value-fn] cleaned-ordering))))

(reg-event-fx
  ::sort
  (fn [{db :db} [_ {sort-key       :field
                    sort-value-fn  :value-fn
                    sort-direction :direction
                    db-path        :db-path
                    fetch-event    :fetch-event}]]
    {:db (update db db-path (partial calc-new-ordering {:sort-key sort-key :sort-value-fn sort-value-fn :order sort-direction}))
     :fx [(when fetch-event [:dispatch fetch-event])]}))

(reg-sub
  ::sort-direction
  (fn [db [_ db-path sort-key]]
    (let [ordering (get db db-path)]
      (some #(when (= sort-key (first %)) (second %)) ordering))))

(defn SortIcon [direction]
  (let [direction->class {"asc"  " ascending"
                          "desc" " descending"}]
    (when direction
      [uix/LinkIcon {:name (str "sort" (direction->class direction))}])))

;; Bulk selection, table plugin args
(s/def ::key keyword?)
(s/def ::name string?)
(s/def ::menuitem any?)
(s/def ::event (s/or :k (s/* keyword?) :fn fn?))
(s/def ::icon (s/nilable fn?))
(s/def ::total-count-sub-key (s/* keyword?))
(s/def ::resources-sub-key (s/* keyword?))

(s/def ::bulk-action (s/and (s/keys :opt-un [::name ::event ::icon])
                            (fn [m] (or (:key m) (:menuitem m)))))

(s/def ::bulk-actions (s/nilable (s/coll-of ::bulk-action :kind vector?)))
(s/def ::select-db-path (s/* keyword?))
(s/def ::rights-needed keyword?)
(s/def ::select-label-accessor (s/nilable fn?))
(s/def ::select-config (s/nilable (s/keys :req-un [::select-db-path
                                                   ::total-count-sub-key ::resources-sub-key]
                                          :opt-un [::bulk-actions ::rights-needed ::select-label-accessor])))
;; Bulk selection db entries
(s/def ::bulk-edit-success-msg (s/nilable string?))
(s/def ::select-all? (s/nilable boolean?))
(s/def ::selected-set (s/nilable set?))

(defn build-bulk-edit-spec
  []
  {::select-all?           false
   ::selected-set          #{}
   ::bulk-edit-success-msg nil})

(reg-event-db
  ::reset-bulk-edit-selection
  (fn [db [_ db-path]]
    (update-in db db-path merge {::select-all?  false
                                 ::selected-set #{}})))

(reg-event-db
  ::set-bulk-edit-success-message
  (fn [db [_ msg db-path]]
    (assoc-in db (conj db-path ::bulk-edit-success-msg) msg)))

(reg-sub
  ::bulk-edit-success-message-sub
  (fn [db [_ db-path]]
    (get-in db (conj db-path ::bulk-edit-success-msg))))

(s/def ::selection-status #{:all :page
                            :some :none})


(defn build-bulk-filter
  [{:keys [::select-all? ::selected-set]} filter-string]
  (if select-all?
    filter-string
    (general-utils/filter-eq-ids selected-set)))

(defn- all-page-selected?
  [selected-set visible-deps-ids-set]
  (set/superset? selected-set visible-deps-ids-set))

(defn- visible-ids
  [resources]
  (set (map :id resources)))

(defn- is-selected?
  [selected-set id]
  (contains? selected-set id))

(defn- get-in-db
  ([db db-path k]
   (get-in-db db db-path k nil))
  ([db db-path k default]
   (get-in db (conj (or db-path []) k) default)))

(reg-event-fx
  ::init-pre-selection
  (fn [{{:keys [current-route]} :db} [_ select-all-query-param selection-db-path]]
    (let [query-key (or select-all-query-param :select)]
      (when (= "all" (get-query-param current-route query-key))
        {:fx [[:dispatch [::select-all selection-db-path]]
              [:dispatch [::routing-events/remove-query-param query-key]]]}))))

(reg-event-fx
  ::select-all-in-page
  (fn [{db :db} [_ {:keys [resources db-path]}]]
    (let [selected-set       (get-in-db db db-path ::selected-set #{})
          visible-dep-ids    (visible-ids resources)
          all-page-selected? (all-page-selected? selected-set visible-dep-ids)
          new-selected-set   (if all-page-selected?
                               (set/difference selected-set
                                               visible-dep-ids)
                               (set/union visible-dep-ids
                                          selected-set))]
      {:db (-> db
               (assoc-in (conj (or db-path []) ::selected-set) new-selected-set)
               (assoc-in (conj (or db-path []) ::select-all?) false))})))

(reg-event-db
  ::deselect-all
  (fn [db [_ db-path]]
    (let [db-path-prefix (or db-path [])]
      (-> db
          (assoc-in (conj db-path-prefix ::selected-set) #{})
          (assoc-in (conj db-path-prefix ::select-all?) false)))))

(reg-event-db
  ::select-id
  (fn [db [_ id db-path resource-ids]]
    (let [selected-set (get-in-db db db-path ::selected-set #{})
          select-all?  (get-in-db db db-path ::select-all?)
          new-set      (cond select-all?
                             (disj (set resource-ids) id)

                             (is-selected? selected-set id)
                             (disj selected-set id)

                             :else
                             (conj selected-set id))]
      (-> db
          (assoc-in (conj (or db-path []) ::selected-set) new-set)
          (assoc-in (conj (or db-path []) ::select-all?) false)))))

(reg-event-db
  ::select-all
  (fn [db [_ db-path current-status]]
    (-> db
        (assoc-in (conj (or db-path []) ::select-all?) (not= current-status :all))
        (assoc-in (conj (or db-path []) ::selected-set) #{}))))

(reg-sub
  ::bulk-update-modal
  (fn [db [_ db-path]]
    (get-in-db db db-path ::bulk-update-modal)))

(reg-sub
  ::selected-set-sub
  (fn [db [_ db-path]]
    (when db-path (get-in-db db db-path ::selected-set))))

(reg-sub
  ::select-all?-sub
  (fn [db [_ db-path]]
    (when db-path (get-in-db db db-path ::select-all?))))


(reg-sub
  ::is-all-page-selected?
  (fn [[_ db-path resources-sub-key rights-needed]]
    [(subscribe [::selected-set-sub db-path])
     (when resources-sub-key (subscribe [::editable-resources-on-page resources-sub-key rights-needed]))])
  (fn [[selected-set resources]]
    (and selected-set resources (all-page-selected? selected-set (visible-ids resources)))))

(reg-sub
  ::off-page-selected-set
  (fn [[_ db-path resources-sub-key]]
    [(subscribe [::selected-set-sub db-path])
     (subscribe resources-sub-key)])
  (fn [[selected-set resources]]
    (set/difference selected-set (visible-ids resources))))

(reg-sub
  ::on-page-selected-set
  (fn [[_ db-path resources-sub-key]]
    [(subscribe [::selected-set-sub db-path])
     (subscribe resources-sub-key)])
  (fn [[selected-set resources]]
    (set/intersection selected-set (visible-ids resources))))

(reg-sub
  ::selection-status
  (fn [[_ db-path resources-sub-key]]
    [(subscribe [::selected-set-sub db-path])
     (subscribe [::select-all?-sub db-path])
     (subscribe [::is-all-page-selected? db-path resources-sub-key])
     (subscribe resources-sub-key)])
  (fn [[selected-set select-all? is-all-page-selected? resources] [_ _ _ total-count]]
    (let [number-of-selected (count selected-set)
          visible-on-page    (count resources)]
      (cond
        (or select-all? (= total-count number-of-selected))
        :all

        (zero? number-of-selected)
        :none

        (and
          (<= 0 number-of-selected)
          (= number-of-selected visible-on-page)
          is-all-page-selected?)
        :page

        (and
          (< visible-on-page number-of-selected)
          is-all-page-selected?)
        :page-plus

        (pos? number-of-selected)
        :some

        :else
        :none))))

(reg-sub
  ::editable-resources-on-page
  (fn [[_ resources-sub-key _]]
    (subscribe resources-sub-key))
  (fn [resources [_ _ rights-needed]]
    (if-not rights-needed resources
                          (filter (partial general-utils/can-operation? rights-needed) resources))))

(defn CellCheckbox
  [{:keys [id selected-all-sub selected-set-sub db-path
           resources-sub-key edge-name idx]}]
  (let [resources (subscribe resources-sub-key)
        select-fn (fn [] (dispatch [::select-id id db-path (map :id @resources)]))
        checked?  (or @selected-all-sub (is-selected? @selected-set-sub id))]
    [ui/TableCell {:aria-label (str "select row " idx)
                   :on-click   (fn [event]
                                 (select-fn)
                                 (.stopPropagation event))}
     [ui/Checkbox {:id         idx
                   :aria-label (str "select " edge-name)
                   :checked    checked?}]]))


(defn HeaderCellCeckbox
  [{:keys [db-path resources-sub-key page-selected?-sub]}]
  (let [resources @(subscribe resources-sub-key)]
    [ui/Checkbox {:aria-label "select all on page"
                  :checked    @page-selected?-sub
                  :on-click   #(dispatch [::select-all-in-page {:resources resources :db-path db-path}])}]))

(defn BulkActionBar
  [{:keys [selected-set-sub total-count-sub-key selected-all-sub disabled-tooltip
           db-path bulk-actions resources-sub-key selectable? resource-type]}]
  (when resources-sub-key
    (let [tr                        (subscribe [::i18n-subs/tr])
          rows                      @(subscribe resources-sub-key)
          total-count               (subscribe total-count-sub-key)
          selection-status          (subscribe [::selection-status db-path resources-sub-key @total-count])
          on-page-selected          (count @(subscribe [::on-page-selected-set db-path resources-sub-key]))
          off-page-selected         (if @selected-all-sub
                                      (- @total-count (count rows))
                                      (count @(subscribe [::off-page-selected-set db-path resources-sub-key])))
          off-page-selection?       (pos? off-page-selected)
          off-page-selection-text   (when off-page-selection?
                                      (str/join " " [off-page-selected
                                                     (@tr [:on-other-pages])]))
          selected-all-text         (when (= :all @selection-status)
                                      (str/join " " [(str/capitalize (@tr [:all]))
                                                     @total-count
                                                     (@tr [(or resource-type :items)])
                                                     (@tr [:are-selected])
                                                     (when off-page-selection-text
                                                       (str "(" off-page-selection-text ")"))]))
          on-page-selection?        (pos? on-page-selected)
          manual-selection-text     (str/join " " [(when (#{:all :page :page-plus} @selection-status)
                                                     (str/capitalize (@tr [:all])))
                                                   (when on-page-selection?
                                                     (str on-page-selected
                                                          " "
                                                          (@tr [(or resource-type :items)])
                                                          " "
                                                          (@tr [:on-this-page])
                                                          " "
                                                          (@tr [:are-selected])))
                                                   (when off-page-selection?
                                                     (if on-page-selection?
                                                       (str "(" off-page-selection-text ")")
                                                       (str off-page-selected " " (@tr [:on-other-pages]) " " (@tr [:are-selected]))))])
          button-text               (if (= @selection-status :all)
                                      (@tr [:clear-selection])
                                      (str (@tr [:select-all]) " " @total-count))
          payload                   {:select-all   @selected-all-sub
                                     :selected-set @selected-set-sub}
          nothing-selected?         (or (= :none @selection-status) (zero? @total-count))
          bulk-edit-success-message (subscribe [::bulk-edit-success-message-sub db-path])]
      [:div
       {:style {:position :sticky :top "39px" :z-index 998}}
       (when (seq bulk-actions)
         [:div {:style {:display          :flex
                        :border           "1px solid rgb(230 230 230)"
                        :border-radius    "0.28rem"
                        :background-color "rgb(249 250 251)"
                        :justify-content  :space-between
                        :align-items      :center
                        :height           "40px"
                        :gap              "1rem"}}
          [:div
           [ui/Menu {:style     {:border           :none
                                 :box-shadow       :none
                                 :background-color :transparent}
                     :stackable true}
            (doall
              (for [[idx action] (map-indexed vector bulk-actions)
                    :let [{:keys [key name event icon menuitem]} action]]
                (or menuitem
                    ^{:key key}
                    [ui/Popup {:trigger
                               (r/as-element
                                 [:div
                                  [uix/HighlightableMenuItem
                                   {:disabled          (or nothing-selected? disabled-tooltip)
                                    :query-param-value key
                                    :on-click          (fn []
                                                         (if (fn? event) (event payload)
                                                                         (dispatch event)))
                                    :key               idx}
                                   (when icon [icon])
                                   name]])
                               :basic    true
                               :disabled (and (not disabled-tooltip) (not= :none @selection-status))
                               :content  (or disabled-tooltip (@tr [:select-at-least-one-item]))}])))]]
          [:div {:style       {:padding-right "1rem"}
                 :data-testid "bulk-edit-success-message"}
           @bulk-edit-success-message]])
       [:div
        {:style {:height           "2rem"
                 :padding-left     "1rem"
                 :border           "1px solid rgb(230 230 230)"
                 :border-radius    "0.28rem"
                 :background-color "#f0eeef"}}
        [:div {:style {:display         :flex
                       :justify-content :center
                       :align-items     :center}}
         [:span (or selected-all-text manual-selection-text)]
         [:button {:style    {:width "120px" :text-align :center :border :none}
                   :on-click (fn [] (dispatch [::select-all db-path @selection-status]))
                   :class    [:select-all]}
          button-text]]]])))


(def local-storage-key
  "nuvla.ui.table.column-configs")

(reg-event-fx
  ::remove-col
  (fn [{{:keys [::current-cols]} :db} [_ col-key db-path]]
    (let [cur-cols (get current-cols db-path)]
      (when (< 1 (count cur-cols))
        {:fx [[:dispatch [::set-current-cols
                          (filterv #(not= col-key %) cur-cols)
                          db-path]]]}))))

(def default-columns
  [:id :name :description :created :updated])

(reg-event-fx
  ::add-col
  (fn [{{:keys [::current-cols
                ::default-cols]} :db} [_ {:keys [col-key position db-path]}]]
    (let [cols (get current-cols db-path
                    (get default-cols db-path))]
      (when-not (some #{col-key} cols)
        (let [new-cols (if position
                         (vec (concat (take position cols)
                                      [col-key]
                                      (drop position cols)))
                         (into cols [col-key]))]
          {:fx [[:dispatch
                 [::set-current-cols
                  new-cols
                  db-path]]]})))))

(reg-event-fx
  ::reset-current-cols
  (fn [{{defaults ::default-cols} :db} [_ db-path]]
    {:fx [[:dispatch [::set-current-cols (get defaults db-path default-columns) db-path]]]}))

(reg-event-fx
  ::set-current-cols
  [(inject-cofx :storage/get {:name local-storage-key})]
  (fn [{storage                  :storage/get
        {:keys [::default-cols]} :db} [_ cols db-path]]
    (let [defaults (get default-cols db-path)
          new-cols (cond
                     (sequential? cols)
                     cols

                     (= (set defaults) cols)
                     defaults

                     :else
                     (sort
                       (fn [k1 k2]
                         (let [pos-fn (fn [k]
                                        (let [po (.indexOf (or defaults []) k)]
                                          (if (neg? po) 100 po)))]
                           (- (pos-fn k1) (pos-fn k2))))
                       (vec cols)))]
      {:fx          [[:dispatch [::store-cols new-cols db-path]]]
       :storage/set {:session? false
                     :name     local-storage-key
                     :value    (merge (edn/read-string storage) {db-path new-cols})}})))

(reg-event-fx
  ::select-fields
  (fn [{{:keys [::current-cols]} :db} [_ fields db-path]]
    ;; on fields selection, compute new cols by keeping the ordering of existing cols and by appending
    ;; remaining cols at the end
    (let [cur-cols (get current-cols db-path)
          cols     (into (filterv #(contains? fields %) cur-cols)
                         (set/difference fields (set cur-cols)))]
      {:fx [[:dispatch [::set-current-cols cols db-path]]]})))

(reg-event-fx
  ::move-col
  (fn [{{:keys [::current-cols]} :db} [_ col-key dest-col-key db-path]]
    (let [cur-cols (get current-cols db-path)
          cols-    (filterv #(not= col-key %) cur-cols)
          dest-idx (.indexOf cols- dest-col-key)
          new-cols (let [[l r] (split-at dest-idx cols-)]
                     (concat l [col-key] r))]
      {:fx [[:dispatch [::set-current-cols new-cols db-path]]]})))

(reg-event-db
  ::store-cols
  (fn [db [_ cols db-path]]
    (assoc-in db [::current-cols db-path] cols)))

(reg-sub
  ::get-all-current-cols
  :-> ::current-cols)

(reg-sub
  ::get-all-default-cols
  :-> ::default-cols)

(reg-sub
  ::get-current-cols
  :<- [::get-all-current-cols]
  (fn [current-cols [_ db-path]]
    (get current-cols db-path)))

(reg-sub
  ::get-default-cols
  :<- [::get-all-default-cols]
  (fn [default-cols [_ db-path]]
    (get default-cols db-path)))

(reg-event-fx
  ::init-table-col-config
  [(inject-cofx :storage/get {:name local-storage-key})]
  (fn [{db          :db
        cols-config :storage/get} [_ cols db-path]]
    (let [defaults (distinct
                     (or
                       (some->>
                         cols
                         (map :field-key))
                       default-columns))
          cols     (or
                     (get (edn/read-string cols-config) db-path)
                     defaults)]
      {:db (assoc-in db [::default-cols db-path] defaults)
       :fx [[:dispatch [::store-cols cols db-path]]]})))

(defn- HeaderCellContent
  [{:keys [db-path sort-value-fn sort-key no-sort? header-content field-key fetch-event]}]
  (let [tr            @(subscribe [::i18n-subs/tr])
        sort-key      (or sort-key field-key)
        direction     (when db-path @(subscribe [::sort-direction db-path sort-key]))
        sort-enabled? (and
                        db-path
                        (or sort-key field-key)
                        (not no-sort?))
        sort-fn       #(dispatch [::sort {:field       sort-key
                                          :value-fn    sort-value-fn
                                          :direction   direction
                                          :db-path     db-path
                                          :fetch-event fetch-event}])]
    [:span {:on-click (when sort-enabled? sort-fn)
            :style    (when sort-enabled?
                        {:cursor :pointer})}
     (cond
       (fn? header-content)
       (header-content)

       header-content
       header-content

       :else
       (or (tr [field-key]) field-key))
     (when sort-enabled?
       [SortIcon direction])]))

(defn on-drag-start-fn
  [event]
  ;; this is needed to avoid showing a green plus sign on drag in Google Chrome. See https://github.com/react-dnd/react-dnd/issues/414
  (-> event .-dataTransfer .-effectAllowed (set! "move")))

(defn on-drag-fn [dnd-state key _event]
  (swap! dnd-state assoc :dragging key))

(defn on-drop-fn [dnd-state key move-fn _event]
  (let [dragging (:dragging @dnd-state)]
    (swap! dnd-state dissoc :dragging key)
    (when-not (= key dragging)
      (move-fn dragging key))))

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
              `:sort-key` or `:sort-value-fn` is provided or sort is disabled with `:no-sort?` for this column,
    - `:header-content` can be passed in for custom column header, else `:field-key` is used,
    - `:accessor` custom fn to accessing data for this column, else `:field-key` is used,
    - `:cell` custom render fn for row cell, specific row document and cell-data is passed, else raw data is shown,
    - `:sort-key` custom sort key for this column, else `:field-key` is used,
    - `:no-sort?` disables sort for this column,

    To enable sort, provide a `sort-config` with:
     - `:db-path` tells the table where the currently applied ordering is stored,
     - `:fetch-event` dispatch event after a new ordering is applied,
    Enabled sort for all columns, column wise disabling via `:no-sort?` key in column definition.

    To enable bulk selection, provide a `select-config` with:
     - `:select-db-path` usually [::spec/select],
     - `:total-count-sub-key` sub key to the total count of resources rendered in table,
     - `:resources-sub-key`, sub key to the currently visible resources,
     - `:bulk-actions`, a vector of at least one ::bulk-action. A ::bulk-action must be an
                        entire component to render or an event and name.
    Enabled sort for all columns, column wise disabling via `:no-sort?` key in column definition.

    Everything in the table can be pre-selected via `select=all` query parameter.
    Query param name can be overridden via `select-all-query-param` option in `select-config`.
"
  [_props]
  (let [dnd-state (r/atom
                    {:drag-hover nil
                     :dragging   nil})]
    (fn [{:keys [cell-props columns rows
                 row-click-handler row-props
                 sort-config select-config]
          :as   props}]
      (let [{:keys [bulk-actions select-db-path total-count-sub-key disabled-tooltip
                    resources-sub-key rights-needed select-label-accessor select-all-query-param]} select-config
            columns        (or columns (map (fn [[k _]] {:field-key k}) (first rows)))
            selectable?    (and
                             select-config
                             (s/valid? ::select-config select-config)
                             (or (not rights-needed)
                                 (some (partial general-utils/can-operation? rights-needed) rows)))
            selected-set   (subscribe [::selected-set-sub select-db-path])
            select-all?    (subscribe [::select-all?-sub select-db-path])
            page-selected? (subscribe [::is-all-page-selected? select-db-path resources-sub-key])
            get-row-props  (fn [row]
                             (update (merge row-props {:on-click #(when row-click-handler (row-click-handler row))})
                                     :style
                                     merge (:style (:table-row-prop row))))
            move-fn        (-> props :col-config :move-col-fn)]
        (dispatch [::init-pre-selection select-all-query-param select-db-path])
        [:div
         [BulkActionBar {:selectable?         selectable?
                         :selected-all-sub    select-all?
                         :selected-set-sub    selected-set
                         :page-selected?-sub  page-selected?
                         :total-count-sub-key total-count-sub-key
                         :rows                rows
                         :db-path             select-db-path
                         :bulk-actions        bulk-actions
                         :resources-sub-key   resources-sub-key
                         :disabled-tooltip    disabled-tooltip}]
         [:div
          [:div {:style {:overflow :auto
                         :padding  0
                         :position :relative}
                 :class :table-fixed-row-height}
           [ui/Table (merge {:stackable false} (:table-props props))
            [ui/TableHeader (:header-props props)
             [ui/TableRow
              (when selectable?
                [ui/TableHeaderCell
                 {:style {:width "30px"}}
                 [HeaderCellCeckbox {:db-path            select-db-path :resources-sub-key resources-sub-key
                                     :page-selected?-sub page-selected? :rights-needed rights-needed}]])
              (doall
                (for [col columns
                      :when col
                      :let [{:keys [field-key header-content header-cell-props]} col]]
                  ^{:key (or field-key (random-uuid))}
                  [ui/TableHeaderCell
                   (merge {:draggable     true
                           :class         (when (and (= field-key (:drag-hover @dnd-state))
                                                     (not= field-key (:dragging @dnd-state)))
                                            :drag-hover)
                           :on-drag-start on-drag-start-fn
                           :on-drag-end   #(swap! dnd-state assoc
                                                  :dragging nil
                                                  :drag-hover nil)
                           :on-drag       (partial on-drag-fn dnd-state field-key)
                           :on-drag-over  (fn []
                                            (js/event.preventDefault)
                                            (swap! dnd-state assoc :drag-hover field-key))
                           :on-drag-leave #(swap! dnd-state assoc :drag-hover nil)
                           :on-drop       (partial on-drop-fn dnd-state field-key move-fn)}
                          (:header cell-props)
                          header-cell-props)
                   [:div {:style {:display :flex}
                          :class :show-child-on-hover}
                    [:div {:style {:flex-grow 1}}
                     [HeaderCellContent
                      (merge sort-config
                             {:header-content header-content}
                             (select-keys col [:sort-key :sort-value-fn :field-key :no-sort?]))]
                     (when-let [remove-fn (-> props :col-config :remove-col-fn)]
                       (when (and (< 1 (count columns))
                                  (not (:no-remove-icon? col)))
                         [:span {:style {:margin-left "0.8rem"}}
                          [uix/LinkIcon {:color    "red"
                                         :disabled (< (count columns) 2)
                                         :name     "remove circle"
                                         :on-click #(remove-fn field-key)
                                         :class    :toggle-invisible-on-parent-hover}]]))]]]))
              (when-let [col-cong-button (-> props :col-config :col-config-modal)]
                [ui/TableHeaderCell {:style {:width "30px"}} [:div col-cong-button]])]]
            [ui/TableBody (:body-props props)
             (doall
               (for [[idx row] (map-indexed vector rows)
                     :let [id (or (:id row) (random-uuid))]]
                 ^{:key (:id row)}
                 [ui/TableRow (get-row-props row)
                  (when selectable?
                    [CellCheckbox {:id               id :selected-set-sub selected-set :db-path select-db-path
                                   :selected-all-sub select-all? :resources-sub-key resources-sub-key
                                   :rights-needed    rights-needed
                                   :edge-name        (or
                                                       (and select-label-accessor
                                                            (select-label-accessor row))
                                                       (:name row)
                                                       (:id row))
                                   :idx              idx}])
                  (for [[idx {:keys [field-key stop-event-propagation? accessor cell cell-props]}] (map-indexed vector columns)
                        :when (or field-key accessor)
                        :let [cell-data ((or accessor field-key) row)
                              last?     (= idx (dec (count columns)))]]
                    ^{:key (str id "-" field-key)}
                    [ui/TableCell
                     (cond->
                       cell-props

                       last?
                       (assoc :colSpan 2)

                       (and
                         stop-event-propagation?
                         (not (:on-click cell-props)))
                       (-> (assoc :on-click (fn [event] (.stopPropagation event)))
                           (update :style merge {:cursor :auto})))

                     (cond
                       cell (if (string? cell) [tt/with-overflow-tooltip
                                                [:div.vcenter [:div.ellipsing cell]] cell]
                                               [cell {:row-data  row
                                                      :cell-data cell-data
                                                      :field-key field-key}])
                       :else (let [s (str (if (or
                                                (not (coll? cell-data))
                                                (seq cell-data))
                                            cell-data
                                            ""))]
                               [tt/with-overflow-tooltip
                                [:div.vcenter [:div.ellipsing s]] s]))])]))]]]]]))))


(defn ConfigureVisibleColumns
  [db-path available-fields]
  (let [default-cols       (subscribe [::get-default-cols db-path])
        current-cols       (subscribe [::get-current-cols db-path])
        selected-cols      (r/atom (set @current-cols))
        show?              (r/atom false)
        available-col-keys (disj (set (keys available-fields)) :module)]
    (fn []
      [SelectFieldsView
       {:field->view         (into {}
                                   (map (fn [[k v]]
                                          [k (:header-content v)])
                                        available-fields))
        :title-tr-key        :columns
        :show?               show?
        :selections-atom     selected-cols
        :reset-to-default-fn #(reset! selected-cols (set @default-cols))
        :selected-fields-sub current-cols
        :available-fields    available-col-keys
        :update-fn           #(dispatch [::select-fields (set %) db-path])
        :trigger             [uix/Button
                              {:basic    true
                               :icon     :options
                               :class    :table-select-fields-button
                               :style    {:padding    0
                                          :box-shadow :none
                                          :position   :relative
                                          :z-index    1000}
                               :on-click (fn []
                                           (reset! selected-cols (set @current-cols))
                                           (reset! show? true))}]}])))

(defn- no-rmv-column? [cols-without-rmv-icon k]
  (boolean
    (get cols-without-rmv-icon k)))

(defn TableColsEditable
  [{:keys [columns default-columns cols-without-rmv-icon]} db-path]
  (dispatch [::init-table-col-config
             (if default-columns
               (filterv (comp default-columns :field-key) columns)
               columns)
             db-path])
  (let [db-path       (or db-path ::table-cols-config)
        current-cols  (subscribe [::get-current-cols db-path])
        default-cols  (subscribe [::get-default-cols db-path])
        remove-col-fn (fn [col-key]
                        (dispatch [::remove-col col-key db-path]))
        move-col-fn   (fn [col-key dest-col-key]
                        (dispatch [::move-col col-key dest-col-key db-path]))]
    (fn [{:keys [rows columns] :as props}]
      (let [available-cols (merge
                             (let [ks (set (mapcat keys rows))]
                               (zipmap
                                 ks
                                 (map (fn [k]
                                        {:field-key       k
                                         :no-remove-icon? (no-rmv-column? cols-without-rmv-icon k)})
                                      ks)))
                             (into {} (map (juxt :field-key
                                                 (fn [col]
                                                   (assoc col :no-remove-icon?
                                                              (no-rmv-column? cols-without-rmv-icon (:field-key col))))) columns)))]
        [:div
         [Table (assoc props
                  :col-config {:remove-col-fn    remove-col-fn
                               :move-col-fn      move-col-fn
                               :col-config-modal [ConfigureVisibleColumns db-path available-cols]}
                  :columns (->> (or @current-cols @default-cols)
                                (mapv (fn [k] (available-cols k)))
                                (remove nil?)
                                vec))]]))))


(s/fdef Table :args (s/cat :opts (s/keys
                                   :req-un [::rows]
                                   :opt-un [::columns
                                            ::table-props
                                            ::header-cell-props
                                            ::header-props
                                            ::body-props
                                            ::cell-props
                                            ::bulk-actions
                                            ::helpers/db-path
                                            ::sort-config
                                            ::select-config
                                            ::wide?])))
