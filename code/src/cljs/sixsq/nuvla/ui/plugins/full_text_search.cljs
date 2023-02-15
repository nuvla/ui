(ns sixsq.nuvla.ui.plugins.full-text-search
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :refer [dispatch reg-event-fx subscribe]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.routing.events :as route-events]
            [sixsq.nuvla.ui.routing.utils :refer [db-path->query-param-key]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(s/def ::text string?)
(s/def ::change-event (s/nilable coll?))

(defn build-spec
  []
  {::text ""})

(defn filter-text
  [db db-path]
  (let [text (get-in db (conj db-path ::text))]
    (general-utils/fulltext-query-string text)))

(reg-event-fx
  ::search
  (fn [{db :db} [_ db-path text]]
    (let [change-event (get-in db (conj db-path ::change-event))
          query-key    (db-path->query-param-key db-path)]
      {:db (assoc-in db (conj db-path ::text) text)
       :fx [[:dispatch change-event]
            [:dispatch [::route-events/change-query-param {:partial-query-params {query-key text}}]]]})))

(reg-event-fx
  ::init-search
  (fn [{{:keys [current-route] :as db} :db} [_ db-path change-event]]
    (let [search (-> current-route :query-params (get (db-path->query-param-key db-path)))]
      (when (seq search)
        {:db (assoc-in db (conj db-path ::text) search)
         :fx [[:dispatch change-event]]}))))


(defn FullTextSearch
  [{:keys [db-path change-event _placeholder-suffix]}]
  (let [tr   (subscribe [::i18n-subs/tr])
        text (subscribe [::helpers/retrieve db-path ::text])]
    (dispatch [::helpers/set db-path ::change-event change-event])
    (dispatch [::init-search db-path change-event])
    (fn [{:keys [placeholder-suffix] :as opts}]
      [ui/Input
       (-> opts
           (dissoc :db-path :change-event :placeholder-suffix)
           (assoc :placeholder (str (@tr [:search]) placeholder-suffix "...")
                  :icon "search"
                  :default-value (or @text "")
                  :on-change (ui-callback/input-callback
                               #(dispatch [::search db-path %]))))])))

(s/fdef FullTextSearch
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::helpers/change-event])))
