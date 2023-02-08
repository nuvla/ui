(ns sixsq.nuvla.ui.plugins.nav-tab
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :refer [dispatch reg-event-fx reg-sub subscribe]]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.routing.events :as route-events]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.routing.utils :refer [db-path->query-param-key
                                                  gen-href]]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(s/def ::default-tab keyword?)
(s/def ::change-event (s/nilable coll?))

(defn build-spec
  [& {:keys [active-tab]}]
  {::default-tab active-tab})


(defn get-active-tab
  [db db-path]
  (get-in (:current-route db)
          [:query-params (db-path->query-param-key db-path)]
          (get-in db (conj db-path ::default-tab))))

(reg-sub
  ::default-tab
  (fn [db [_ db-path]]
    (get-in db (conj db-path ::default-tab))))

(reg-sub
  ::active-tab
  (fn [[_ db-path]]
    [(subscribe [::route-subs/query-param (db-path->query-param-key db-path)])
     (subscribe [::default-tab db-path])])
  (fn [[query-param default-tab]]
    (keyword (or query-param default-tab))))


(reg-event-fx
  ::change-tab
  (fn [{db :db} [_ db-path tab-key]]
    (let [change-event (get-in db (conj db-path ::change-event))]
      {:fx [[:dispatch [::route-events/navigate-partial
                        {:change-event         change-event
                         :partial-query-params {(db-path->query-param-key db-path) tab-key}}]]]})))

(defn Tab
  [{:keys [db-path panes change-event] :as _opts}]
  (dispatch [::helpers/set db-path ::change-event change-event])
  (let [active-tab      (subscribe [::helpers/retrieve db-path ::default-tab])
        route           (subscribe [::route-subs/current-route])
        panes           (remove nil? panes)
        key->index      (zipmap (map (comp :key :menuItem) panes)
                                (range (count panes)))
        query-param-key (db-path->query-param-key db-path)
        cur-view        (subscribe [::route-subs/query-param query-param-key])
        add-hrefs       (fn [item]
                          (let [menuItem (:menuItem item)
                                k        (:key menuItem)
                                href     (gen-href @route {:partial-query-params {query-param-key k}})
                                on-click (fn [event]
                                           (.preventDefault event)
                                           (dispatch [::change-tab db-path k]))]
                            (-> item
                                (update :menuItem merge {:href                     href :onClick on-click
                                                         :data-reitit-handle-click false}))))]
    (when (nil? @active-tab)
      (dispatch [::helpers/set db-path ::default-tab (or @cur-view (some-> (seq panes) first :menuItem :key))]))
    (fn [opts]
      [ui/Tab
       (-> (dissoc opts :db-path :change-event)
           (assoc :panes (map add-hrefs panes)
                  :active-index (get key->index (keyword @cur-view) 0))
           (assoc-in [:menu :class] :uix-tab-nav))])))

(s/fdef Tab
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path]
                                   :opt-un [::helpers/change-event])))
