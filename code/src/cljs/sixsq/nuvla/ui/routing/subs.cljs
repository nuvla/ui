(ns sixsq.nuvla.ui.routing.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.main.spec :as spec]
            [sixsq.nuvla.ui.routing.utils :refer [get-query-param
                                                  ->canonical-route]]))

(reg-sub
  ::nav-path
  :-> ::spec/nav-path)

(reg-sub
  ::nav-query-params
  :-> ::spec/nav-query-params)

(reg-sub
  ::nav-path-first
  :<- [::nav-path]
  :-> first)

(reg-sub
  ::nav-url-active?
  :<- [::canonical-route-name]
  :<- [::nav-path-first]
  (fn [[route-name nav-path-first] [_ route-names url]]
    (boolean
      (or (route-names route-name) (= route-name route-names)
        (-> (str/replace-first url (str config/base-path "/") "")
            (str/split "/")
            first
            (= nav-path-first)
            boolean)))))

(reg-sub
  ::current-route
  :-> :current-route)

(reg-sub
  ::route-name
  :<- [::current-route]
  :-> (comp :name :data))

(reg-sub
  ::canonical-route-name
  :<- [::route-name]
  :-> ->canonical-route)

(reg-sub
  ::path-params
  :<- [::current-route]
  :-> :path-params)

(reg-sub
  ::path-param
  :<- [::path-params]
  (fn [path-params [_ path-param-key]]
    (get path-params path-param-key)))

(reg-sub
  ::query-param
  :<- [::current-route]
  (fn [current-route [_ query-param-key]]
    (get-query-param current-route query-param-key)))

(reg-sub
  ::has-query-param-value?
  :<- [::current-route]
  (fn [current-route [_ query-param-key query-param-value]]
    (let [vals (some-> (get-query-param current-route query-param-key)
                       (str/split #",")
                       (->> (map str/lower-case)))]
      (boolean ((set vals) query-param-value)))))
