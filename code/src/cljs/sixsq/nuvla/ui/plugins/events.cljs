(ns sixsq.nuvla.ui.plugins.events
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-fx subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :refer [Table]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.values :as values]))

(s/def ::events (s/nilable coll?))
(s/def ::loading? (s/nilable boolean?))

(defn build-spec
  [& {:keys [default-items-per-page]
      :or   {default-items-per-page 10}}]
  {::loading?   true
   ::pagination (pagination-plugin/build-spec
                  :default-items-per-page default-items-per-page)})

(reg-event-fx
  ::load-events
  (fn [{db :db} [_ db-path href loading?]]
    (let [params (->>
                   {:filter  (str "content/resource/href='" href "'")
                    :orderby "created:desc"
                    :select  "id, content, severity, timestamp, category"}
                   (pagination-plugin/first-last-params
                     db (conj db-path ::pagination)))]
      {:db                  (cond-> db
                                    loading? (assoc-in
                                               (conj db-path ::loading?) true))
       ::cimi-api-fx/search [:event params
                             #(dispatch [::helpers/set db-path
                                         ::events %
                                         ::loading? false])]})))


(defn EventsTable
  [{:keys [db-path href] :as _opts}]
  (let [events    @(subscribe [::helpers/retrieve db-path ::events])
        resources (:resources events)
        start     (-> resources last :timestamp)]
    [:<>
     [Table {:columns
             [{:field-key :events
               :accessor  :id
               :cell      (fn [{id :cell-data}]
                            [values/AsLink id
                             :label (general-utils/id->short-uuid id)])}
              {:field-key :timestamp}
              {:field-key :delta-min
               :accessor  #(-> start
                               (time/delta-minutes (:timestamp %))
                               general-utils/round-up)}
              {:field-key :category}
              {:field-key  :state
               :accessor   #(get-in % [:content :state])
               :cell-props {:style {:white-space "pre"}}}]
             :rows resources}]
     [pagination-plugin/Pagination
      {:db-path      (conj db-path ::pagination)
       :total-items  (:count events)
       :change-event [::load-events db-path href true]}]]))

(s/def ::href string?)

(s/fdef EventsTable
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::href])))

(defn- EventsTabPane
  [{:keys [db-path] :as _opts}]
  (let [loading? @(subscribe [::helpers/retrieve db-path ::loading?])]
    (fn [opts]
      [ui/TabPane {:loading loading?}
       [EventsTable opts]])))

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
                :icon    "bolt"}
     :render   #(r/as-element [EventsTabPane opts])}))

(s/fdef events-section
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::href])))
