(ns sixsq.nuvla.ui.common-components.plugins.audit-log
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table-refactor :refer [TableController]]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as u]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.tooltip :as tt]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.values :as values]))

(s/def ::events (s/nilable coll?))
(s/def ::loading? (s/nilable boolean?))

(defn build-spec
  [& {:keys [default-items-per-page default-show-all-events?]
      :or   {default-items-per-page   10
             default-show-all-events? false}}]
  {::loading?         true
   ::pagination       (pagination-plugin/build-spec
                        :default-items-per-page default-items-per-page)
   ::show-all-events? default-show-all-events?})

(defn build-filter
  [{:keys [active-claim user] :as _session} {:keys [href event-name all-events?]}]
  (let [current-role (or active-claim user)]
    (str/join " and "
              (remove
                nil?
                [(when-not all-events?
                   ;; only show events initiated or somehow related to the current role
                   (str "(authn-info/active-claim='" current-role "' or"
                        "content/linked-identifiers='" current-role "')"))
                 (when href
                   (if (coll? href)
                     (str "content/resource/href=" href)
                     (str "content/resource/href='" href "'")))
                 (when event-name
                   (if (coll? event-name)
                     (str "name=" event-name)
                     (str "name='" event-name "'")))]))))

(reg-event-fx
  ::load-events
  (fn [{{:keys [::session-spec/session] :as db} :db} [_ db-path filters loading?]]
    (let [all-events? (get-in db (conj db-path ::show-all-events?))
          params      (->>
                        {:filter  (build-filter session (merge filters {:all-events? all-events?}))
                         :orderby "created:desc"
                         :select  "id, name, description, content, severity, timestamp, category, authn-info"}
                        (pagination-plugin/first-last-params
                          db (conj db-path ::pagination)))]
      {:db                  (cond-> db
                                    loading? (assoc-in
                                               (conj db-path ::loading?) true))
       ::cimi-api-fx/search [:event params
                             #(dispatch [::helpers/set db-path
                                         ::events %
                                         ::loading? false])]})))


(reg-event-fx
  ::show-all-events
  (fn [{db :db} [_ {:keys [db-path filters] :as _opts}]]
    {:db (assoc-in db (conj db-path ::show-all-events?) true)
     :fx [[:dispatch [::load-events db-path (assoc filters :all-events? true) true]]]}))


(reg-event-fx
  ::show-current-user-events
  (fn [{db :db} [_ {:keys [db-path filters] :as _opts}]]
    {:db (assoc-in db (conj db-path ::show-all-events?) false)
     :fx [[:dispatch [::load-events db-path (assoc filters :all-events? false) true]]]}))


(reg-sub
  ::show-all-events?
  (fn [db [_ db-path]]
    (get-in db (conj db-path ::show-all-events?))))


(defn LinkedIdentifiers
  [linked-identifiers]
  [:<>
   (for [linked-identifier linked-identifiers]
     ^{:key linked-identifier}
     [:div [values/AsPageLink linked-identifier
            :label (u/id->resource-name linked-identifier)]])])

(defn EventUser
  [user-id]
  (let [principal-name @(subscribe [::session-subs/resolve-principal user-id])]
    [:span principal-name]))

