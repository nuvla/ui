(ns sixsq.nuvla.ui.plugins.events-table
  (:require [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.values :as values]
            [re-frame.core :refer [subscribe dispatch reg-event-fx reg-event-db]]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.plugins.pagination :as pagination]
            [cljs.spec.alpha :as s]))

(s/def ::events (s/nilable coll?))
(s/def ::loading? (s/nilable boolean?))

(defn build-spec
  [& {:keys [default-items-per-page]
      :or   {default-items-per-page 10}}]
  {::loading?   true
   ::pagination (pagination/build-spec
                  :default-items-per-page default-items-per-page)})

(reg-event-fx
  ::load-events
  (fn [{db :db} [_ db-path href loading?]]
    (let [params (->> {:filter  (str "content/resource/href='" href "'")
                       :orderby "created:desc"
                       :select  "id, content, severity, timestamp, category"}
                      (pagination/first-last-params
                        db (conj db-path ::pagination)))]
      {:db                  (cond-> db
                                    loading? (assoc-in
                                               (conj db-path ::loading?) true))
       ::cimi-api-fx/search [:event params
                             #(dispatch [::helpers/set db-path
                                         ::events %
                                         ::loading? false])]})))


(defn Events
  [{:keys [db-path href] :as _opts}]
  (dispatch [::load-events db-path href true])
  (fn [{:keys [db-path] :as _opts}]
    (let [tr       @(subscribe [::i18n-subs/tr])
          events   @(subscribe [::helpers/retrieve db-path ::events])
          loading? @(subscribe [::helpers/retrieve db-path ::loading?])]
      [ui/Segment {:basic   true
                   :loading loading?
                   :style   {:padding 0}}
       [ui/Table {:basic :very}
        [ui/TableHeader
         [ui/TableRow
          [ui/TableHeaderCell [:span (tr [:event])]]
          [ui/TableHeaderCell [:span (tr [:timestamp])]]
          [ui/TableHeaderCell [:span (tr [:category])]]
          [ui/TableHeaderCell [:span (tr [:state])]]]]
        [ui/TableBody
         (for [{:keys [id content timestamp category]} (:resources events)]
           ^{:key id}
           [ui/TableRow
            [ui/TableCell [values/as-link id
                           :label (general-utils/id->short-uuid id)]]
            [ui/TableCell timestamp]
            [ui/TableCell category]
            [ui/TableCell (:state content)]])]]
       [pagination/Pagination
        {:db-path      (conj db-path ::pagination)
         :total-items  (get events :count 0)
         :change-event [::load-events db-path href true]}]])))

(s/def ::href string?)

(s/fdef Events
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::href])))
