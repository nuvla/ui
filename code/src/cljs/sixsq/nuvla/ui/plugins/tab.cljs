(ns sixsq.nuvla.ui.plugins.tab
  (:require
    [cljs.spec.alpha :as s]
    [re-frame.core :refer [dispatch reg-event-fx reg-sub subscribe]]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.plugins.helpers :as helpers]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [taoensso.timbre :as log]))

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
  (fn [{{:keys [::main-spec/changes-protection?] :as db} :db} [_ db-path tab-key]]
    (let [change-event    (get-in db (conj db-path ::change-event))
          normal-behavior {:db (assoc-in db (conj db-path ::active-tab) tab-key)
                           :fx [(when change-event
                                  [:dispatch change-event])
                                [:dispatch [::main-events/changes-protection? false]]]}]
      (if changes-protection?
        {:db (assoc db ::main-spec/ignore-changes-modal normal-behavior)}
        normal-behavior))))

(defn active-tab
  [db db-path]
  (get-in db (conj db-path ::active-tab)))

(reg-sub
  ::active-tab
  (fn [db [_ db-path]]
    (active-tab db db-path)))


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
