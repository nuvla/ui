(ns sixsq.nuvla.ui.plugins.tab
  (:require
    [re-frame.core :refer [dispatch subscribe reg-sub reg-event-db]]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.plugins.helpers :as helpers]
    [taoensso.timbre :as log]
    [cljs.spec.alpha :as s]))

(s/def ::default-active-tab keyword?)
(s/def ::active-tab keyword?)

(defn build-spec
  [& {:keys [active-tab]}]
  {::active-tab active-tab})

(defn add-spec
  [db-location default-active-tab]
  {db-location {::active-tab default-active-tab}})

(defn- key->index
  [panes k]
  (loop [i          0
         next-panes panes]
    (if (seq next-panes)
      (if (= ((comp :key :menuItem) (first next-panes)) k)
        i
        (recur (inc i) (next next-panes)))
      (do
        (log/error "tab-key not found: " k panes)
        0))))

(defn- index->key
  [panes i]
  (if-let [k (some-> panes vec (get i) :menuItem :key)]
    k
    (log/error "tab-index not found:" i panes)))

(defn change-tab
  [db-path tab-key]
  (dispatch [::helpers/set db-path ::active-tab tab-key]))

(defn- on-tab-change
  [db-path panes on-change]
  (fn [_ data]
    (let [tab-key (index->key panes (.-activeIndex data))]
      (change-tab db-path tab-key)
      (when on-change
        (on-change tab-key)))))

(defn Tab
  [{:keys [db-path panes on-change] :as opts}]
  (let [active-tab (subscribe [::helpers/retrieve db-path ::active-tab])]
    [ui/Tab
     (-> opts
         (dissoc :db-path :on-change)
         (assoc :active-index (key->index panes @active-tab)
                :on-tab-change (on-tab-change db-path panes on-change)))]))
