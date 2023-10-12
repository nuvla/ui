(ns sixsq.nuvla.ui.plugins.table
  (:require [cljs.spec.alpha :as s]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch inject-cofx reg-event-db
                                   reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi.views :refer [SelectFieldsView]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

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

;;;; Additional Features ;;;;
;; sorting


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
     :fx [[:dispatch fetch-event]]}))

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

;; Bulk selection, table plugin args
(s/def ::name string?)
(s/def ::component fn?)
(s/def ::event (s/or :k (s/* keyword?) :fn fn?))
(s/def ::icon (s/nilable fn?))
(s/def ::total-count-sub-key (s/* keyword?))
(s/def ::resources-sub-key (s/* keyword?))

(s/def ::bulk-action (s/keys :req-un [::name ::event]
                       :opt-un [::component ::icon]))

(s/def ::bulk-actions (s/nilable (s/coll-of ::bulk-action :kind vector?)))
(s/def ::select-db-path (s/* keyword?))
(s/def ::rights-needed keyword?)
(s/def ::select-label-accessor (s/nilable fn?))
(s/def ::select-config (s/nilable (s/keys :req-un [::bulk-actions ::select-db-path
                                                   ::total-count-sub-key ::resources-sub-key]
                                    :opt-un [::rights-needed ::select-label-accessor])))
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
    (update-in db db-path merge {::select-all?           false
                                 ::selected-set          #{}})))

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
    (->> selected-set
      (map #(str "id='" % "'"))
      (apply general-utils/join-or))))

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
  ::select-id
  (fn [db [_ id db-path resources]]
    (let [selected-set (get-in-db db db-path ::selected-set #{})
          select-all?  (get-in-db db db-path ::select-all?)
          new-set      (cond select-all?
                         (disj (set resources) id)

                         (is-selected? selected-set id)
                         (disj selected-set id)

                         :else
                         (conj selected-set id))]
      (-> db
        (assoc-in (conj (or db-path []) ::selected-set) new-set)
        (assoc-in (conj (or db-path []) ::select-all?) false)))))

(reg-event-db
  ::select-all
  (fn [db [_ db-path status]]
    (-> db
      (assoc-in (conj (or db-path []) ::select-all?) (not= status :all))
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
                   :on-click (fn [event]
                               (select-fn)
                               (.stopPropagation event))}
     [ui/Checkbox {:id idx
                   :aria-label (str "select " edge-name)
                   :checked checked?}]]))


(defn HeaderCellCeckbox
  [{:keys [db-path resources-sub-key page-selected?-sub]}]
  (let [resources @(subscribe resources-sub-key)]
    [ui/Checkbox {:aria-label "select all on page"
                  :checked    @page-selected?-sub
                  :on-click   #(dispatch [::select-all-in-page {:resources resources :db-path db-path}])}]))

(defn BulkActionBar
  [{:keys [selected-set-sub total-count-sub-key selected-all-sub
           db-path bulk-actions resources-sub-key selectable? resource-type]}]
  (let [tr                          (subscribe [::i18n-subs/tr])
        rows                        @(subscribe resources-sub-key)
        total-count                 (subscribe total-count-sub-key)
        selection-status            (subscribe [::selection-status db-path resources-sub-key @total-count])
        on-page-selected            (count @(subscribe [::on-page-selected-set db-path resources-sub-key]))
        off-page-selected           (if @selected-all-sub
                                      (- @total-count (count rows))
                                      (count @(subscribe [::off-page-selected-set db-path resources-sub-key])))
        off-page-selection?         (pos? off-page-selected)
        off-page-selection-text     (when off-page-selection?
                                      (str/join " " [off-page-selected
                                                     (@tr [:on-other-pages])]))
        selected-all-text           (when (= :all @selection-status)
                                      (str/join " " [(str/capitalize (@tr [:all]))
                                                     @total-count
                                                     (@tr [(or resource-type :items)])
                                                     (@tr [:are-selected])
                                                     (when off-page-selection-text
                                                       (str "(" off-page-selection-text ")"))]))
        on-page-selection?          (pos? on-page-selected)
        manual-selection-text       (str/join " " [(when (#{:all :page :page-plus} @selection-status)
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
        button-text                 (if (= @selection-status :all)
                                      "Clear selection"
                                      (str "Select all " @total-count))
        payload                     {:select-all   @selected-all-sub
                                     :selected-set @selected-set-sub}
        nothing-selected?           (= :none @selection-status)
        bulk-edit-success-message   (subscribe [::bulk-edit-success-message-sub db-path])]
    [:div
     {:style  {:position :sticky :top "39px" :z-index 998}
      :class [(if selectable? :visible :invisible)]}
     [:div {:style {:display :flex
                    :border "1px solid rgb(230 230 230)"
                    :border-radius "0.28rem"
                    :background-color "rgb(249 250 251)"
                    :justify-content :space-between
                    :align-items :center
                    :height "40px"
                    :gap "1rem"}}
      [:div
       [ui/Popup {:trigger
                  (r/as-element
                    [:div
                     [ui/Menu {:style {:border          :none
                                       :box-shadow      :none
                                       :background-color :transparent}
                               :borderless (= 1 (count bulk-actions))
                               :stackable  true}
                      (for [[idx action] (map-indexed vector bulk-actions)
                            :let [{:keys [name event icon]} action]]
                        [ui/MenuItem
                         {:disabled nothing-selected?
                          :class :bulk-action-bar-item
                          :on-click (fn []
                                      (if (fn? event) (event payload)
                                        (dispatch event)))
                          :key idx}
                         (when icon [icon])
                         name])]])
                  :basic    true
                  :disabled (not= :none @selection-status)
                  :content  (@tr [:select-at-least-one-item])}]]
      [:div {:style {:padding-right "1rem"}}
       @bulk-edit-success-message]]
     [:div
      {:style {:heigh "2rem"
               :padding-left "1rem"
               :border "1px solid rgb(230 230 230)"
               :border-radius "0.28rem"
               :background-color "#f0eeef"}}
      [:div {:style {:display         :flex
                     :justify-content :center
                     :align-items     :center}}
       [:span (or selected-all-text manual-selection-text)]
       [:button {:style {:width "120px" :text-align :center :border :none}
                 :on-click (fn [] (dispatch [::select-all db-path @selection-status]))
                 :class [:select-all]}
        button-text]]]]))

(defn- ColumnsDropDown
  [{:keys [current-cols available-cols default-cols add-col-fn]} position]
  (let [tr               (subscribe [::i18n-subs/tr])
        cols-not-visible (->> (keys available-cols)
                           (remove (set current-cols)))
        sorted-cols     (sort
                          (fn [k1 k2]
                            (let [pos-fn (fn [k]
                                           (let [po (.indexOf (or default-cols []) k)]
                                             (if (neg? po) 100 po)))]
                              (- (pos-fn k1) (pos-fn k2))))
                          cols-not-visible)]
    [ui/Dropdown
     {:options
      (mapv (fn [col-key]
              {:key   col-key
               :value col-key
               :text  (or (@tr [col-key]) col-key)})
        sorted-cols)
      :trigger (@tr [:add-column-to-right])
      :on-change (ui-callback/value
                   (fn [key-string]
                     (add-col-fn (keyword key-string) position)))}]))

;;;;;; CONFIGURE TABLE COLUMNS ;;;;;;;;;;;;;
;; default-cols
;; -> vector of field-keys
;;    order important
;; available-cols
;; -> map of field-key->col-config
;; current-cols
;; -> vector of field-keys
;;    order important
;; only current-cols persisted in local-storage

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
  (fn [{storage :storage/get
        {:keys [::default-cols]} :db} [_ cols db-path]]
    (let [defaults (get default-cols db-path)
          new-cols (if
                     (seq? cols)
                     cols
                     (sort
                       (fn [k1 k2]
                         (let [pos-fn (fn [k]
                                        (let [po (.indexOf (or defaults []) k)]
                                          (if (neg? po) 100 po)))]
                           (- (pos-fn k1) (pos-fn k2))))
                       (vec cols)))]
      ()
      {:fx [[:dispatch [::store-cols new-cols db-path]]]
       :storage/set {:session? false
                     :name     local-storage-key
                     :value    (merge (edn/read-string storage) {db-path cols})}})))

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
  (fn [{{:keys [::current-cols] :as db} :db
        cols-config                     :storage/get} [_ cols db-path]]
    (let [defaults (or
                     (some->>
                       cols
                       (map :field-key))
                     default-columns)
          cols     (or
                     (get (edn/read-string cols-config) db-path)
                     (get current-cols db-path)
                     defaults)]
      {:db (assoc-in db [::default-cols db-path] defaults)
       :fx [[:dispatch [::store-cols cols db-path]]]})))

(defn ConfigureVisibleColumns
  [db-path available-fields]
  (let [default-cols  (subscribe [::get-default-cols db-path])
        current-cols  (subscribe [::get-current-cols db-path])
        selected-cols (r/atom (set @current-cols))
        show?         (r/atom false)
        available-col-keys (set (keys available-fields))]
    (fn []
      [SelectFieldsView
       {:field->view (into {}
                       (map (fn [[k v]]
                              [k (:header-content v)])
                         available-fields))
        :title-tr-key :columns
        :show? show?
        :selections-atom selected-cols
        :reset-to-default-fn #(reset! selected-cols (set @default-cols))
        :selected-fields-sub current-cols
        :available-fields available-col-keys
        :update-fn     #(dispatch [::set-current-cols % db-path])
        :trigger       [uix/Button
                        {:basic true
                         :icon :options
                         :style {:padding 0
                                 :box-shadow :none
                                 :position :relative
                                 :z-index 1000}
                         :on-click (fn []
                                     (reset! selected-cols (set @current-cols))
                                     (reset! show? true))}]}])))


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
"
  [{:keys [cell-props columns rows
           row-click-handler row-props
           sort-config select-config]
    :as   props}]
  (let [{:keys [bulk-actions select-db-path total-count-sub-key
                resources-sub-key rights-needed select-label-accessor]} select-config
        tr             @(subscribe [::i18n-subs/tr])
        columns        (or columns (map (fn [[k _]] {:field-key k}) (first rows)))
        selectable?    (and
                         select-config
                         (s/valid? ::select-config select-config)
                         (seq (:bulk-actions select-config))
                         (or (not rights-needed)
                           (some (partial general-utils/can-operation? rights-needed) rows)))
        selected-set   (when selectable? (subscribe [::selected-set-sub select-db-path]))
        select-all?    (when selectable? (subscribe [::select-all?-sub select-db-path]))
        page-selected? (when selectable? (subscribe [::is-all-page-selected? select-db-path resources-sub-key rights-needed]))
        get-row-props  (fn [row]
                         (merge row-props {:on-click #(when row-click-handler (row-click-handler row))} (:table-row-prop row)))]
    [:div
     (when selectable?
       [BulkActionBar {:selectable?         selectable?
                       :selected-all-sub    select-all?
                       :selected-set-sub    selected-set
                       :page-selected?-sub  page-selected?
                       :total-count-sub-key total-count-sub-key
                       :rows                rows
                       :db-path             select-db-path
                       :bulk-actions        bulk-actions
                       :resources-sub-key   resources-sub-key}])
     [:div
      [:div {:style {:overflow :auto
                     :padding 0
                     :position :relative}}
       [ui/Table (merge {:stackable false} (:table-props props))
        [ui/TableHeader (:header-props props)
         [ui/TableRow
          (when selectable?
            [ui/TableHeaderCell
             {:style {:width "30px"}}
             [HeaderCellCeckbox {:db-path select-db-path :resources-sub-key resources-sub-key
                                 :page-selected?-sub page-selected? :rights-needed rights-needed}]])
          (for [[idx col] (map-indexed vector columns)
                :when col
                :let [{:keys [field-key header-content header-cell-props no-sort?]} col]]
            ^{:key (or field-key (random-uuid))}
            [ui/TableHeaderCell
             (merge (:header cell-props) header-cell-props)
             [:div {:style {:display :flex}
                    :class :show-child-on-hover}
              [ui/Popup
               {:trigger
                (r/as-element
                  [:div {:style {:flex-grow 1}}
                   (when (and
                           sort-config
                           (not no-sort?))
                     [Sort (merge
                             sort-config
                             (select-keys col [:field-key :disable-sort :sort-key]))])
                   (cond
                     (fn? header-content)
                     (header-content)

                     header-content
                     header-content

                     :else
                     (or (tr [field-key]) field-key))
                   [:span {:style {:margin-left "0.2rem"}} (when-let [remove-fn (-> props :col-config :remove-col-fn)]
                                                             (when (< 1 (count columns))
                                                               [uix/LinkIcon {:color "red"
                                                                              :disabled (< (count columns) 2)
                                                                              :name "remove circle"
                                                                              :on-click #(remove-fn field-key)
                                                                              :class :toggle-invisible-on-parent-hover}]))]])
                :position "top right"
                :disabled (not (-> props :col-config :remove-col-fn))
                :hoverable true
                :basic   true
                :content
                (r/as-element
                  [:div {:style {:display :flex}}
                   [:a {:on-click (fn [] (let [reset-fn (-> props :col-config :reset-cols-fn)]
                                           (reset-fn)))
                        :href ""}
                    "RESET default columns"]
                   [ColumnsDropDown (:col-config props) (inc idx)]])}]]])
          (when-let [col-cong-button (-> props :col-config :col-config-modal)]
            [ui/TableHeaderCell [:div col-cong-button]])]]
        [ui/TableBody (:body-props props)
         (doall
           (for [[idx row] (map-indexed vector rows)
                 :let [id (or (:id row) (random-uuid))]]
             ^{:key (:id row)}
             [ui/TableRow (get-row-props row)
              (when selectable?
                [CellCheckbox {:id id :selected-set-sub selected-set :db-path select-db-path
                               :selected-all-sub select-all? :resources-sub-key resources-sub-key
                               :rights-needed rights-needed
                               :edge-name (or
                                            (and select-label-accessor
                                              (select-label-accessor row))
                                            (:name row)
                                            (:id row))
                               :idx idx}])
              (for [{:keys [field-key accessor cell cell-props]} columns
                    :let [cell-data ((or accessor field-key) row)]]
                ^{:key (str id "-" field-key)}
                [ui/TableCell
                 cell-props
                 (cond
                   cell (if (string? cell) cell
                          [cell {:row-data  row
                                 :cell-data cell-data
                                 :field-key field-key}])
                   :else (str cell-data))])
              (when (:col-config props) [ui/TableCell])]))]]]]]))

(defn TableColsEditable
  [{:keys [columns rows default-columns]} db-path]
  (dispatch [::init-table-col-config
             (if default-columns
               (filterv (comp default-columns :field-key) columns)
               columns)
             db-path])
  (let [db-path (or db-path ::table-cols-config)
        available-cols (merge
                         (let [ks (mapcat keys rows)]
                           (zipmap
                             ks
                             (map (fn [k] {:field-key k}) ks)))
                         (into {} (map (juxt :field-key identity) columns)))
        current-cols   (subscribe [::get-current-cols db-path])
        default-cols   (subscribe [::get-default-cols db-path])
        add-col-fn     (fn [col-key position]
                         (dispatch [::add-col {:col-key col-key
                                               :position position
                                               :db-path db-path}]))
        remove-col-fn (fn [col-key]
                        (dispatch [::remove-col col-key db-path]))
        reset-cols-fn #(dispatch [::reset-current-cols db-path])]
    (fn [props]
      [:div
       [Table (assoc props
                :col-config {:default-cols   @default-cols
                             :available-cols available-cols
                             :current-cols   @current-cols
                             :remove-col-fn  remove-col-fn
                             :add-col-fn     add-col-fn
                             :reset-cols-fn  reset-cols-fn
                             :col-config-modal [ConfigureVisibleColumns db-path available-cols]}
                :columns (->> (or @current-cols @default-cols)
                              (mapv (fn [k] (available-cols k)))
                              (remove nil?)
                              vec))]])))


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