(defn event-name->operation
  [event-name]
  (some->> event-name (re-matches #".*\.(.*)") second))

(defn ResourceLink
  [_cell-data {{{:keys [href]} :resource} :content :as _row} _column]
  (let [resource-name (u/id->resource-name href)]
    [values/AsPageLink href :label resource-name]))

(defn EventLink
  [_cell-data {event-id :id event-name :name :as _row} _column]
  [values/AsLink event-id :label (if (= "legacy" event-name)
                                   "event"
                                   (event-name->operation event-name))])

(defn UserLink
  [_cell-data {{:keys [active-claim]} :authn-info :as _row} _column]
  [EventUser active-claim])

(defn EventTime
  [timestamp _row _column]
  [uix/TimeAgo timestamp])

(defn EventDescription
  [_cell-data {:keys [description] {:keys [state]} :content :as _row} _column]
  (let [desc (or description state)]
    [tt/WithOverflowTooltip {:as      :div.max-width-50ch.ellipsing
                             :content desc :tooltip desc}]))

(defn EventDetails
  [_cell-data {{:keys [linked-identifiers]} :content :as _row} _column]
  (when (seq linked-identifiers)
    [ui/Popup {:position  "top center"
               :trigger   (r/as-element
                            [ui/Icon {:class icons/i-info
                                      :link  true}])
               :hoverable true}
     [LinkedIdentifiers linked-identifiers]]))

(def events-table-col-configs-local-storage-key "nuvla.ui.table.events.column-configs")

(reg-sub
  ::events-table-current-cols
  :<- [::main-subs/current-cols events-table-col-configs-local-storage-key ::events-columns-ordering]
  identity)

(main-events/reg-set-current-cols-event-fx
  ::set-events-table-current-cols-main events-table-col-configs-local-storage-key)

(reg-event-fx
  ::set-events-table-current-cols
  (fn [_ [_ columns]]
    {:fx [[:dispatch [::set-events-table-current-cols-main ::events-columns-ordering columns]]]}))

(reg-sub
  ::event-resources
  (fn [[_ db-path]]
    [(subscribe [::helpers/retrieve db-path ::events])])
  (fn [[events]]
    (:resources events)))

(reg-sub
  ::event-count
  (fn [[_ db-path]]
    [(subscribe [::helpers/retrieve db-path ::events])])
  (fn [[events]]
    (:count events)))

(defn EventsTable
  [{:keys [db-path filters max-height] :as _opts}]
  (let [!events      (subscribe [::event-resources db-path])
        !event-count (subscribe [::event-count db-path])]
    [:<>
     [TableController {:!columns               (r/atom [{::table-refactor/field-key      :resource
                                                         ::table-refactor/header-content "Resource"
                                                         ::table-refactor/field-cell     ResourceLink}
                                                        {::table-refactor/field-key      :event
                                                         ::table-refactor/header-content "Event"
                                                         ::table-refactor/field-cell     EventLink}
                                                        {::table-refactor/field-key      :user
                                                         ::table-refactor/header-content "User"
                                                         ::table-refactor/field-cell     UserLink}
                                                        {::table-refactor/field-key      :timestamp
                                                         ::table-refactor/header-content "Time"
                                                         ::table-refactor/field-cell     EventTime}
                                                        {::table-refactor/field-key      :description
                                                         ::table-refactor/header-content "Description"
                                                         ::table-refactor/field-cell     EventDescription}
                                                        {::table-refactor/field-key      :details
                                                         ::table-refactor/header-content "Details"
                                                         :accessor                       #(get-in % [:content :state])
                                                         ::table-refactor/field-cell     EventDetails
                                                         :cell-props                     {:style {:white-space "pre"}}}])
                       :!default-columns       (r/atom [:resource :event :user :timestamp :description :details])
                       :!current-columns       (subscribe [::events-table-current-cols])
                       :set-current-columns-fn #(dispatch [::set-events-table-current-cols %])
                       :!data                  !events
                       :!enable-global-filter? (r/atom false)
                       :!enable-sorting?       (r/atom false)
                       :!sticky-headers?       (r/atom (some? max-height))
                       :!max-height            (r/atom max-height)}]
     [pagination-plugin/Pagination
      {:db-path      (conj db-path ::pagination)
       :total-items  @!event-count
       :change-event [::load-events db-path filters true]}]]))

(s/def ::filters (s/nilable map?))

(s/fdef EventsTable
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path]
                                   :opt-un [::filters])))

(defn EventsFilters
  [{:keys [db-path] :as opts}]
  (let [tr               @(subscribe [::i18n-subs/tr])
        show-all-events? @(subscribe [::show-all-events? db-path])]
    [ui/Checkbox {:name      "show-all-events"
                  :label     (tr [:show-all-events])
                  :checked   show-all-events?
                  :on-change (ui-callback/checked
                               (fn [checked]
                                 (if checked
                                   (dispatch [::show-all-events opts])
                                   (dispatch [::show-current-user-events opts]))))
                  :align     :middle
                  :style     {:padding 5}}]))

(defn EventsTableWithFilters
  [{:keys [db-path max-height] :as opts}]
  (let [show-all-events? @(subscribe [::show-all-events? db-path])]
    [ui/Grid
     [ui/GridColumn
      [EventsFilters opts]
      [EventsTable (assoc-in opts [:filters :all-events?] show-all-events?)]]]))

(defn- EventsTabPane
  [{:keys [db-path] :as _opts}]
  (let [loading? (subscribe [::helpers/retrieve db-path ::loading?])]
    (fn [opts]
      [ui/TabPane {:loading @loading?}
       [EventsTableWithFilters opts]])))

(defn events-section
  [{:keys [db-path] :as opts}]
  (let [tr           @(subscribe [::i18n-subs/tr])
        events-count @(subscribe [::helpers/retrieve
                                  (conj db-path ::events) :count])]
    {:menuItem {:content (r/as-element
                           [:span (str/capitalize (tr [:events]))
                            (when (pos? events-count)
                              [ui/Label {:circular true
                                         :size     "mini"
                                         :attached "top right"}
                               events-count])])
                :key     :events
                :icon    icons/i-bolt}
     :render   #(r/as-element [EventsTabPane opts])}))

(s/fdef events-section
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path]
                                   :opt-un [::filters])))
