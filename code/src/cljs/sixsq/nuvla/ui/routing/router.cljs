(ns sixsq.nuvla.ui.routing.router
  (:require [clojure.string :as str]
            [re-frame.core :as re-frame]
            [reitit.core :as r]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.history :as rfh]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.routing.r-routes :refer [router]]
            [sixsq.nuvla.ui.routing.utils :refer [decode-query-string]]
            [sixsq.nuvla.ui.main.events :as main-events]))

(def page-alias {"nuvlabox"        "edges"
                 "edge"            "edges"
                 "infrastructures" "clouds"
                 "deployment"      "deployments"})

(defn split-path-alias
  [path]
  (let [[page :as path-vec] (vec (str/split path #"/"))
        real-page (get page-alias page)]
    (if (and page real-page)
      (assoc path-vec 0 real-page)
      path-vec)))

;;; Effects ;;;

;; Triggering navigation from events.

(re-frame/reg-fx :push-state
  (fn [route]
(js/console.error "route in effex" route)
    (apply rfe/push-state route)))


(re-frame/reg-event-fx ::push-state
  (fn [_ [_ & route]]
(js/console.error "route" route)
    {:push-state route}))


(re-frame/reg-event-fx
  ::navigated
  (fn [{db :db} [_ new-match]]
    (let [old-match   (:current-route db)
          controllers (rfc/apply-controllers (:controllers old-match) new-match)
          path (-> new-match
                   :path
                   (str/replace-first config/base-path "")
                   (str/replace #"^/|/$" ""))
          path-parts   (split-path-alias path)
          query-params (decode-query-string path)]
      {:db
         (-> db (assoc :current-route
                       (assoc new-match :controllers controllers))
             (assoc ::main-spec/nav-path path-parts)
             (assoc ::main-spec/nav-query-params query-params))
         :fx [[:dispatch [::main-events/set-navigation-info]]]})))

;;; Subscriptions ;;;

(re-frame/reg-sub
  ::current-route
  (fn [db]
    (:current-route db)))


(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (rfe/href k params query)))


(defn on-navigate [new-match]
  (when new-match
    (re-frame/dispatch [::navigated new-match])))


(defn init-routes! []
  (js/console.log "initializing routes")
  (rfe/start!
    router
    on-navigate
    {:use-fragment false
     :ignore-anchor-click? (fn [router e el uri]
                             (and (rfh/ignore-anchor-click? router e el uri)
                                (not= "false" (.getAttribute el "data-reitit-handle-click"))))}))

(defn nav [{:keys [router current-route]}]
  [:ul
   (for [route-name (r/route-names router)
         :let       [route (r/match-by-name router route-name)
                     text (-> route :data :link-text)]]
     [:li {:key route-name}
      (when (= route-name (-> current-route :data :name))
        "> ")
      [:a {:href (href route-name)} text]])])

(defn- router-component-internal [{:keys [router]}]
  (let [current-route @(re-frame/subscribe [::current-route])
        view        (-> current-route :data :view)
        path           @(re-frame/subscribe [::main-subs/nav-path])]
    [:div
    [nav {:router router :current-route current-route}]
     (when current-route
       [view (assoc current-route :path path)])]))

(defn router-component []
  [router-component-internal {:router router}])

(comment (re-frame/dispatch [::push-state :r-routes/deployments]))