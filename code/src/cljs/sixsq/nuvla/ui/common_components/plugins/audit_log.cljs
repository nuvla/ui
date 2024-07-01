(ns sixsq.nuvla.ui.common-components.plugins.audit-log
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.common-components.plugins.table :refer [Table]]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as u]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.values :as values]))

(s/def ::events (s/nilable coll?))
(s/def ::loading? (s/nilable boolean?))

(defn build-spec
  [& {:keys [default-items-per-page]
      :or   {default-items-per-page 10}}]
  {::loading?         true
   ::pagination       (pagination-plugin/build-spec
                        :default-items-per-page default-items-per-page)
   ::show-all-events? false})

(defn build-filter
  [{:keys [active-claim] :as _session} {:keys [href event-name all-events?]}]
  (str/join " and "
            (remove
              nil?
              [(when-not all-events?
                 (str "authn-info/active-claim='" active-claim "'"))
               (when href
                 (if (coll? href)
                   (str "content/resource/href=" href)
                   (str "content/resource/href='" href "'")))
               (when event-name
                 (if (coll? event-name)
                   (str "name=" event-name)
                   (str "name='" event-name "'")))])))

(reg-event-fx
  ::load-events
  (fn [{{:keys [::session-spec/session] :as db} :db} [_ db-path filters loading?]]
    (let [params (->>
                   {:filter  (build-filter session filters)
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

(defn EventsTable
  [{:keys [db-path filters] :as _opts}]
  (let [tr        @(subscribe [::i18n-subs/tr])
        events    @(subscribe [::helpers/retrieve db-path ::events])
        resources (:resources events)]
    [:<>
     [Table {:columns
             [{:field-key :event
               :cell      (fn [{{event-id :id event-name :name} :row-data}]
                            [values/AsLink event-id :label event-name])}
              {:field-key :user
               :cell (fn [{{{:keys [active-claim]} :authn-info} :row-data}]
                       [EventUser active-claim])}
              {:field-key :resource
               :cell      (fn [{{{{:keys [href]} :resource} :content} :row-data}]
                            (let [resource-name (u/id->resource-name href)]
                              [values/AsPageLink href :label resource-name]))}
              {:field-key :description}
              {:field-key      :timestamp
               :header-content (constantly (str/lower-case (tr [:time])))
               :cell           (fn [{timestamp :cell-data}]
                                 [uix/TimeAgo timestamp])}
              {:field-key  :state
               :accessor   #(get-in % [:content :state])
               :cell-props {:style {:white-space "pre"}}}
              {:field-key  :details
               :accessor   #(get-in % [:content :state])
               :cell       (fn [{{{:keys [linked-identifiers]} :content} :row-data}]
                             [ui/Popup {:position  "top center"
                                        :trigger   (r/as-element
                                                     [ui/Icon {:class icons/i-info
                                                               :link  true}])
                                        :hoverable true}
                              [LinkedIdentifiers linked-identifiers]])
               :cell-props {:style {:white-space "pre"}}}]
             :rows resources}]
     [pagination-plugin/Pagination
      {:db-path      (conj db-path ::pagination)
       :total-items  (:count events)
       :change-event [::load-events db-path filters true]}]]))

(s/def ::href string?)

(s/def ::filters (s/nilable map?))

#_(s/fdef EventsTable
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::href]
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
  [{:keys [db-path] :as opts}]
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
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::href]
                                   :opt-un [::filters])))
