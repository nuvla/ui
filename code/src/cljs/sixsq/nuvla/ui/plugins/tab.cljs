(ns sixsq.nuvla.ui.plugins.tab
  (:require
    [re-frame.core :refer [dispatch subscribe reg-sub reg-event-db]]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.plugins.helpers :as helpers]
    [taoensso.timbre :as log]))

(defn add-spec
  [location-kw default-active-tab]
  {location-kw {::active-tab default-active-tab}})

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
  [db-location tab-key]
  (dispatch [::helpers/set db-location ::active-tab tab-key]))

(defn- on-tab-change
  [db-location panes on-change]
  (fn [_ data]
    (let [tab-key (index->key panes (.-activeIndex data))]
      (change-tab db-location tab-key)
      (when on-change
        (on-change tab-key)))))

(defn Tab
  [{:keys [db-location panes on-change] :as opts}]
  (let [active-tab (subscribe [::helpers/retrieve db-location ::active-tab])]
    [ui/Tab
     (-> opts
         (dissoc :db-location :on-change)
         (assoc :activeIndex (key->index panes @active-tab)
                :onTabChange (on-tab-change db-location panes on-change)))]))
