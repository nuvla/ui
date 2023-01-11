(ns sixsq.nuvla.ui.routing.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.main.spec :as spec]))

(reg-sub
  ::nav-path
  (fn [db]
    (::spec/nav-path db)))

(reg-sub
  ::nav-query-params
  (fn [db]
    (::spec/nav-query-params db)))

(reg-sub
  ::nav-path-first
  :<- [::nav-path]
  (fn [nav-path]
    (first nav-path)))

(reg-sub
  ::nav-url-active?
  :<- [::nav-path-first]
  (fn [nav-path-first [_ url]]
    (boolean (= nav-path-first (-> (str/replace-first url (str config/base-path "/") "")
                                   (str/split "/")
                                   first)))))

;;; Subscriptions ;;;
(reg-sub
  ::current-route
  (fn [db]
    (:current-route db)))