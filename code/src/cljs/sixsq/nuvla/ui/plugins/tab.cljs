(ns sixsq.nuvla.ui.plugins.tab
  (:require
    [re-frame.core :refer [dispatch subscribe reg-event-fx]]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.plugins.helpers :as helpers]
    [taoensso.timbre :as log]
    [cljs.spec.alpha :as s]))

(s/def ::active-tab keyword?)
(s/def ::change-event (s/nilable coll?))

(defn build-spec
  [& {:keys [active-tab]}]
  {::active-tab active-tab})

(defn- key->index
  [panes k]
  (loop [i          0
         next-panes panes]
    (if (seq next-panes)
      (if (= ((comp :key :menuItem) (first next-panes)) k)
        i
        (recur (inc i) (next next-panes)))
      (do
        (log/warn "tab-key not found: " k panes)
        0))))

(defn- index->key
  [panes i]
  (if-let [k (some-> panes vec (get i) :menuItem :key)]
    k
    (log/warn "tab-index not found:" i panes)))

(reg-event-fx
  ::change-tab
  (fn [{db :db} [_ db-path tab-key]]
    (let [change-event (get-in db (conj db-path ::change-event))]
      {:db (assoc-in db (conj db-path ::active-tab) tab-key)
       :fx [(when change-event
              [:dispatch change-event])]})))

(defn- on-tab-change
  [db-path panes]
  (fn [_ data]
    (let [tab-key (index->key panes (.-activeIndex data))]
      (dispatch [::change-tab db-path tab-key]))))

(defn Tab
  [{:keys [db-path change-event] :as _opts}]
  (let [active-tab (subscribe [::helpers/retrieve db-path ::active-tab])]
    (dispatch [::helpers/set db-path ::change-event change-event])
    (fn [{:keys [db-path panes] :as opts}]
      (when (nil? @active-tab)
        (dispatch [::helpers/set db-path
                   ::active-tab (some-> (seq panes) first :menuItem :key)]))
      [ui/Tab
       (-> opts
           (dissoc :db-path :change-event :default-active-tab)
           (assoc :on-tab-change (on-tab-change db-path panes))
           (cond->
             @active-tab (assoc :active-index
                                (key->index panes @active-tab))))])))


(s/fdef Tab
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path]
                                   :opt-un [::helpers/change-event])))
