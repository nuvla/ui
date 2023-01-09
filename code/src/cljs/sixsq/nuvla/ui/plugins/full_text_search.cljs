(ns sixsq.nuvla.ui.plugins.full-text-search
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :refer [dispatch reg-event-fx subscribe]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
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
    (let [change-event (get-in db (conj db-path ::change-event))]
      {:db (assoc-in db (conj db-path ::text) text)
       :fx [[:dispatch change-event]]})))

(defn FullTextSearch
  [{:keys [db-path change-event placeholder-suffix] :as opts}]
  (dispatch [::helpers/set db-path ::change-event change-event])
  (let [tr   @(subscribe [::i18n-subs/tr])
        text @(subscribe [::helpers/retrieve db-path ::text])]
    [ui/Input
     (-> opts
         (dissoc :db-path :change-event :placeholder-suffix)
         (assoc :placeholder (str (tr [:search]) placeholder-suffix "...")
                :icon "search"
                :default-value (or text "")
                :on-change (ui-callback/input-callback
                             #(dispatch [::search db-path %]))))]))

(s/fdef FullTextSearch
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::helpers/change-event])))
