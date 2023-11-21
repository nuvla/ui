(ns sixsq.nuvla.ui.routing.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.main.spec :as spec]
            [sixsq.nuvla.ui.routing.utils :refer [get-query-param]]))

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
  :<- [::nav-path-first]
  (fn [nav-path-first [_ url]]
    (-> (str/replace-first url (str config/base-path "/") "")
        (str/split "/")
        first
        (= nav-path-first)
        boolean)))

(reg-sub
  ::current-route
  :-> :current-route)

(reg-sub
  ::route-name
  :<- [::current-route]
  :-> (comp :name :data))

(reg-sub
  ::query-param
  :<- [::current-route]
  (fn [current-route [_ query-param-key]]
    (get-query-param current-route query-param-key)))
