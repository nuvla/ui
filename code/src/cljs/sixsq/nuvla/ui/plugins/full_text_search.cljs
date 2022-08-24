(ns sixsq.nuvla.ui.plugins.full-text-search
  (:require
    [re-frame.core :refer [dispatch subscribe reg-event-fx]]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.plugins.helpers :as helpers]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [cljs.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.general :as general-utils]))

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
    (let [change-event (get-in db (conj db-path ::change-event))]
      {:db (assoc-in db (conj db-path ::text) text)
       :fx [[:dispatch change-event]]})))

(defn FullTextSearch
  [{:keys [db-path change-event] :as opts}]
  (dispatch [::helpers/set db-path ::change-event change-event])
  (let [tr   @(subscribe [::i18n-subs/tr])
        text @(subscribe [::helpers/retrieve db-path ::text])]
    [ui/Input
     (-> opts
         (dissoc :db-path :change-event)
         (assoc :placeholder (tr [:search])
                :icon "search"
                :value (or text "")
                :on-change (ui-callback/input-callback
                             #(dispatch [::search db-path %]))))]))

(s/fdef FullTextSearch
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::helpers/change-event])))
