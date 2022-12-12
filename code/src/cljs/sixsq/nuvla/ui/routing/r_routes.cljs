(ns sixsq.nuvla.ui.routing.r-routes
  (:require [re-frame.core :as re-frame]
            [sixsq.nuvla.ui.about.views :refer [about]]
            [sixsq.nuvla.ui.apps.views :refer [apps-view]]
            [sixsq.nuvla.ui.cimi.views :refer [api-view]]
            [sixsq.nuvla.ui.clouds.views :refer [clouds-view]]
            [sixsq.nuvla.ui.credentials.views :refer [credentials-view]]
            [sixsq.nuvla.ui.dashboard.views :refer [dashboard-view]] ;; [sixsq.nuvla.ui.data.views :refer [data-view]]
            [sixsq.nuvla.ui.data.views :refer [data-view]]
            [sixsq.nuvla.ui.deployment-sets.views :refer [deployment-sets-view]]
            [sixsq.nuvla.ui.deployments-detail.views :refer [DeploymentDetails]]
            [sixsq.nuvla.ui.deployments.views :refer [deployments-view]]
            [sixsq.nuvla.ui.edges.views :refer [DetailedView edges-view]]
            [sixsq.nuvla.ui.notifications.views :refer [notifications-view]]
            [sixsq.nuvla.ui.welcome.views :refer [home-view]]))

;;; Views ;;;

(defn home-page []
  [:div
   [:h1 "This is home page"]
   [:button
    ;; Dispatch navigate event that triggers a (side)effect.
    {:on-click #(re-frame/dispatch [:sixsq.nuvla.ui.routing.router/push-state ::sub-page2])}
    "Go to sub-page 2"]])

(defn sub-page1 []
  [:div
   [:h1 "This is sub-page 1"]])

(defn sub-page2 []
  [:div
   [:h1 "This is sub-page 2"]])

(def r-routes
  ["/ui/"
   [""
    {:name      ::home-root
     :view      home-page
     :link-text "Home"
     :controllers
     [{;; Do whatever initialization needed for home page
       ;; I.e (re-frame/dispatch [::events/load-something-with-ajax])
       :start (fn [& params] (js/console.log "Entering home page"))
       ;; Teardown can be done here.
       :stop  (fn [& params] (js/console.log "Leaving home page"))}]}]
   ["sub-page2"
    {:name ::sub-page2
     :view sub-page2
     :link-text "About"}]
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
    {:name ::deployment
     :view DeploymentDetails}]
   ["deployment-sets"
    {:name ::deployment-sets
     :view deployment-sets-view
     :link-text "deployment-sets"}]
   ["edges"
    {:name ::edges
     :view edges-view
     :link-text "edges"}]
   ["edges/:id"
    {:name ::edges-details
     :view DetailedView
     :link-text "edges-details"}]
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
     :link-text "api"}]])
