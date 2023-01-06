(ns sixsq.nuvla.ui.router
  (:require [re-frame.core :as re-frame]
            [reitit.coercion.spec :as rss]
            [reitit.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]
            [sixsq.nuvla.ui.about.views :refer [about]]
            [sixsq.nuvla.ui.apps.views :refer [apps-view]]
            [sixsq.nuvla.ui.cimi.views :refer [api-view]]
            [sixsq.nuvla.ui.clouds.views :refer [clouds-view]]
            [sixsq.nuvla.ui.credentials.views :refer [credentials-view]]
            [sixsq.nuvla.ui.dashboard.views :refer [dashboard-view]]
            [sixsq.nuvla.ui.deployment-dialog.views-data :refer [data-view]]
            [sixsq.nuvla.ui.deployment-sets.views :refer [deployment-sets-view]]
            [sixsq.nuvla.ui.deployments.views :refer [deployments-view]]
            [sixsq.nuvla.ui.edges.views :refer [edges-view]]
            [sixsq.nuvla.ui.notifications.views :refer [notifications-view]]
            [sixsq.nuvla.ui.welcome.views :refer [home-view]]
            [sixsq.nuvla.ui.deployment-dialog.views-module-version :as dep-diag-versions]))

;;; Effects ;;;

;; Triggering navigation from events.

(re-frame/reg-fx :push-state
  (fn [route]
    (apply rfe/push-state route)))

;;; Events ;;;

(re-frame/reg-event-db ::initialize-db
  (fn [db _]
    (if db
      db
      {:current-route nil})))

(re-frame/reg-event-fx ::push-state
  (fn [_ [_ & route]]
    {:push-state route}))

(re-frame/reg-event-db ::navigated
  (fn [db [_ new-match]]
(js/console.error new-match)
    (let [old-match   (:current-route db)
          controllers (rfc/apply-controllers (:controllers old-match) new-match)]
      (assoc db :current-route (assoc new-match :controllers controllers)))))

;;; Subscriptions ;;;

(re-frame/reg-sub ::current-route
  (fn [db]
    (:current-route db)))

;;; Views ;;;

(defn home-page []
  [:div
   [:h1 "This is home page"]
   [:button
    ;; Dispatch navigate event that triggers a (side)effect.
    {:on-click #(re-frame/dispatch [::push-state ::sub-page2])}
    "Go to sub-page 2"]])

(defn sub-page1 []
  [:div
   [:h1 "This is sub-page 1"]])

(defn sub-page2 []
  [:div
   [:h1 "This is sub-page 2"]])

;;; Routes ;;;

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (rfe/href k params query)))

(def routes
  ["/ui/"
   [""
    {:name      ::home-from-example
     :view      home-page
     :link-text "Home"
     :controllers
     [{;; Do whatever initialization needed for home page
       ;; I.e (re-frame/dispatch [::events/load-something-with-ajax])
       :start (fn [& params] (js/console.log "Entering home page"))
       ;; Teardown can be done here.
       :stop  (fn [& params] (js/console.log "Leaving home page"))}]}]
   ["about"
    {:name ::about
     :view about
     :link-text "About"}]
   ["welcome"
    {:name ::home
     :view home-view
     :link-text "home"}]
   ["dashboard"
    {:name ::dashboard
     :view dashboard-view
     :link-text "dashboard"}]
   ["apps"
    {:name ::apps
     :view apps-view
     :link-text "Apps"}]
   ["deployments"
    {:name ::deployments
     :view deployments-view
     :link-text "deployments"}]
   ["deployment/:id"
    {:name ::deployement
     :view deployments-view
     :parameters
     {:path {:id string?}}}]
   ["deployment-sets"
    {:name ::deployment-sets
     :view deployment-sets-view
     :link-text "deployment-sets"}]
   ["edges"
    {:name ::edges
     :view edges-view
     :link-text "edges"}]
   ["credentials"
    {:name ::credentials
     :view credentials-view
     :link-text "credentials"}]
   ["notifications"
    {:name ::notifications
     :view notifications-view
     :link-text "notifications"}]
   ["data"
    {:name ::data
     :view data-view
     :link-text "data"}]
   ["clouds"
    {:name ::clouds
     :view clouds-view
     :link-text "clouds"}]
   ["api"
    {:name ::api
     :view api-view
     :link-text "api"}]
   ["sub-page1"
    {:name      ::sub-page1
     :view      sub-page1
     :link-text "Sub page 1"
     :controllers
     [{:start (fn [& params] (js/console.log "Entering sub-page 1"))
       :stop  (fn [& params] (js/console.log "Leaving sub-page 1"))}]}]
   ["sub-page2"
    {:name      ::sub-page2
     :view      sub-page2
     :link-text "Sub-page 2"
     :controllers
     [{:start (fn [& params] (js/console.log "Entering sub-page 2"))
       :stop  (fn [& params] (js/console.log "Leaving sub-page 2"))}]}]])

(defn on-navigate [new-match]
  (when new-match
    (re-frame/dispatch [::navigated new-match])))

(def router
  (rf/router
    routes
    {:data {:coercion rss/coercion}}))

(defn init-routes! []
  (js/console.log "initializing routes")
  (rfe/start!
    router
    on-navigate
    {:use-fragment false}))

(defn nav [{:keys [router current-route]}]
  [:ul
   (for [route-name (r/route-names router)
         :let       [route (r/match-by-name router route-name)
                     text (-> route :data :link-text)]]
     [:li {:key route-name}
      (when (= route-name (-> current-route :data :name))
        "> ")
      ;; Create a normal links that user can click
      [:a {:href (href route-name)} text]])])

(defn- router-component-internal [{:keys [router]}]
  (let [current-route @(re-frame/subscribe [::current-route])]
    [:div
     #_[nav {:router router :current-route current-route}]
     (when current-route
       [(-> current-route :data :view)])]))

(defn router-component []
  [router-component-internal {:router router}])