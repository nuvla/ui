(ns sixsq.nuvla.ui.routing.router
  (:require
    [clojure.string :as str]
    [re-frame.core :as re-frame]
    [reitit.core :as r]
    [reitit.frontend.controllers :as rfc]
    [reitit.frontend.easy :as rfe]
    [reitit.frontend.history :as rfh]
    [sixsq.nuvla.ui.routing.r-routes :refer [router]]))

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

(re-frame/reg-event-db
  ::navigated
  (fn [db [_ new-match]]
   (let [old-match   (:current-route db)
         controllers (rfc/apply-controllers (:controllers old-match) new-match)]
     (assoc db :current-route (assoc new-match :controllers controllers)))))

;;; Subscriptions ;;;

(re-frame/reg-sub ::current-route
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
      ;; Create a normal links that user can click
      [:a {:href (href route-name) :on-click (fn [event] (.preventDefault event)) :data-reitit-handle-click false} text]])])

(defn- router-component-internal [{:keys [router]}]
  (let [current-route @(re-frame/subscribe [::current-route])
        view        (-> current-route :data :view)
        path-string (-> current-route :path (str/replace "/ui" ""))
        path        (->> (str/split path-string #"/") (remove str/blank?))]
    [:div
    [nav {:router router :current-route current-route}]
     (when current-route
       [view (merge current-route {:path-string path-string :path path})]) ]))

(defn router-component []
  [router-component-internal {:router router}])

(comment (re-frame/dispatch [::push-state :r-routes/deployments])
         )