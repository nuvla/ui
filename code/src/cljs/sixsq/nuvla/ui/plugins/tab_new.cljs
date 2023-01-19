(ns sixsq.nuvla.ui.plugins.tab-new
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-fx reg-sub subscribe]]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.routing.events :as route-events]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.routing.utils :refer [gen-href]]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(s/def ::change-event (s/nilable coll?))

(defn- db-path->query-param-key
  [[qualified-key]]
  (let [ns-path     (str/split (namespace qualified-key) #"\.")
        last-two-ns (drop (- (count ns-path) 2) ns-path)
        k-prefix     (str/join last-two-ns)]
    (->> qualified-key
         name
         (str k-prefix)
         keyword)))

(reg-sub
  ::default-tab
  (fn [db [_ db-path]]
    (get-in db (conj db-path ::active-tab))))

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
                        {:change-event change-event
                         :partial-query-params {(db-path->query-param-key db-path) tab-key}}]]]})))

(defn Tab
  [{:keys [db-path panes change-event] :as _opts}]
  (dispatch [::helpers/set db-path ::change-event change-event])
  (dispatch [::helpers/set db-path
             ::active-tab (some-> (seq panes) first :menuItem :key)])
  (let [route           (subscribe [::route-subs/current-route])
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
                                (update :menuItem merge {:href href :onClick on-click
                                                         :data-reitit-handle-click false}))))]
    (fn [opts]
      [ui/Tab
       (-> (update opts :panes #(map add-hrefs %))
           (dissoc :db-path :change-event :default-active-tab)
           (assoc :active-index
             (get key->index (keyword @cur-view) 0)))])))


(s/fdef Tab
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path]
                                   :opt-un [::helpers/change-event])))
